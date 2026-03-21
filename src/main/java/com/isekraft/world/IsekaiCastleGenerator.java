package com.isekraft.world;
import net.minecraft.item.ItemStack;

import com.isekraft.IseKraftMod;
import com.isekraft.entity.ModEntities;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.state.property.Properties;

public class IsekaiCastleGenerator {

    private static final Identifier CASTLE_LOOT =
        new Identifier(IseKraftMod.MOD_ID, "chests/isekai_castle");

    public static void generate(ServerWorld w, BlockPos pos, Random random) {
        if (w.isClient()) return; // safety: never run client-side
        buildFoundation(w, pos);
        buildFloor1(w, pos);
        buildFloor2(w, pos);
        buildFloor3(w, pos);
        buildRoof(w, pos);
        buildTowers(w, pos);
        placeInterior(w, pos, random);
        placeTraps(w, pos);
        IseKraftMod.LOGGER.info("[IseKraft] Castle generated at {}", pos);
    }

    private static void set(ServerWorld w, BlockPos o, int dx, int dy, int dz, Block b) {
        w.setBlockState(o.add(dx, dy, dz), b.getDefaultState());
    }

    private static void fillRect(ServerWorld w, BlockPos o, int y, int x0, int z0, int x1, int z1, Block b) {
        for (int dx = x0; dx <= x1; dx++)
            for (int dz = z0; dz <= z1; dz++)
                w.setBlockState(o.add(dx, y, dz), b.getDefaultState());
    }

    private static void buildWalls(ServerWorld w, BlockPos o, int yOff, int height, int sx, int sz, Block wall) {
        for (int dy = yOff; dy < yOff + height; dy++) {
            for (int dx = 0; dx <= sx; dx++) {
                w.setBlockState(o.add(dx, dy, 0),  wall.getDefaultState());
                w.setBlockState(o.add(dx, dy, sz), wall.getDefaultState());
            }
            for (int dz = 1; dz < sz; dz++) {
                w.setBlockState(o.add(0,  dy, dz), wall.getDefaultState());
                w.setBlockState(o.add(sx, dy, dz), wall.getDefaultState());
            }
        }
    }

    private static void buildFoundation(ServerWorld w, BlockPos o) {
        for (int dx = -1; dx <= 24; dx++)
            for (int dz = -1; dz <= 24; dz++)
                for (int dy = -2; dy <= 0; dy++)
                    w.setBlockState(o.add(dx, dy, dz), Blocks.STONE_BRICKS.getDefaultState());
    }

    private static void buildFloor1(ServerWorld w, BlockPos o) {
        buildWalls(w, o, 0, 6, 22, 22, Blocks.STONE_BRICKS);
        fillRect(w, o, 0, 1, 1, 21, 21, Blocks.STONE_BRICKS);
        // Gate
        for (int dy = 1; dy <= 4; dy++)
            for (int dx = 9; dx <= 13; dx++)
                w.setBlockState(o.add(dx, dy, 0), Blocks.AIR.getDefaultState());
        // Iron bars on gate sides
        for (int dy = 1; dy <= 4; dy++) {
            w.setBlockState(o.add(8, dy, 0), Blocks.IRON_BARS.getDefaultState());
            w.setBlockState(o.add(14, dy, 0), Blocks.IRON_BARS.getDefaultState());
        }
        placeTorch(w, o.add(5, 3, 1));
        placeTorch(w, o.add(17, 3, 1));
        placeTorch(w, o.add(1, 3, 10));
        placeTorch(w, o.add(21, 3, 10));
        buildPillar(w, o.add(4, 1, 4), 5);
        buildPillar(w, o.add(18, 1, 4), 5);
        buildPillar(w, o.add(4, 1, 18), 5);
        buildPillar(w, o.add(18, 1, 18), 5);
    }

