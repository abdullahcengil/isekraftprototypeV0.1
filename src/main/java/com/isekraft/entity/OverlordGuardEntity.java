package com.isekraft.entity;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import java.util.UUID;
import net.minecraft.entity.LivingEntity;

/**
 * Overlord Guard — personal minion of an Isekai Overlord (level 100).
 *
 * COMMANDS (via OverlordSealItem):
 *   - Right-click air  → summon guard (max 4) / dismiss all if at max
 *   - Right-click mob  → all guards target that mob
 *   - Sneak+use        → toggle attack-all-hostiles mode
 *
 * Behaviour:
 *   - Follows owner passively (teleports if >24 blocks behind)
 *   - Attacks assigned target or all hostiles if in that mode
 *   - Never targets players (multiplayer safe)
 *   - Despawns after MAX_LIFETIME ticks (10 min)
 *   - Persists through world save/load via NBT
 */
public class OverlordGuardEntity extends HostileEntity {

    private UUID  ownerUuid     = null;
    private UUID  targetUuid    = null;
    private boolean attackAllMobs = false;
    private int   lifetimeTicks = 0;
    private static final int MAX_LIFETIME = 20 * 60 * 10; // 10 minutes

    public OverlordGuardEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
            .add(EntityAttributes.GENERIC_ARMOR, 10.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.7));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8f));
        targetSelector.add(2, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false,
            mob -> shouldTargetMob(mob)));
    }

    private boolean shouldTargetMob(LivingEntity mob) {
        // Hard rules: never attack players or other guards
        if (mob instanceof PlayerEntity) return false;
        if (mob instanceof OverlordGuardEntity) return false;
        // Only attack hostile mobs — never passives (SpiritBeast, ForestWolf, etc.)
        if (!(mob instanceof HostileEntity)) return false;
        // Specific target assigned by player
        if (targetUuid != null) return mob.getUuid().equals(targetUuid);
        // Attack-all-hostiles mode
        if (attackAllMobs) return true;
        return false;
    }

    // ── COMMAND API ───────────────────────────────────────────────────────────

    public void setOwner(UUID uuid)         { this.ownerUuid = uuid; }
    public UUID getOwnerUuid()              { return ownerUuid; }
    public boolean isAttackingAllMobs()     { return attackAllMobs; }
    public UUID getTargetUuid()             { return targetUuid; }

    public void setTargetEntity(UUID id)    { this.targetUuid = id; this.attackAllMobs = false; }
    public void setAttackAllMobs(boolean v) { this.attackAllMobs = v; if (v) this.targetUuid = null; }
    public void setFollowMode()             { this.targetUuid = null; this.attackAllMobs = false; setTarget(null); }

    // ── TICK ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;

        if (++lifetimeTicks >= MAX_LIFETIME) {
            notifyOwnerDeath();
            discard();
            return;
        }

        // Follow owner
        if (ownerUuid != null && getTarget() == null && !attackAllMobs) {
            // FIX: cast to ServerWorld to access getPlayerByUuid
            if (getWorld() instanceof ServerWorld sw) {
                PlayerEntity owner = sw.getPlayerByUuid(ownerUuid);
                if (owner != null) {
                    double dist = distanceTo(owner);
                    if (dist > 6.0)  getNavigation().startMovingTo(owner, 1.0);
                    if (dist > 24.0) teleport(
                        owner.getX() + (random.nextDouble() - 0.5) * 3,
                        owner.getY(),
                        owner.getZ() + (random.nextDouble() - 0.5) * 3);
                }
            }
        }
    }

    private void notifyOwnerDeath() {
        if (ownerUuid == null) return;
        if (getWorld() instanceof ServerWorld sw) {
            PlayerEntity owner = sw.getPlayerByUuid(ownerUuid);
            if (owner != null)
                owner.sendMessage(Text.literal("Your Overlord Guard has faded away.")
                    .formatted(Formatting.DARK_GRAY), false);
        }
    }

    // ── INIT ──────────────────────────────────────────────────────────────────

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty diff,
                                  SpawnReason reason, EntityData data, NbtCompound nbt) {
        EntityData result = super.initialize(world, diff, reason, data, nbt);
        // Enchanted iron armor + iron sword, no drops
        equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        equipStack(EquipmentSlot.HEAD,  enchant(new ItemStack(Items.IRON_HELMET), 3));
        equipStack(EquipmentSlot.CHEST, enchant(new ItemStack(Items.IRON_CHESTPLATE), 3));
        equipStack(EquipmentSlot.LEGS,  enchant(new ItemStack(Items.IRON_LEGGINGS), 2));
        equipStack(EquipmentSlot.FEET,  enchant(new ItemStack(Items.IRON_BOOTS), 2));
        for (EquipmentSlot slot : EquipmentSlot.values()) setEquipmentDropChance(slot, 0f);
        return result;
    }

    private static ItemStack enchant(ItemStack stack, int level) {
        stack.addEnchantment(Enchantments.PROTECTION, level);
        return stack;
    }

    // ── PROPERTIES ────────────────────────────────────────────────────────────

    @Override public boolean cannotDespawn()          { return true; }
    @Override public boolean isDisallowedInPeaceful() { return false; }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity p) { super.onStartedTrackingBy(p); }
    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity p) { super.onStoppedTrackingBy(p); }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (ownerUuid  != null) nbt.putUuid("OverlordOwner",  ownerUuid);
        if (targetUuid != null) nbt.putUuid("OverlordTarget", targetUuid);
        nbt.putBoolean("AttackAllMobs",  attackAllMobs);
        nbt.putInt("LifetimeTicks", lifetimeTicks);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("OverlordOwner"))  ownerUuid  = nbt.getUuid("OverlordOwner");
        if (nbt.containsUuid("OverlordTarget")) targetUuid = nbt.getUuid("OverlordTarget");
        attackAllMobs = nbt.getBoolean("AttackAllMobs");
        lifetimeTicks = nbt.getInt("LifetimeTicks");
    }

    @Override protected SoundEvent getAmbientSound()            { return SoundEvents.ENTITY_ZOMBIE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound()              { return SoundEvents.ENTITY_ZOMBIE_DEATH; }
}
