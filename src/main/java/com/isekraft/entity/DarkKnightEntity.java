package com.isekraft.entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;

import com.isekraft.item.ModItems;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

public class DarkKnightEntity extends HostileEntity {

    private int parryCooldown = 0;

    public DarkKnightEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)   // was 60 - halved
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0) // was 9 - much weaker
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23)
            .add(EntityAttributes.GENERIC_ARMOR, 4.0)         // was 10
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.1);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.add(3, new WanderAroundFarGoal(this, 0.7));
        goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(2, new RevengeGoal(this));
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!getWorld().isClient && parryCooldown <= 0 && random.nextFloat() < 0.15f) {
            amount *= 0.5f;
            parryCooldown = 60;
            getWorld().playSound(null, getBlockPos(),
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1f, 1f);
        }
        return super.damage(source, amount);
    }

    @Override
    public void tick() { super.tick(); if (parryCooldown > 0) parryCooldown--; }

    @Override
    public void onDeath(DamageSource src) {
        super.onDeath(src);
        dropStack(new ItemStack(ModItems.RUNE_FRAGMENT, 1 + random.nextInt(2)));
        if (random.nextInt(8) == 0) dropStack(new ItemStack(ModItems.SOUL_CRYSTAL));
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty diff,
                                 SpawnReason reason, EntityData data, NbtCompound nbt) {
        equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        return super.initialize(world, diff, reason, data, nbt);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_ZOMBIE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_ZOMBIE_DEATH; }
}