    private static void buildFloor2(ServerWorld w, BlockPos o) {
        buildWalls(w, o, 7, 6, 22, 22, Blocks.STONE_BRICKS);
        fillRect(w, o, 7, 1, 1, 21, 21, Blocks.STONE_BRICKS);
        // Throne carpet
        for (int dz = 5; dz <= 19; dz++)
            w.setBlockState(o.add(11, 8, dz), Blocks.RED_CARPET.getDefaultState());
        // Throne
        w.setBlockState(o.add(11, 8, 20), Blocks.STONE_BRICK_STAIRS.getDefaultState());
        placeTorch(w, o.add(5, 10, 1));
        placeTorch(w, o.add(17, 10, 1));
        placeTorch(w, o.add(5, 10, 20));
        placeTorch(w, o.add(17, 10, 20));
        // Decorative chains
        for (int dy = 8; dy <= 12; dy++) {
            w.setBlockState(o.add(3, dy, 11), Blocks.CHAIN.getDefaultState());
            w.setBlockState(o.add(19, dy, 11), Blocks.CHAIN.getDefaultState());
        }
    }

    private static void buildFloor3(ServerWorld w, BlockPos o) {
        buildWalls(w, o, 14, 6, 22, 22, Blocks.CRACKED_STONE_BRICKS);
        fillRect(w, o, 14, 1, 1, 21, 21, Blocks.STONE_BRICKS);
        // Broken windows
        for (int side : new int[]{0, 22}) {
            w.setBlockState(o.add(side, 16, 11), Blocks.AIR.getDefaultState());
            w.setBlockState(o.add(side, 17, 11), Blocks.AIR.getDefaultState());
            w.setBlockState(o.add(11, 16, side), Blocks.AIR.getDefaultState());
            w.setBlockState(o.add(11, 17, side), Blocks.AIR.getDefaultState());
        }
        placeTorch(w, o.add(5, 16, 5));
        placeTorch(w, o.add(17, 16, 5));
        placeTorch(w, o.add(5, 16, 17));
        placeTorch(w, o.add(17, 16, 17));
    }

    private static void buildRoof(ServerWorld w, BlockPos o) {
        for (int dx = 0; dx <= 22; dx++)
            for (int dz = 0; dz <= 22; dz++)
                w.setBlockState(o.add(dx, 21, dz), Blocks.STONE_BRICK_SLAB.getDefaultState());
        for (int i = 0; i <= 22; i += 2) {
            w.setBlockState(o.add(i, 22, 0),  Blocks.STONE_BRICK_WALL.getDefaultState());
            w.setBlockState(o.add(i, 22, 22), Blocks.STONE_BRICK_WALL.getDefaultState());
            w.setBlockState(o.add(0,  22, i), Blocks.STONE_BRICK_WALL.getDefaultState());
            w.setBlockState(o.add(22, 22, i), Blocks.STONE_BRICK_WALL.getDefaultState());
        }
    }

    private static void buildTowers(ServerWorld w, BlockPos o) {
        int[][] corners = {{-3,-3},{-3,22},{22,-3},{22,22}};
        for (int[] c : corners) {
            for (int dy = 0; dy <= 26; dy++) {
                Block b = dy < 23 ? Blocks.STONE_BRICKS : Blocks.CRACKED_STONE_BRICKS;
                for (int dx = 0; dx < 5; dx++) {
                    w.setBlockState(o.add(c[0]+dx, dy, c[1]),   b.getDefaultState());
                    w.setBlockState(o.add(c[0]+dx, dy, c[1]+4), b.getDefaultState());
                }
                for (int dz = 1; dz < 4; dz++) {
                    w.setBlockState(o.add(c[0],   dy, c[1]+dz), b.getDefaultState());
                    w.setBlockState(o.add(c[0]+4, dy, c[1]+dz), b.getDefaultState());
                }
            }
            for (int dx = 0; dx < 5; dx++)
                for (int dz = 0; dz < 5; dz++)
                    w.setBlockState(o.add(c[0]+dx, 26, c[1]+dz), Blocks.STONE_BRICK_SLAB.getDefaultState());
            // Tower torch
            placeTorch(w, o.add(c[0]+2, 4, c[1]+2));
            placeTorch(w, o.add(c[0]+2, 12, c[1]+2));
            placeTorch(w, o.add(c[0]+2, 20, c[1]+2));
        }
    }

