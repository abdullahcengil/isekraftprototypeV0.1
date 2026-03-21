package com.isekraft.entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class ForestWolfEntity extends PathAwareEntity {

    private boolean angry = false;

    public ForestWolfEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.add(4, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        goalSelector.add(6, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand == Hand.MAIN_HAND && player.getStackInHand(hand).isEmpty() && !getWorld().isClient) {
            player.sendMessage(Text.literal(
                "Forest Wolf  HP: " + (int) getHealth() + "/" + (int) getMaxHealth()
                + "  [" + (angry ? "ANGRY" : "Neutral") + "]")
                .formatted(angry ? Formatting.RED : Formatting.GREEN), false);
            return ActionResult.SUCCESS;
        }
        return super.interactMob(player, hand);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hit = super.damage(source, amount);
        if (hit && !angry && source.getAttacker() instanceof LivingEntity attacker) {
            angry = true;
            getWorld().getEntitiesByClass(ForestWolfEntity.class,
                getBoundingBox().expand(14), w -> w != this && !w.angry
            ).forEach(wolf -> {
                wolf.angry = true;
                wolf.setTarget(attacker);
                getWorld().playSound(null, wolf.getBlockPos(),
                    SoundEvents.ENTITY_WOLF_GROWL, SoundCategory.NEUTRAL, 1f, 1f);
            });
        }
        return hit;
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        dropStack(new ItemStack(Items.RABBIT_HIDE, 1 + random.nextInt(2)));
        if (random.nextBoolean()) dropStack(new ItemStack(Items.BEEF));
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_WOLF_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_WOLF_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_WOLF_DEATH; }
}
