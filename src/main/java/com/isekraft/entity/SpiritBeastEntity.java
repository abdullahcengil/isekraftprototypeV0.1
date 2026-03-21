package com.isekraft.entity;

import com.isekraft.item.ModItems;
import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.Formatting;

import java.util.UUID;
import net.minecraft.world.World;

/**
 * Spirit Beast — tameable companion.
 * Tame with Spirit Essence (33% chance). Right-click to sit/follow.
 *
 * SKILL INTERACTIONS:
 *   ranger_4 (Pack Leader) — Spirit Beast gets +10 attack damage when tamed.
 */
public class SpiritBeastEntity extends PathAwareEntity {

    private boolean tamed     = false;
    private boolean sitting   = false;
    private UUID    ownerUuid = null;

    public SpiritBeastEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(4, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6f));
        goalSelector.add(8, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!tamed) {
            if (stack.isOf(ModItems.SPIRIT_ESSENCE)) {
                // Return SUCCESS on client to consume the interaction and send packet to server
                if (getWorld().isClient) return ActionResult.SUCCESS;
                if (!player.getAbilities().creativeMode) stack.decrement(1);
                if (random.nextInt(3) == 0) {
                    tamed     = true;
                    sitting   = false;
                    ownerUuid = player.getUuid();
                    getWorld().sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
                    player.sendMessage(Text.literal("The Spirit Beast has bonded with you!")
                        .formatted(Formatting.LIGHT_PURPLE), false);
                    syncStats(player);
                } else {
                    getWorld().sendEntityStatus(this, EntityStatuses.ADD_NEGATIVE_PLAYER_REACTION_PARTICLES);
                    player.sendMessage(Text.literal("The Spirit Beast resists... try again!")
                        .formatted(Formatting.GRAY), false);
                }
                return ActionResult.SUCCESS;
            }
        } else if (ownerUuid != null && player.getUuid().equals(ownerUuid)) {
            if (getWorld().isClient) return ActionResult.SUCCESS;
            sitting = !sitting;
            player.sendMessage(Text.literal(sitting ? "Spirit Beast: sitting." : "Spirit Beast: following.")
                .formatted(Formatting.GRAY), false);
            return ActionResult.SUCCESS;
        }
        return super.interactMob(player, hand);
    }

    /** Sync stats from owner level + Pack Leader skill. */
    private void syncStats(PlayerEntity owner) {
        int level = PlayerRpgManager.getLevel(owner);

        double atkDmg = 6.0 + level * 0.2;
        // Pack Leader (ranger_4): +10 bonus attack damage
        if (PlayerRpgManager.isSkillActive(owner, "ranger_4")) atkDmg += 10.0;

        var atkInst = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        var hpInst  = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (atkInst != null) atkInst.setBaseValue(atkDmg);
        if (hpInst  != null) hpInst.setBaseValue(30.0 + level);
        setHealth(getMaxHealth());
    }

    @Override
    public void onDeath(DamageSource src) {
        super.onDeath(src);
        if (tamed && !getWorld().isClient && getWorld() instanceof ServerWorld sw) {
            PlayerEntity owner = ownerUuid != null ? sw.getPlayerByUuid(ownerUuid) : null;
            if (owner != null)
                owner.sendMessage(Text.literal("Your Spirit Beast has fallen...").formatted(Formatting.RED));
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Tamed", tamed);
        nbt.putBoolean("Sitting", sitting);
        if (ownerUuid != null) nbt.putUuid("OwnerUUID", ownerUuid);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        tamed   = nbt.getBoolean("Tamed");
        sitting = nbt.getBoolean("Sitting");
        if (nbt.containsUuid("OwnerUUID")) ownerUuid = nbt.getUuid("OwnerUUID");
    }
}
