package com.isekraft.world;

import com.isekraft.IseKraftMod;
import com.isekraft.entity.ModEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Battle Tower — 9x9 footprint, 8 floors + boss roof.
 *
 * queue()                    — builds a pure block-placement list (no world access).
 *                              Called by BattleTowerPiece (Structure API) and admin command.
 * setupSpawnersAndChests()   — configures block entities. Accepts WorldAccess so it works
 *                              with both StructureWorldAccess (worldgen) and ServerWorld (commands).
 *
 * FLOORS and FLOOR_H are package-visible so BattleTowerPiece can compute bounding box.
 */
public class BattleTowerGenerator {

    public record BlockPlacement(BlockPos pos, BlockState state) {}

    private static final Identifier TOWER_LOOT =
        new Identifier(IseKraftMod.MOD_ID, "chests/battle_tower");

    static final int WIDTH   = 9;
    static final int FLOORS  = 8;
    static final int FLOOR_H = 6;

    // ── BLOCK LIST BUILDER ───────────────────────────────────────────────────

    /**
     * Builds the complete block-placement list for a tower at {@code base}.
     * Pure computation — no world access, safe to call during worldgen.
     */
    public static void queue(BlockPos base, Random random, List<BlockPlacement> out) {
        // Foundation (3 deep, 1 block wider on each side)
        for (int dx = -1; dx <= WIDTH; dx++)
            for (int dz = -1; dz <= WIDTH; dz++)
                for (int dy = -3; dy < 0; dy++)
                    add(out, base, dx, dy, dz, Blocks.COBBLESTONE);

        // Floors
        for (int floor = 0; floor < FLOORS; floor++)
            buildFloor(base, floor, floor * FLOOR_H, out);

        // ── RE-CARVE LADDER HOLES ────────────────────────────────────────────
        // buildFloor(floor+1) overwrites holes with solid stone brick floor slabs.
        // Re-carve after all floors are built so the holes stay open.
        // Also re-place the transition ladder at the hole level (overwritten by next floor slab).
        for (int floor = 0; floor < FLOORS - 1; floor++) {
            int holeY = (floor + 1) * FLOOR_H;
            // Clear the two hole blocks in the floor slab
            out.add(new BlockPlacement(base.add(WIDTH / 2, holeY, 1), Blocks.AIR.getDefaultState()));
            out.add(new BlockPlacement(base.add(WIDTH / 2, holeY, 2), Blocks.AIR.getDefaultState()));
            // Re-place ladder at hole level (next floor slab overwrote it during buildFloor)
            out.add(new BlockPlacement(base.add(WIDTH / 2, holeY, 1),
                Blocks.LADDER.getDefaultState()
                    .with(net.minecraft.state.property.Properties.HORIZONTAL_FACING,
                        net.minecraft.util.math.Direction.SOUTH)));
        }

        // Roof platform
        int roofY = FLOORS * FLOOR_H;
        for (int dx = 0; dx < WIDTH; dx++)
            for (int dz = 0; dz < WIDTH; dz++)
                add(out, base, dx, roofY, dz, Blocks.STONE_BRICK_SLAB);

        // Roof crenellations
        for (int i = 0; i < WIDTH; i += 2) {
            add(out, base, i,         roofY + 1, 0,         Blocks.STONE_BRICK_WALL);
            add(out, base, i,         roofY + 1, WIDTH - 1, Blocks.STONE_BRICK_WALL);
            add(out, base, 0,         roofY + 1, i,         Blocks.STONE_BRICK_WALL);
            add(out, base, WIDTH - 1, roofY + 1, i,         Blocks.STONE_BRICK_WALL);
        }

        // Boss spawner on FLOOR 7 (top interior room) — needs darkness to spawn
        // Roof is open sky (light=15), spawners need light<8. Top interior is dark.
        int bossFloorY = (FLOORS - 1) * FLOOR_H; // floor 7 base
        out.add(new BlockPlacement(base.add(WIDTH / 2, bossFloorY + 2, WIDTH / 2), Blocks.SPAWNER.getDefaultState()));
        out.add(new BlockPlacement(base.add(WIDTH / 2 + 1, bossFloorY + 1, WIDTH / 2), Blocks.CHEST.getDefaultState()));
    }

