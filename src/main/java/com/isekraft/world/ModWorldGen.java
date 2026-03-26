package com.isekraft.world;

import com.isekraft.IseKraftMod;
import com.isekraft.world.SkyIslandGenerator;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * Battle Tower spawn system.
 *
 * CHUNK_LOAD does ZERO world access — only stores (world, chunkPos, seed).
 * All expensive work (getTopPosition, block placement) runs in START_SERVER_TICK.
 *
 * Why this matters:
 *   getTopPosition() computes heightmap — expensive on main thread during chunk load.
 *   setBlockState() with NOTIFY_ALL cascades to 26 neighbors — 1400 blocks = 36k calls.
 *   Both are now in tick(), never in CHUNK_LOAD. CHUNK_LOAD is 3 lines of pure math.
 *
 *   Block.FORCE_STATE (flag 16): writes directly to chunk section, zero notifications.
 *   1400 blocks with FORCE_STATE = ~0.5ms. Full tower in one tick, no lag.
 */
public class ModWorldGen {

    private static boolean worldReady = false;
    private static int ticksSinceReady = 0;

    private static final int SPAWN_CHANCE        = 500;  // Tower: 1/500 chunks
    private static final int SKY_ISLAND_CHANCE   = 800;  // Sky island: 1/800 chunks
    private static final int MAX_CACHE    = 10_000;

    private static final Set<Long> processedChunks = ConcurrentHashMap.newKeySet();

    // CHUNK_LOAD only stores these — no world access at all
    private record PendingChunk(ServerWorld world, ChunkPos cp, long seed) {}
    private static final ConcurrentLinkedQueue<PendingChunk> pendingChunks    = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<PendingChunk> pendingIslands   = new ConcurrentLinkedQueue<>();

    public static void registerCastleSpawn() {

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            worldReady = true;
            ticksSinceReady = 0;
            IseKraftMod.LOGGER.info("[IseKraft] World ready — tower system enabled.");
        });

        ServerTickEvents.START_SERVER_TICK.register(ModWorldGen::tick);

                ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (!worldReady || ticksSinceReady < 100) return;
            if (world.isClient()) return;
            if (!world.getRegistryKey().equals(World.OVERWORLD)) return;

            ChunkPos cp = chunk.getPos();
            long chunkKey = (long) cp.x << 32 | (cp.z & 0xFFFFFFFFL);

            if (processedChunks.size() > MAX_CACHE) processedChunks.clear();
            if (!processedChunks.add(chunkKey)) return;

            // Deterministic seed
            long seed = world.getSeed() ^ (cp.x * 341873128712L) ^ (cp.z * 132897987541L);

            // Tower spawn
            if (new java.util.Random(seed).nextInt(SPAWN_CHANCE) == 0) {
                pendingChunks.add(new PendingChunk((ServerWorld) world, cp, seed));
            }

            // Sky Island spawn — daha yüksek şans + daha stabil random
            long islandSeed = seed ^ 0x9E3779B97F4A7C15L;   // iyi bir karıştırma
            if (new java.util.Random(islandSeed).nextInt(SKY_ISLAND_CHANCE) == 0) {
                pendingIslands.add(new PendingChunk((ServerWorld) world, cp, islandSeed));
                IseKraftMod.LOGGER.info("[SkyIsland DEBUG] Added pending sky island at chunk {}", cp);
            }
        });

        IseKraftMod.LOGGER.info("IseKraft world gen registered.");
    }

    private static void tick(MinecraftServer server) {
        if (worldReady) ticksSinceReady++;

        tickIsland(server);
        PendingChunk next = pendingChunks.poll();
        if (next == null) return;

        ServerWorld world = next.world();
        ChunkPos cp = next.cp();

        // Chunk must still be loaded
        if (!world.isChunkLoaded(cp.x, cp.z)) return;

        // ── EXPENSIVE WORK HAPPENS HERE IN TICK, NEVER IN CHUNK_LOAD ────────
        int bx = cp.getStartX() + 8;
        int bz = cp.getStartZ() + 8;
        BlockPos surface = world.getTopPosition(
            Heightmap.Type.WORLD_SURFACE_WG,
            new BlockPos(bx, 64, bz));

        if (surface.getY() < 63) return;
        if (world.getBlockState(surface).isOf(Blocks.COBBLESTONE)) return;

        Random random = Random.create(next.seed());

        // Build full block list — pure computation, no world access
        List<BattleTowerGenerator.BlockPlacement> placements = new ArrayList<>(1500);
        BattleTowerGenerator.queue(surface, random, placements);

        // Pass 1: Solid blocks — FORCE_STATE, zero neighbor cascade
        for (BattleTowerGenerator.BlockPlacement bp : placements) {
            if (!isBehaviourSensitive(bp.state())) {
                world.setBlockState(bp.pos(), bp.state(), Block.FORCE_STATE | Block.SKIP_DROPS);
            }
        }

        // Pass 2: Neighbour-sensitive blocks — need NOTIFY_ALL to orient/connect
        for (BattleTowerGenerator.BlockPlacement bp : placements) {
            if (isBehaviourSensitive(bp.state())) {
                world.setBlockState(bp.pos(), bp.state(), Block.NOTIFY_ALL | Block.FORCE_STATE);
            }
        }

        // Pass 3: Configure spawners + chests (block entities)
        BattleTowerGenerator.setupSpawnersAndChests(world, surface, Random.create(next.seed()), null);

        IseKraftMod.LOGGER.info("[IseKraft] Tower placed at {} ({} blocks, 1 tick)",
            surface, placements.size());
    }

            private static void tickIsland(MinecraftServer server) {
        PendingChunk next = pendingIslands.poll();
        if (next == null) return;

        ServerWorld world = next.world();
        ChunkPos cp = next.cp();

        if (!world.isChunkLoaded(cp.x, cp.z)) {
            // Tekrar dene diye geri koy (nadiren lazım olur)
            pendingIslands.add(next);
            return;
        }

        int bx = cp.getStartX() + 8;
        int bz = cp.getStartZ() + 8;
        Random rng = Random.create(next.seed());
        int islandY = 170 + rng.nextInt(20);
        BlockPos centre = new BlockPos(bx, islandY, bz);

        List<SkyIslandGenerator.BlockPlacement> placements = new ArrayList<>();
        SkyIslandGenerator.queue(centre, rng, placements);

        // Pass 1: Solid blocks
        for (SkyIslandGenerator.BlockPlacement bp : placements) {
            if (!SkyIslandGenerator.isBehaviourSensitive(bp.state())) {
                world.setBlockState(bp.pos(), bp.state(), Block.FORCE_STATE | Block.SKIP_DROPS);
            }
        }

        // Pass 2: Sensitive blocks (ladder, torch, end rod, flower vs.)
        for (SkyIslandGenerator.BlockPlacement bp : placements) {
            if (SkyIslandGenerator.isBehaviourSensitive(bp.state())) {
                world.setBlockState(bp.pos(), bp.state(), Block.NOTIFY_ALL | Block.FORCE_STATE);
            }
        }

        SkyIslandGenerator.setupSpawnersAndChests(world, centre, rng);

        IseKraftMod.LOGGER.info("[IseKraft] Sky Island placed at {} (Gilgamesh)", centre);
    }

    private static boolean isBehaviourSensitive(BlockState state) {
        Block b = state.getBlock();
        return b instanceof LadderBlock
            || b instanceof PaneBlock
            || b instanceof WallTorchBlock
            || b instanceof TorchBlock;
    }
}   // ← this is now the correct closing brace of the ModWorldGen class
