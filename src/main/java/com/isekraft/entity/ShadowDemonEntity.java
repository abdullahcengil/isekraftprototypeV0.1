package com.isekraft.entity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.MinecraftServer;

import com.isekraft.item.ModItems;
import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Shadow Demon — The Final Boss.
 *
 * Spawned in The End when a level 100 player arrives.
 * Phase 1: Melee + void bolts
 * Phase 2 (below 50% HP): Summons Dark Knights + AOE void explosion
 * Death: Drops Demon Lord Crown + 1000 XP to all nearby players
 */
public class ShadowDemonEntity extends HostileEntity {

    private final ServerBossBar bossBar;
    private boolean phase2 = false;
    private int attackTimer = 80; // delay first attack
    private int summonTimer = 200;

    public ShadowDemonEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        bossBar = new ServerBossBar(
            Text.literal("☠ Shadow Demon ☠").formatted(Formatting.DARK_PURPLE, Formatting.BOLD),
            BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 500.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 15.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
            .add(EntityAttributes.GENERIC_ARMOR, 12.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 16f));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(2, new RevengeGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        // setPercent → sendToPlayers() çağırır, server-only olmalı
        bossBar.setPercent(getHealth() / getMaxHealth());

        ServerWorld sw = (ServerWorld) getWorld();

        // Constant dark particles around the boss
        if (getWorld().getTime() % 10 == 0) {
            sw.spawnParticles(ParticleTypes.PORTAL,
                getX(), getY() + 1, getZ(),
                8, 1.5, 1.5, 1.5, 0.1);
            sw.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                getX(), getY() + 2, getZ(),
                4, 0.5, 0.5, 0.5, 0.05);
        }

        // Enter phase 2
        if (!phase2 && getHealth() < getMaxHealth() * 0.5f) {
            phase2 = true;
            enterPhase2(sw);
        }

        // Void bolt attack (every 3 seconds)
        if (--attackTimer <= 0) {
            attackTimer = 60;
            voidBoltAttack(sw);
        }