    private static void buildFloor(BlockPos base, int floor, int yBase, List<BlockPlacement> out) {
        Block wallBlock  = (floor % 2 == 0) ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;

        // Floor slab
        for (int dx = 0; dx < WIDTH; dx++)
            for (int dz = 0; dz < WIDTH; dz++)
                add(out, base, dx, yBase, dz, Blocks.STONE_BRICKS);

        // Walls (5 blocks tall)
        for (int dy = 1; dy <= 5; dy++) {
            for (int dx = 0; dx < WIDTH; dx++) {
                add(out, base, dx, yBase + dy, 0,         wallBlock);
                add(out, base, dx, yBase + dy, WIDTH - 1, wallBlock);
            }
            for (int dz = 1; dz < WIDTH - 1; dz++) {
                add(out, base, 0,         yBase + dy, dz, wallBlock);
                add(out, base, WIDTH - 1, yBase + dy, dz, wallBlock);
            }
        }

        // Clear interior
        for (int dy = 1; dy <= 5; dy++)
            for (int dx = 1; dx < WIDTH - 1; dx++)
                for (int dz = 1; dz < WIDTH - 1; dz++)
                    add(out, base, dx, yBase + dy, dz, Blocks.AIR);

        // Ladder on north wall (dz=1), centred.
        // Goes from yBase+1 to yBase+6 (6 blocks) — the 6th block is at the hole level
        // so the player can climb through to the next floor.
        for (int dy = 0; dy <= 5; dy++)
            out.add(new BlockPlacement(base.add(WIDTH / 2, yBase + 1 + dy, 1),
                Blocks.LADDER.getDefaultState()
                    .with(net.minecraft.state.property.Properties.HORIZONTAL_FACING,
                        net.minecraft.util.math.Direction.SOUTH)));

        // Hole in floor above for ladder access
        if (floor < FLOORS - 1) {
            int nextFloorY = yBase + FLOOR_H;
            out.add(new BlockPlacement(base.add(WIDTH / 2,     nextFloorY, 1), Blocks.AIR.getDefaultState()));
            out.add(new BlockPlacement(base.add(WIDTH / 2,     nextFloorY, 2), Blocks.AIR.getDefaultState()));
        }

        // Windows (all floors except ground)
        if (floor > 0) {
            int wy = yBase + 3;
            out.add(new BlockPlacement(base.add(WIDTH / 2, wy, 0),         Blocks.GLASS_PANE.getDefaultState()));
            out.add(new BlockPlacement(base.add(WIDTH / 2, wy, WIDTH - 1), Blocks.GLASS_PANE.getDefaultState()));
            out.add(new BlockPlacement(base.add(0,         wy, WIDTH / 2), Blocks.GLASS_PANE.getDefaultState()));
            out.add(new BlockPlacement(base.add(WIDTH - 1, wy, WIDTH / 2), Blocks.GLASS_PANE.getDefaultState()));
        }

        // Entrance on floor 0 south wall
        if (floor == 0) {
            add(out, base, WIDTH / 2,     yBase + 1, WIDTH - 1, Blocks.AIR);
            add(out, base, WIDTH / 2,     yBase + 2, WIDTH - 1, Blocks.AIR);
            add(out, base, WIDTH / 2 - 1, yBase + 1, WIDTH - 1, Blocks.AIR);
            add(out, base, WIDTH / 2 - 1, yBase + 2, WIDTH - 1, Blocks.AIR);
        }

        // Interior torches
        int ty = yBase + 3;
        out.add(new BlockPlacement(base.add(1, ty, 1),
            Blocks.WALL_TORCH.getDefaultState()
                .with(net.minecraft.state.property.Properties.HORIZONTAL_FACING,
                    net.minecraft.util.math.Direction.EAST)));
        out.add(new BlockPlacement(base.add(WIDTH - 2, ty, WIDTH - 2),
            Blocks.WALL_TORCH.getDefaultState()
                .with(net.minecraft.state.property.Properties.HORIZONTAL_FACING,
                    net.minecraft.util.math.Direction.WEST)));

        // Per-floor content (spawners, chests, traps) — blocks only
        addFloorBlocks(base, floor, yBase, out);
    }

