package com.isekraft.entity;

import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Gilgamesh — King of Heroes. Legendary sky island boss.
 *
 * Stats:
 *   HP:     1200  (60x a player)
 *   Damage: 18 base (before armour, will one-shot unprepared players)
 *   Speed:  0.38 (faster than a sprint)
 *   Armour: 20 (full diamond equivalent)
 *
 * Phases:
 *   Phase 1 (HP > 50%): melee + golden arrow volley every 5s
 *   Phase 2 (HP ≤ 50%): enrages — speed x1.5, summons 3 Gate Arrows (entity projectiles),
 *                        permanent Strength V + Resistance III, full server broadcast
 *
 * Special abilities:
 *   • Gate of Babylon (phase 2): fires 5 arrows in a spread cone every 3s
 *   • King's Presence: nearby players (≤16) get Weakness II + Slowness II for 3s
 *   • Undying Will: at <10% HP, heals 200 HP once per life (max 1 use)
 *
 * Drops: Overlord Seal, Demon Core x5, Rune Fragment x16, Demon Lord Crown
 * Required player level for full XP: 80
 */
public class GilgameshEntity extends HostileEntity {

    private boolean phase2Triggered   = false;
    private boolean undyingUsed       = false;
    private int     abilityTick        = 0;
    private int     presenceTick       = 0;
    private static final int ARROW_CD  = 100;  // 5s
    private static final int GATE_CD   = 60;   // 3s in phase 2
    private static final int PRESENCE_CD = 40; // 2s debuff pulse

    public GilgameshEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 500;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,       1200.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,      18.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,      0.38)
            .add(EntityAttributes.GENERIC_ARMOR,               20.0)
            .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS,      8.0)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,  1.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,         48.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 16f));
        goalSelector.add(4, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(2, new RevengeGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;

        float hp    = getHealth();
        float maxHp = getMaxHealth();

        // ── Phase 2 trigger ─────────────────────────────────────────────────
        if (!phase2Triggered && hp <= maxHp * 0.5f) {
            phase2Triggered = true;
            onPhase2();
        }

        // ── Undying Will ─────────────────────────────────────────────────────
        if (!undyingUsed && hp <= maxHp * 0.10f) {
            undyingUsed = true;
            setHealth(Math.min(maxHp, hp + 200f));
            addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 400, 9, false, false));
            spawnParticles(30);
            getWorld().playSound(null, getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN,
                SoundCategory.HOSTILE, 1.5f, 0.5f);
            broadcastToNear("§6[Gilgamesh]: §eUnworthy. The Grail belongs to me alone!");
        }

        // ── King's Presence (debuff nearby players) ──────────────────────────
        presenceTick++;
        if (presenceTick >= PRESENCE_CD) {
            presenceTick = 0;
            getWorld().getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(16),
                p -> true).forEach(p -> {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,  60, 1, true, false));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,  60, 1, true, false));
            });
        }

        // ── Ability cooldown ─────────────────────────────────────────────────
        abilityTick++;
        int cd = phase2Triggered ? GATE_CD : ARROW_CD;
        if (abilityTick >= cd) {
            abilityTick = 0;
            if (getTarget() instanceof PlayerEntity target) {
                if (phase2Triggered) gateOfBabylon(target);
                else                 fireGoldenArrow(target);
            }
        }

        // ── Phase 2 particles ─────────────────────────────────────────────────
        if (phase2Triggered && getWorld().getTime() % 4 == 0) spawnParticles(3);
    }

    private void onPhase2() {
        // Speed + power boost
        if (getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED) != null)
            getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.57);
        addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,   99999, 4, false, false));
        addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 99999, 2, false, false));

        // Dramatic particles + sound
        spawnParticles(60);
        getWorld().playSound(null, getBlockPos(), SoundEvents.ENTITY_WITHER_DEATH,
            SoundCategory.HOSTILE, 2f, 0.7f);

        // Server-wide broadcast
        if (getWorld() instanceof ServerWorld sw) {
            sw.getServer().getPlayerManager().broadcast(
                Text.literal("☠ §6GILGAMESH§r — KING OF HEROES — §cENRAGED! §7Half the world trembles.")
                    .formatted(Formatting.BOLD), false);
        }
    }

    /** Single golden arrow aimed at target */
    private void fireGoldenArrow(PlayerEntity target) {
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - getEyeY();
        double dz = target.getZ() - getZ();
        ArrowEntity arrow = new ArrowEntity(getWorld(), this);
        arrow.setPos(getX(), getEyeY(), getZ());
        double dist = Math.sqrt(dx * dx + dz * dz);
        arrow.setVelocity(dx / dist, dy / dist + 0.15, dz / dist, 3.2f, 0.5f);
        arrow.setDamage(12.0);
        arrow.setCritical(true);
        getWorld().spawnEntity(arrow);
        getWorld().playSound(null, getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT,
            SoundCategory.HOSTILE, 1f, 0.8f);
    }

    /** Gate of Babylon — 5-arrow spread cone */
    private void gateOfBabylon(PlayerEntity target) {
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - getEyeY();
        double dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        for (int i = -2; i <= 2; i++) {
            double angle = Math.atan2(dz, dx) + i * 0.18;
            ArrowEntity arrow = new ArrowEntity(getWorld(), this);
            arrow.setPos(getX(), getEyeY(), getZ());
            arrow.setVelocity(
                Math.cos(angle) * 3.0,
                dy / Math.max(dist, 1) + 0.1,
                Math.sin(angle) * 3.0,
                3.0f, 0.3f
            );
            arrow.setDamage(16.0);
            arrow.setPierceLevel((byte) 3);
            arrow.setCritical(true);
            getWorld().spawnEntity(arrow);
        }
        getWorld().playSound(null, getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
            SoundCategory.HOSTILE, 1.5f, 1.2f);
        spawnParticles(15);
    }

    private void spawnParticles(int count) {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        for (int i = 0; i < count; i++) {
            double ox = (random.nextDouble() - 0.5) * 2;
            double oy = random.nextDouble() * 3;
            double oz = (random.nextDouble() - 0.5) * 2;
            sw.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                getX() + ox, getY() + oy, getZ() + oz,
                1, 0, 0, 0, 0.1);
        }
    }

    private void broadcastToNear(String msg) {
        getWorld().getEntitiesByClass(PlayerEntity.class, getBoundingBox().expand(64),
            p -> true).forEach(p ->
            p.sendMessage(Text.literal(msg), false));
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (getWorld() instanceof ServerWorld sw) {
            sw.getServer().getPlayerManager().broadcast(
                Text.literal("★ §6Gilgamesh§r, King of Heroes, has been slain! A legend ends.")
                    .formatted(Formatting.GOLD, Formatting.BOLD), false);
            // Grant XP and notify player who killed
            if (source.getAttacker() instanceof ServerPlayerEntity sp) {
                PlayerRpgManager.addXp(sp, 3000);
                sp.sendMessage(Text.literal("✦ +3000 XP — You defeated the King of Heroes!").formatted(Formatting.GOLD), false);
            }
        }
    }

    @Override public boolean isUndead() { return false; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Phase2", phase2Triggered);
        nbt.putBoolean("UndyingUsed", undyingUsed);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        phase2Triggered = nbt.getBoolean("Phase2");
        undyingUsed     = nbt.getBoolean("UndyingUsed");
    }
}