    // ── TRAPS ─────────────────────────────────────────────────────────────────
    private static void placeTraps(ServerWorld w, BlockPos o) {
        // Trap 1: Pressure plate + TNT in floor 1 corridor
        // Hidden TNT under floor, pressure plate on top
        for (int[] trap : new int[][]{{7,1,8},{15,1,8},{7,1,14},{15,1,14}}) {
            // TNT one block below floor
            w.setBlockState(o.add(trap[0], trap[1]-1, trap[2]), Blocks.TNT.getDefaultState());
            // Stone pressure plate (looks like normal floor at a glance)
            w.setBlockState(o.add(trap[0], trap[1], trap[2]), Blocks.STONE_PRESSURE_PLATE.getDefaultState());
        }

        // Trap 2: Dispenser with arrows in corridor wall
        // disp[0]=x, disp[1]=y, disp[2]=z
        for (int[] disp : new int[][]{{0,2,8},{22,2,14}}) {
            // x=0 duvarı → içeriye (EAST) yönünde ateşle
            // x=22 duvarı → içeriye (WEST) yönünde ateşle
            Direction facing = (disp[0] == 0) ? Direction.EAST : Direction.WEST;
            BlockPos dp = o.add(disp[0], disp[1], disp[2]);
            w.setBlockState(dp, Blocks.DISPENSER.getDefaultState()
                .with(Properties.FACING, facing));
            if (w.getBlockEntity(dp) instanceof net.minecraft.block.entity.DispenserBlockEntity d) {
                d.addToFirstFreeSlot(new net.minecraft.item.ItemStack(net.minecraft.item.Items.ARROW, 16));
            }
            // Pressure plate: dispenser'ın önünde, zemin seviyesinde (disp[1]-1)
            int plateX = (disp[0] == 0) ? disp[0] + 2 : disp[0] - 2;
            w.setBlockState(o.add(plateX, disp[1] - 1, disp[2]),
                Blocks.STONE_PRESSURE_PLATE.getDefaultState());
        }

        // Trap 3: Floor 3 tripwire
        w.setBlockState(o.add(5, 15, 11), Blocks.TRIPWIRE_HOOK.getDefaultState());
        w.setBlockState(o.add(17, 15, 11), Blocks.TRIPWIRE_HOOK.getDefaultState());
        for (int dx = 6; dx <= 16; dx++)
            w.setBlockState(o.add(dx, 15, 11), Blocks.TRIPWIRE.getDefaultState());
        // TNT above tripwire
        for (int dx : new int[]{6, 11, 16})
            w.setBlockState(o.add(dx, 16, 11), Blocks.TNT.getDefaultState());
    }

    // ── INTERIOR ──────────────────────────────────────────────────────────────
    private static void placeInterior(ServerWorld w, BlockPos o, Random random) {
        // Floor 1 chest
        placeChest(w, o.add(11, 1, 3), random);
        // Floor 1 spawner
        placeSpawner(w, o.add(5, 1, 11), ModEntities.DARK_KNIGHT, random);
        // Floor 2 chest
        placeChest(w, o.add(3, 8, 11), random);
        // Floor 2 spawner
        placeSpawner(w, o.add(19, 8, 11), ModEntities.DARK_KNIGHT, random);
        // Floor 3 boss chest
        placeChest(w, o.add(11, 15, 11), random);
        // Floor 3 Goblin King spawner
        placeSpawner(w, o.add(11, 15, 18), ModEntities.GOBLIN_KING, random);
    }

    private static void placeChest(ServerWorld w, BlockPos pos, Random random) {
        w.setBlockState(pos, Blocks.CHEST.getDefaultState());
        if (w.getBlockEntity(pos) instanceof ChestBlockEntity c)
            c.setLootTable(CASTLE_LOOT, random.nextLong());
    }

    private static <T extends net.minecraft.entity.Entity> void placeSpawner(
            ServerWorld w, BlockPos pos, EntityType<T> type, Random random) {
        w.setBlockState(pos, Blocks.SPAWNER.getDefaultState());
        if (w.getBlockEntity(pos) instanceof MobSpawnerBlockEntity s)
            s.getLogic().setEntityId(type, w, random, pos);
    }

    private static void buildPillar(ServerWorld w, BlockPos base, int height) {
        for (int dy = 0; dy < height; dy++)
            w.setBlockState(base.add(0, dy, 0), Blocks.STONE_BRICK_WALL.getDefaultState());
    }

    private static void placeTorch(ServerWorld w, BlockPos pos) {
        if (w.getBlockState(pos).isAir())
            w.setBlockState(pos, Blocks.TORCH.getDefaultState());
    }
}
