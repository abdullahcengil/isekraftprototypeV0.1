package com.isekraft.world;

import com.isekraft.IseKraftMod;
import com.isekraft.entity.ModEntities;
import com.isekraft.rpg.PlayerRpgManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Level 100 event — simplified.
 *
 * OLD approach: teleport player to The End. Problem: The End may not be loaded,
 * hardcoded BlockPos(100,60,0) could be mid-air, and there was no exit route.
 *
 * NEW approach: spawn the Shadow Demon 25 blocks away from the player IN THEIR
 * CURRENT WORLD. No teleport, no dimension dependency. Works anywhere.
 *
 * Flow (ticks):
 *   0   — global broadcast
 *   20  — particles + sound (dark power surge)
 *   60  — explosion particles + shatter message
 *   120 — warning message
 *   160 — Shadow Demon spawns 25 blocks away. Fight begins.
 */
public class DemonLordEvent {

    private static final Set<String> triggered = new HashSet<>();
    private static int checkTimer = 0;

    private record ScheduledTask(long targetTick, Runnable task) {}
    private static final PriorityQueue<ScheduledTask> taskQueue =
        new PriorityQueue<>(Comparator.comparingLong(ScheduledTask::targetTick));
    private static long serverTick = 0;

    /** Called by /isekraft setlevel — prevents auto-trigger for this player. */
    public static void markTriggered(String playerName) {
        triggered.add(playerName);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DemonLordEvent::tick);
    }

    private static void tick(MinecraftServer server) {
        serverTick++;

        while (!taskQueue.isEmpty() && taskQueue.peek().targetTick() <= serverTick)
            taskQueue.poll().task().run();

        if (++checkTimer < 20) return;
        checkTimer = 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (PlayerRpgManager.getLevel(player) < 100) continue;
            String name = player.getName().getString();
            if (triggered.contains(name)) continue;

            NbtCompound data = PlayerRpgManager.getData(player);
            if (data.getBoolean("DemonLordDone")) continue;

            triggered.add(name);
            data.putBoolean("DemonLordDone", true);
            PlayerRpgManager.setData(player, data);
            triggerEvent(player, server);
        }
    }

    private static void triggerEvent(ServerPlayerEntity player, MinecraftServer server) {
        // Capture world at trigger time — use player's current world, no End teleport
        ServerWorld world = player.getServerWorld();

        server.getPlayerManager().getPlayerList().forEach(p ->
            p.sendMessage(Text.literal("★ " + player.getName().getString()
                + " has reached the pinnacle of power! ★")
                .formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false));

        schedule(20, () -> {
            world.spawnParticles(ParticleTypes.DRAGON_BREATH,
                player.getX(), player.getY() + 1, player.getZ(), 50, 2, 2, 2, 0.2);
            world.playSound(null, player.getBlockPos(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 2f, 0.5f);
            player.sendMessage(Text.literal("An overwhelming dark power surges through you...")
                .formatted(Formatting.DARK_PURPLE), false);
        });

        schedule(60, () -> {
            world.spawnParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY(), player.getZ(), 20, 1, 0.5, 1, 0);
            player.sendMessage(Text.literal("The boundary between worlds shatters!")
                .formatted(Formatting.RED, Formatting.BOLD), false);
        });

        schedule(120, () ->
            player.sendMessage(Text.literal("Something stirs in the darkness nearby...")
                .formatted(Formatting.DARK_PURPLE), false));

        schedule(160, () -> {
            // Spawn Shadow Demon 25 blocks north of player — no teleport needed
            BlockPos demonPos = player.getBlockPos().add(0, 1, -25);
            com.isekraft.entity.ShadowDemonEntity demon = ModEntities.SHADOW_DEMON.create(world);
            if (demon == null) {
                IseKraftMod.LOGGER.warn("[IseKraft] Failed to create ShadowDemonEntity!");
                return;
            }
            demon.setPos(demonPos.getX(), demonPos.getY(), demonPos.getZ());
            demon.initialize(world, world.getLocalDifficulty(demonPos),
                SpawnReason.EVENT, null, null);
            world.spawnEntity(demon);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                demonPos.getX(), demonPos.getY() + 1, demonPos.getZ(),
                100, 3, 3, 3, 0.5);
            world.playSound(null, demonPos,
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3f, 0.3f);
            player.sendMessage(Text.literal("☠ THE SHADOW DEMON EMERGES! ☠")
                .formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            server.getPlayerManager().getPlayerList().forEach(p ->
                p.sendMessage(Text.literal("The Shadow Demon has appeared near "
                    + player.getName().getString() + "!")
                    .formatted(Formatting.DARK_RED), false));
        });
    }

    /** Public API for other classes to schedule a delayed task via this queue. */
    public static void scheduleDelayed(int delayTicks, Runnable task) {
        taskQueue.add(new ScheduledTask(serverTick + delayTicks, task));
    }

    private static void schedule(int delayTicks, Runnable task) {
        taskQueue.add(new ScheduledTask(serverTick + delayTicks, task));
    }
}