    private static void addFloorBlocks(BlockPos base, int floor, int yBase, List<BlockPlacement> out) {
        BlockPos spawnerPos = base.add(WIDTH / 2, yBase + 2, WIDTH / 2);
        BlockPos chestPos   = base.add(WIDTH - 2, yBase + 1, WIDTH - 2);
        switch (floor) {
            case 0 -> out.add(new BlockPlacement(spawnerPos, Blocks.SPAWNER.getDefaultState()));
            case 1 -> {
                out.add(new BlockPlacement(spawnerPos, Blocks.SPAWNER.getDefaultState()));
                out.add(new BlockPlacement(chestPos,   Blocks.CHEST.getDefaultState()));
                out.add(new BlockPlacement(base.add(WIDTH / 2, yBase - 1, WIDTH / 2 + 2), Blocks.TNT.getDefaultState()));
                out.add(new BlockPlacement(base.add(WIDTH / 2, yBase,     WIDTH / 2 + 2), Blocks.STONE_PRESSURE_PLATE.getDefaultState()));
            }
            case 2 -> out.add(new BlockPlacement(spawnerPos, Blocks.SPAWNER.getDefaultState()));
            case 3 -> {
                out.add(new BlockPlacement(spawnerPos, Blocks.SPAWNER.getDefaultState()));
                out.add(new BlockPlacement(chestPos,   Blocks.CHEST.getDefaultState()));
                out.add(new BlockPlacement(base.add(2, yBase + 1, WIDTH / 2), Blocks.TRIPWIRE_HOOK.getDefaultState()));
                out.add(new BlockPlacement(base.add(6, yBase + 1, WIDTH / 2), Blocks.TRIPWIRE_HOOK.getDefaultState()));
                for (int dx = 3; dx <= 5; dx++)
                    out.add(new BlockPlacement(base.add(dx, yBase + 1, WIDTH / 2), Blocks.TRIPWIRE.getDefaultState()));
                out.add(new BlockPlacement(base.add(WIDTH / 2, yBase + 2, WIDTH / 2 + 1), Blocks.TNT.getDefaultState()));
            }
            case 4 -> {
                out.add(new BlockPlacement(spawnerPos,                    Blocks.SPAWNER.getDefaultState()));
                out.add(new BlockPlacement(base.add(2, yBase + 2, 2),    Blocks.SPAWNER.getDefaultState()));
            }
            case 5 -> {
                out.add(new BlockPlacement(spawnerPos, Blocks.SPAWNER.getDefaultState()));
                out.add(new BlockPlacement(chestPos,   Blocks.CHEST.getDefaultState()));
            }
            case 6 -> {
                out.add(new BlockPlacement(base.add(3, yBase + 1, 3), Blocks.SOUL_SAND.getDefaultState()));
                out.add(new BlockPlacement(base.add(5, yBase + 1, 5), Blocks.SOUL_SAND.getDefaultState()));
                out.add(new BlockPlacement(chestPos, Blocks.CHEST.getDefaultState()));
            }
            case 7 -> out.add(new BlockPlacement(chestPos, Blocks.CHEST.getDefaultState()));
        }
    }

    private static void add(List<BlockPlacement> out, BlockPos origin,
                             int dx, int dy, int dz, Block block) {
        out.add(new BlockPlacement(origin.add(dx, dy, dz), block.getDefaultState()));
    }

    // ── BLOCK ENTITY SETUP ───────────────────────────────────────────────────