        // Phase 2: summon minions
        if (phase2 && --summonTimer <= 0) {
            summonTimer = 180;
            summonMinions(sw);
        }
    }

    private void enterPhase2(ServerWorld sw) {
        bossBar.setColor(BossBar.Color.RED);
        bossBar.setName(Text.literal("☠ Shadow Demon [UNLEASHED] ☠").formatted(Formatting.DARK_RED, Formatting.BOLD));

        // Massive explosion of particles
        sw.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 30, 3, 1, 3, 0);
        sw.spawnParticles(ParticleTypes.DRAGON_BREATH, getX(), getY() + 1, getZ(), 50, 2, 2, 2, 0.1);
        sw.playSound(null, getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2f, 0.5f);

        broadcast(sw, Text.literal("★ THE SHADOW DEMON IS UNLEASHED! ★").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.45);
        getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(22.0);
    }

    private void voidBoltAttack(ServerWorld sw) {
        // Target nearest player and shoot a wither skull
        PlayerEntity target = sw.getClosestPlayer(this, 30);
        if (target == null) return;

        sw.spawnParticles(ParticleTypes.SMOKE,
            getX(), getY() + 2, getZ(), 15, 0.5, 0.5, 0.5, 0.1);

        Vec3d dir = target.getPos().add(0, 1, 0).subtract(getPos().add(0, 2, 0)).normalize();
        SmallFireballEntity skull = new SmallFireballEntity(sw, this,
                dir.x * 1.5, dir.y * 1.5, dir.z * 1.5);
        skull.setPos(getX() + dir.x * 2, getY() + 2, getZ() + dir.z * 2);
        sw.spawnEntity(skull);
        sw.playSound(null, getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 0.7f);
    }

    private void summonMinions(ServerWorld sw) {
        for (int i = 0; i < 2; i++) {
            DarkKnightEntity minion = ModEntities.DARK_KNIGHT.create(sw);
            if (minion == null) continue;
            double ox = getX() + (random.nextDouble() - 0.5) * 8;
            double oz = getZ() + (random.nextDouble() - 0.5) * 8;
            minion.setPos(ox, getY(), oz);
            minion.initialize(sw, sw.getLocalDifficulty(getBlockPos()),
                SpawnReason.MOB_SUMMONED, null, null);
            sw.spawnEntity(minion);
            sw.spawnParticles(ParticleTypes.PORTAL, ox, getY(), oz, 10, 0.3, 0.5, 0.3, 0.05);
        }
        broadcast(sw, Text.literal("The Shadow Demon calls its servants!").formatted(Formatting.DARK_PURPLE));
    }

    @Override
    public void onDeath(DamageSource src) {
        super.onDeath(src);
        bossBar.clearPlayers();

        if (!(getWorld() instanceof ServerWorld sw)) return;

        // DEATH EXPLOSION — massive particle burst
        for (int i = 0; i < 5; i++) {
            sw.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY() + i, getZ(), 10, 2, 0.5, 2, 0);
        }
        sw.spawnParticles(ParticleTypes.DRAGON_BREATH, getX(), getY() + 2, getZ(), 100, 4, 4, 4, 0.2);
        sw.spawnParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2, getZ(), 200, 3, 3, 3, 0.5);
        sw.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 3f, 1f);

        // Drop Demon Lord Crown
        dropStack(new ItemStack(ModItems.DEMON_LORD_CROWN));
        dropStack(new ItemStack(ModItems.SOUL_CRYSTAL, 16));
        dropStack(new ItemStack(ModItems.DEMON_CORE, 8));

        // Reward all nearby players
        for (PlayerEntity p : sw.getPlayers()) {
            if (p.distanceTo(this) <= 100f) {
                PlayerRpgManager.addXp(p, 2000);

                // Teleport back to overworld after 5 seconds
                if (p instanceof ServerPlayerEntity sp) {
                    sp.sendMessage(Text.literal("★ SHADOW DEMON DEFEATED! ★ +2000 XP!").formatted(Formatting.GOLD, Formatting.BOLD), false);
                    sp.sendMessage(Text.literal("You will be returned to the overworld...").formatted(Formatting.YELLOW), false);

                    // Tick-based delay — Thread.sleep() ana server thread'ini dondurur, kullanma!
                    scheduleReturn(sw.getServer(), sp);
                }
            }
        }

        broadcast(sw, Text.literal("The Shadow Demon has fallen. The darkness recedes.").formatted(Formatting.LIGHT_PURPLE));
    }

    private void broadcast(ServerWorld sw, Text msg) {
        sw.getPlayers().forEach(p -> p.sendMessage(msg, false));
    }

    /**
     * Schedule a delayed teleport to spawn using DemonLordEvent's task queue.
     * BUG FIX: Previously registered a new ServerTickEvents listener per death,
     * which is a permanent memory leak (Fabric event handlers can't be unregistered).
     * Now delegates to DemonLordEvent.schedule() which uses a PriorityQueue — O(log n),
     * no leak, cleaned up after execution.
     */
    private static void scheduleReturn(MinecraftServer server, ServerPlayerEntity sp) {
        com.isekraft.world.DemonLordEvent.scheduleDelayed(100, () -> {
            ServerWorld overworld = server.getOverworld();
            BlockPos spawn = overworld.getSpawnPos();
            sp.teleport(overworld, spawn.getX(), spawn.getY() + 1,
                spawn.getZ(), sp.getYaw(), sp.getPitch());
            sp.sendMessage(Text.literal("Welcome back, Isekai Overlord.")
                .formatted(Formatting.LIGHT_PURPLE), false);
        });
    }

    @Override public void onStartedTrackingBy(ServerPlayerEntity p) { super.onStartedTrackingBy(p); bossBar.addPlayer(p); }
    @Override public void onStoppedTrackingBy(ServerPlayerEntity p) { super.onStoppedTrackingBy(p); bossBar.removePlayer(p); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_WITHER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_WITHER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_WITHER_DEATH; }
}
