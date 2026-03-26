package com.isekraft.world;

import com.isekraft.IseKraftMod;
import com.isekraft.entity.ModEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * Sky Island Generator — Gökyüzü Adası.
 *
 * A floating island at Y=180 (±20 variation), home of Gilgamesh.
 *
 * Structure:
 *   • Elliptic grass+dirt island base (20×20 footprint, 3 layers deep)
 *   • Dirt taper under the island (stalactite shape, prevents flat bottom look)
 *   • Ancient stone ruin (6x6 with collapsed pillars) on top
 *   • 2 chest rooms with high-tier loot
 *   • Gilgamesh spawner inside the ruin's throne room
 *   • Golden throne (gold block + slab)
 *   • Decorative obsidian pillars with End Rod tops
 *
 * Uses the same two-pass placement as BattleTowerGenerator (FORCE_STATE for bulk,
 * NOTIFY_ALL for behaviour-sensitive blocks).
 */
public class SkyIslandGenerator {

    public record BlockPlacement(BlockPos pos, BlockState state) {}

    static final int ISLAND_R  = 10;  // radius
    static final int ISLAND_Y  = 180; // base Y (overridden by variation)

    private static final Identifier SKY_LOOT =
        new Identifier(IseKraftMod.MOD_ID, "chests/sky_island");

    // ── BLOCK LIST ────────────────────────────────────────────────────────────

    public static void queue(BlockPos centre, Random random, List<BlockPlacement> out) {
        int cx = centre.getX(), cy = centre.getY(), cz = centre.getZ();

        // ── ISLAND BASE ──────────────────────────────────────────────────────
        for (int dx = -ISLAND_R; dx <= ISLAND_R; dx++) {
            for (int dz = -ISLAND_R; dz <= ISLAND_R; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > ISLAND_R) continue;

                // Taper: fewer layers towards the edge
                int depth = (int)(3.5 - dist / ISLAND_R * 2.5);
                depth = Math.max(1, depth);

                for (int dy = -depth; dy <= 0; dy++) {
                    Block b = (dy == 0) ? Blocks.GRASS_BLOCK : Blocks.DIRT;
                    out.add(new BlockPlacement(new BlockPos(cx + dx, cy + dy, cz + dz),
                        b.getDefaultState()));
                }

                // Stalactite taper below
                double edgeFactor = dist / ISLAND_R;
                int stalaDepth = (int)(6 * (1 - edgeFactor * edgeFactor));
                for (int dy = -depth - 1; dy >= -depth - stalaDepth; dy--) {
                    double taper = dist + ((-dy - depth) * 1.5);
                    if (taper > ISLAND_R) break;
                    out.add(new BlockPlacement(new BlockPos(cx + dx, cy + dy, cz + dz),
                        Blocks.DIRT.getDefaultState()));
                }
            }
        }