    /**
     * Configures spawners and chests after blocks are placed.
     *
     * @param world    WorldAccess — works with both StructureWorldAccess (worldgen)
     *                 and ServerWorld (admin /summon tower command).
     * @param chunkBox Only configure block entities within this box.
     *                 Pass {@code null} to configure everything (admin command).
     */
    public static void setupSpawnersAndChests(WorldAccess world, BlockPos base,
                                              Random random, @Nullable BlockBox chunkBox) {
        // Boss spawner on floor 7 (top interior) — must match queue() placement
        int bossFloorY = (FLOORS - 1) * FLOOR_H;
        trySpawner(world, base.add(WIDTH / 2,     bossFloorY + 2, WIDTH / 2), ModEntities.GOBLIN_KING, random, chunkBox);
        tryChest(world,   base.add(WIDTH / 2 + 1, bossFloorY + 1, WIDTH / 2), random, chunkBox);

        // Per-floor entities
        EntityType<?>[] spawnerTypes = {
            ModEntities.FOREST_WOLF,  // floor 0
            ModEntities.DARK_KNIGHT,  // floor 1
            ModEntities.DARK_KNIGHT,  // floor 2
            ModEntities.DARK_KNIGHT,  // floor 3
            ModEntities.DARK_KNIGHT,  // floor 4 (two spawners)
            ModEntities.DARK_KNIGHT,  // floor 5
            null,                      // floor 6
            null,                      // floor 7
        };
        boolean[] hasChest = { false, true, false, true, false, true, true, true };

        for (int floor = 0; floor < FLOORS; floor++) {
            int yBase = floor * FLOOR_H;
            if (spawnerTypes[floor] != null)
                trySpawner(world, base.add(WIDTH / 2, yBase + 2, WIDTH / 2), spawnerTypes[floor], random, chunkBox);
            if (hasChest[floor])
                tryChest(world, base.add(WIDTH - 2, yBase + 1, WIDTH - 2), random, chunkBox);
            if (floor == 4)
                trySpawner(world, base.add(2, yBase + 2, 2), ModEntities.DARK_KNIGHT, random, chunkBox);
        }
    }

    private static void trySpawner(WorldAccess world, BlockPos pos,
                                   EntityType<?> type, Random random, @Nullable BlockBox box) {
        if (box != null && !box.contains(pos)) return;
        if (!(world.getBlockEntity(pos) instanceof MobSpawnerBlockEntity s)) return;

        // Use NBT approach — works with null world (StructureWorldAccess during worldgen)
        // and avoids the World reference requirement of setEntityId().
        net.minecraft.nbt.NbtCompound spawnerNbt = s.createNbt();
        String entityId = net.minecraft.registry.Registries.ENTITY_TYPE.getId(type).toString();
        net.minecraft.nbt.NbtCompound spawnData = new net.minecraft.nbt.NbtCompound();
        net.minecraft.nbt.NbtCompound entityTag = new net.minecraft.nbt.NbtCompound();
        entityTag.putString("id", entityId);
        spawnData.put("entity", entityTag);
        spawnerNbt.put("SpawnData", spawnData);
        // Also set SpawnPotentials so the spawner only spawns this type
        net.minecraft.nbt.NbtList potentials = new net.minecraft.nbt.NbtList();
        net.minecraft.nbt.NbtCompound potential = new net.minecraft.nbt.NbtCompound();
        potential.put("data", spawnData.copy());
        potential.putInt("weight", 1);
        potentials.add(potential);
        spawnerNbt.put("SpawnPotentials", potentials);
        // Reasonable spawn settings
        spawnerNbt.putShort("MinSpawnDelay", (short)200);
        spawnerNbt.putShort("MaxSpawnDelay", (short)400);
        spawnerNbt.putShort("SpawnCount", (short)2);
        spawnerNbt.putShort("MaxNearbyEntities", (short)6);
        spawnerNbt.putShort("RequiredPlayerRange", (short)12);
        s.readNbt(spawnerNbt);
        s.markDirty();
    }

    private static void tryChest(WorldAccess world, BlockPos pos,
                                 Random random, @Nullable BlockBox box) {
        if (box != null && !box.contains(pos)) return;
        if (world.getBlockEntity(pos) instanceof ChestBlockEntity c)
            c.setLootTable(TOWER_LOOT, random.nextLong());
    }
}
