package com.isekraft.entity;

import com.isekraft.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Boomerang projectile.
 *
 * Flight phases:
 *   OUTBOUND (0-30 ticks): flies forward at original velocity, damages enemies (6 dmg)
 *   TURNING  (31-40 ticks): begins curving back toward thrower
 *   RETURN   (41+ ticks):   homes toward thrower position, damages enemies (4 dmg)
 *   CAUGHT:  when within 1.5 blocks of thrower — returned to inventory
 *
 * Falls to ground after 120 ticks if not caught (acts like normal item drop).
 */
public class BoomerangEntity extends ThrownItemEntity {

    private static final int OUTBOUND_TICKS = 30;
    private static final int RETURN_TICKS   = 40;
    private static final int MAX_LIFETIME   = 120;

    private int age = 0;
    private boolean returning = false;
    private boolean hitSomething = false; // track if we already hit on outbound

    public BoomerangEntity(EntityType<? extends ThrownItemEntity> type, World world) {
        super(type, world);
    }

    public BoomerangEntity(World world, LivingEntity thrower) {
        super(ModEntities.BOOMERANG_PROJECTILE, thrower, world);
    }

    @Override
    protected Item getDefaultItem() { return ModItems.BOOMERANG; }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;

        age++;

        // Spin particle effect
        if (age % 2 == 0 && getWorld() instanceof ServerWorld sw)
            sw.spawnParticles(ParticleTypes.CRIT,
                getX(), getY() + 0.1, getZ(), 2, 0.1, 0.1, 0.1, 0.02);

        LivingEntity owner = getOwner() instanceof LivingEntity l ? l : null;

        if (age >= RETURN_TICKS && owner != null) {
            returning = true;
            // Home toward owner
            Vec3d toOwner = owner.getPos().add(0, 0.5, 0).subtract(getPos()).normalize();
            setVelocity(toOwner.multiply(1.6));
            setNoGravity(true);

            // Check if caught
            if (distanceTo(owner) < 1.5) {
                // Return to inventory
                if (owner instanceof PlayerEntity p) {
                    if (!p.getAbilities().creativeMode) {
                        ItemStack boomerang = new ItemStack(ModItems.BOOMERANG);
                        if (!p.getInventory().insertStack(boomerang))
                            p.dropItem(boomerang, false);
                    }
                    p.getItemCooldownManager().set(ModItems.BOOMERANG, 40); // 2s cooldown on catch
                }
                getWorld().playSound(null, owner.getBlockPos(),
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.8f, 1.4f);
                discard();
                return;
            }
        } else if (age < RETURN_TICKS) {
            setNoGravity(true);
        }

        // Max lifetime — fall to ground as item
        if (age >= MAX_LIFETIME) {
            if (!(getOwner() instanceof PlayerEntity p && p.getAbilities().creativeMode)) {
                ItemEntity drop = new ItemEntity(
                    getWorld(), getX(), getY(), getZ(), new ItemStack(ModItems.BOOMERANG));
                getWorld().spawnEntity(drop);
            }
            discard();
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        if (getWorld().isClient) return;
        if (!(hit.getEntity() instanceof LivingEntity target)) return;
        if (getOwner() != null && hit.getEntity() == getOwner()) return; // don't hit self on return

        float damage = returning ? 4f : 6f;
        DamageSource src = getOwner() instanceof PlayerEntity p
            ? getWorld().getDamageSources().playerAttack(p)
            : getWorld().getDamageSources().thrown(this, getOwner());

        target.damage(src, damage);

        if (getWorld() instanceof ServerWorld sw)
            sw.spawnParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1, target.getZ(), 5, 0.3, 0.3, 0.3, 0.05);

        getWorld().playSound(null, target.getBlockPos(),
            SoundEvents.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 0.8f, 1.2f);

        // Don't discard on hit — pass through and keep flying
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        // Only process entity hits, not block hits (pass through blocks would be weird)
        if (hitResult.getType() == HitResult.Type.ENTITY)
            super.onCollision(hitResult);
        // Block hit: bounce back early
        if (hitResult.getType() == HitResult.Type.BLOCK && !returning) {
            returning = true;
            age = RETURN_TICKS; // skip to return phase
        }
    }
}