        // ── STONE RUIN FLOOR ─────────────────────────────────────────────────
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++)
                out.add(new BlockPlacement(new BlockPos(cx + dx, cy + 1, cz + dz),
                    Blocks.POLISHED_ANDESITE.getDefaultState()));

        // ── RUIN WALLS (partial — collapsed) ─────────────────────────────────
        int[][] wallCorners = {{-4,-4},{4,-4},{-4,4},{4,4}};
        for (int[] corner : wallCorners) {
            int wx = corner[0], wz = corner[1];
            int height = 3 + random.nextInt(3); // 3–5 blocks, uneven
            for (int dy = 2; dy <= height; dy++) {
                out.add(new BlockPlacement(new BlockPos(cx+wx, cy+dy, cz+wz),
                    Blocks.STONE_BRICKS.getDefaultState()));
            }
            // Cracked / mossy variation
            if (random.nextBoolean())
                out.add(new BlockPlacement(new BlockPos(cx+wx, cy+2, cz+wz),
                    Blocks.MOSSY_STONE_BRICKS.getDefaultState()));
        }

        // North + south partial walls
        for (int dx = -3; dx <= 3; dx++) {
            if (random.nextFloat() < 0.6f) {
                Block wb = random.nextBoolean() ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE;
                out.add(new BlockPlacement(new BlockPos(cx+dx, cy+2, cz-4), wb.getDefaultState()));
            }
            if (random.nextFloat() < 0.6f) {
                out.add(new BlockPlacement(new BlockPos(cx+dx, cy+2, cz+4),
                    Blocks.STONE_BRICKS.getDefaultState()));
            }
        }

        // ── GOLDEN THRONE (centre-north) ─────────────────────────────────────
        out.add(new BlockPlacement(new BlockPos(cx, cy+2, cz-2), Blocks.GOLD_BLOCK.getDefaultState()));
        out.add(new BlockPlacement(new BlockPos(cx, cy+3, cz-2),
            Blocks.GOLD_BLOCK.getDefaultState()));
        out.add(new BlockPlacement(new BlockPos(cx-1, cy+2, cz-2), Blocks.GOLD_BLOCK.getDefaultState()));
        out.add(new BlockPlacement(new BlockPos(cx+1, cy+2, cz-2), Blocks.GOLD_BLOCK.getDefaultState()));

        // ── OBSIDIAN PILLARS (4 corners of ruin) ─────────────────────────────
        int[][] pillars = {{-3,-3},{3,-3},{-3,3},{3,3}};
        for (int[] p : pillars) {
            for (int dy = 1; dy <= 5; dy++)
                out.add(new BlockPlacement(new BlockPos(cx+p[0], cy+dy, cz+p[1]),
                    Blocks.OBSIDIAN.getDefaultState()));
            out.add(new BlockPlacement(new BlockPos(cx+p[0], cy+6, cz+p[1]),
                Blocks.END_ROD.getDefaultState()));
        }

        // ── GILGAMESH SPAWNER (centre of ruin, elevated) ─────────────────────
        out.add(new BlockPlacement(new BlockPos(cx, cy+2, cz), Blocks.SPAWNER.getDefaultState()));

        // ── LOOT CHESTS ───────────────────────────────────────────────────────
        out.add(new BlockPlacement(new BlockPos(cx-3, cy+2, cz),   Blocks.CHEST.getDefaultState()));
        out.add(new BlockPlacement(new BlockPos(cx+3, cy+2, cz),   Blocks.CHEST.getDefaultState()));

        // ── FLOWERS / DECOR on island grass ──────────────────────────────────
        int[][] flowerSpots = {{-7,0},{7,0},{0,-7},{0,7},{-5,-5},{5,5}};
        Block[] flowers = {Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM};
        for (int[] fs : flowerSpots) {
            double d = Math.sqrt(fs[0]*fs[0] + fs[1]*fs[1]);
            if (d <= ISLAND_R - 1)
                out.add(new BlockPlacement(new BlockPos(cx+fs[0], cy+1, cz+fs[1]),
                    flowers[Math.abs(fs[0]+fs[1]) % flowers.length].getDefaultState()));
        }
    }

    // ── BLOCK ENTITY SETUP ────────────────────────────────────────────────────

    public static void setupSpawnersAndChests(WorldAccess world, BlockPos centre, Random random) {
        int cx = centre.getX(), cy = centre.getY(), cz = centre.getZ();

        // Gilgamesh spawner
        BlockPos spawnerPos = new BlockPos(cx, cy+2, cz);
        if (world.getBlockEntity(spawnerPos) instanceof MobSpawnerBlockEntity s) {
            configureSpawner(s, ModEntities.GILGAMESH,
                300, 600, 1, 1, 32);
        }

        // Loot chests
        Identifier loot = SKY_LOOT;
        tryChest(world, new BlockPos(cx-3, cy+2, cz), loot, random);
        tryChest(world, new BlockPos(cx+3, cy+2, cz), loot, random);
    }

    private static void configureSpawner(MobSpawnerBlockEntity s,
                                          net.minecraft.entity.EntityType<?> type,
                                          int minDelay, int maxDelay,
                                          int count, int maxNearby, int range) {
        NbtCompound nbt  = s.createNbt();
        String entityId  = net.minecraft.registry.Registries.ENTITY_TYPE.getId(type).toString();
        NbtCompound data = new NbtCompound();
        NbtCompound etag = new NbtCompound();
        etag.putString("id", entityId);
        data.put("entity", etag);
        nbt.put("SpawnData", data);
        NbtList potentials = new NbtList();
        NbtCompound pot    = new NbtCompound();
        pot.put("data", data.copy());
        pot.putInt("weight", 1);
        potentials.add(pot);
        nbt.put("SpawnPotentials", potentials);
        nbt.putShort("MinSpawnDelay",      (short) minDelay);
        nbt.putShort("MaxSpawnDelay",      (short) maxDelay);
        nbt.putShort("SpawnCount",         (short) count);
        nbt.putShort("MaxNearbyEntities",  (short) maxNearby);
        nbt.putShort("RequiredPlayerRange",(short) range);
        s.readNbt(nbt);
        s.markDirty();
    }

        private static void tryChest(WorldAccess world, BlockPos pos,
                                  Identifier loot, Random random) {
        if (world.getBlockEntity(pos) instanceof ChestBlockEntity c)
            c.setLootTable(loot, random.nextLong());
    }

    // ── Behaviour-sensitive check (for ModWorldGen two-pass placement) ────────
    public static boolean isBehaviourSensitive(BlockState state) {
        Block b = state.getBlock();
        return b instanceof LadderBlock
            || b instanceof PaneBlock
            || b instanceof WallTorchBlock
            || b instanceof TorchBlock
            || b instanceof EndRodBlock          // ← ekledim
            || b instanceof FlowerBlock          // ← ekledim
            || b instanceof TallPlantBlock;      // ← ekledim
    }
}   // ← Bu en son kapanış parantezi olmalı. Dosyanın en son satırı bu olsun.