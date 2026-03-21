package com.isekraft.entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityType;

import com.isekraft.item.ModItems;
import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.entity.*;
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

public class GoblinKingEntity extends HostileEntity {

    private final ServerBossBar bossBar;
    private boolean enraged = false;
    private int summonTimer = 200; // delay first summon
    private int stompTimer  = 60;

    public GoblinKingEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        bossBar = new ServerBossBar(
            Text.literal("Goblin King").formatted(Formatting.DARK_GREEN, Formatting.BOLD),
            BossBar.Color.GREEN, BossBar.Style.NOTCHED_10);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 12.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
            .add(EntityAttributes.GENERIC_ARMOR, 8.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(3, new WanderAroundFarGoal(this, 0.6));
        goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(2, new RevengeGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        // setPercent → sendToPlayers() çağırır, server-only olmalı
        bossBar.setPercent(getHealth() / getMaxHealth());
        if (!enraged && getHealth() < getMaxHealth() * 0.5f) enterPhaseTwo();
        if (--summonTimer <= 0) { summonTimer = enraged ? 120 : 220; summonMinions(); }
        if (enraged && --stompTimer <= 0) { stompTimer = 60; doStomp(); }
    }

    private void enterPhaseTwo() {
        enraged = true;
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.42);
        getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(18.0);
        bossBar.setColor(BossBar.Color.RED);
        bossBar.setName(Text.literal("Goblin King [ENRAGED]").formatted(Formatting.RED, Formatting.BOLD));
        if (getWorld() instanceof ServerWorld sw)
            sw.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 20, 1.5, 0.5, 1.5, 0.0);
        broadcast(Text.literal("The Goblin King RAGES!").formatted(Formatting.RED));
    }

    private void summonMinions() {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        int count = enraged ? 2 : 1;
        for (int i = 0; i < count; i++) {
            DarkKnightEntity m = ModEntities.DARK_KNIGHT.create(sw);
            if (m == null) continue;
            double ox = getX() + (random.nextDouble() - 0.5) * 5;
            double oz = getZ() + (random.nextDouble() - 0.5) * 5;
            m.setPos(ox, getY(), oz);
            m.initialize(sw, sw.getLocalDifficulty(getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
            sw.spawnEntity(m);
        }
    }

    private void doStomp() {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        sw.spawnParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 15, 2.0, 0.1, 2.0, 0.0);
        sw.playSound(null, getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.6f);
        for (PlayerEntity p : sw.getPlayers()) {
            if (p.distanceTo(this) <= 8f) {
                p.damage(getDamageSources().mobAttack(this), 8f);
                Vec3d dir = p.getPos().subtract(getPos()).normalize();
                p.addVelocity(dir.x * 1.2, 0.6, dir.z * 1.2);
                p.velocityModified = true;
            }
        }
    }

    @Override
    public void onDeath(DamageSource src) {
        super.onDeath(src);
        bossBar.clearPlayers();
        if (getWorld() instanceof ServerWorld sw) {
            dropStack(new ItemStack(ModItems.DEMON_CORE, 2 + random.nextInt(3)));
            dropStack(new ItemStack(ModItems.SOUL_CRYSTAL, 1 + random.nextInt(2)));
            for (PlayerEntity p : sw.getPlayers()) {
                if (p.distanceTo(this) <= 40f) {
                    PlayerRpgManager.addXp(p, 500);
                    p.sendMessage(Text.literal("Goblin King defeated! +500 XP!").formatted(Formatting.GOLD), false);
                }
            }
        }
        broadcast(Text.literal("The Goblin King has been slain!").formatted(Formatting.GREEN));
    }

    private void broadcast(Text msg) {
        if (getWorld() instanceof ServerWorld sw)
            sw.getPlayers().stream().filter(p -> p.distanceTo(this) <= 80f).forEach(p -> p.sendMessage(msg, false));
    }

    @Override public void onStartedTrackingBy(ServerPlayerEntity p) { super.onStartedTrackingBy(p); bossBar.addPlayer(p); }
    @Override public void onStoppedTrackingBy(ServerPlayerEntity p) { super.onStoppedTrackingBy(p); bossBar.removePlayer(p); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_PILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_PILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_PILLAGER_DEATH; }
}
