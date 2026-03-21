package com.isekraft.entity;

import com.isekraft.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * Shuriken projectile — thrown by ShurikenItem.
 *
 * Damage model:
 *   - Base damage: 6
 *   - Close range (<4 blocks): 12 damage (doubled — spinning blade impact)
 *   - Uses ThrownItemEntity base (same as snowball/egg) for physics
 *
 * Particle: CRIT trail while flying for visual feedback.
 */
public class ShurikenEntity extends ThrownItemEntity {

    public ShurikenEntity(EntityType<? extends ThrownItemEntity> type, World world) {
        super(type, world);
    }

    public ShurikenEntity(World world, LivingEntity thrower) {
        super(ModEntities.SHURIKEN_PROJECTILE, thrower, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SHURIKEN;
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        if (getWorld().isClient) return;
        if (!(hit.getEntity() instanceof LivingEntity target)) return;

        // Close-range bonus: doubled damage within 4 blocks
        double distSq = getOwner() != null ? squaredDistanceTo(getOwner()) : Double.MAX_VALUE;
        float damage = distSq < 16.0 ? 12f : 6f; // 4*4=16

        DamageSource src = getOwner() instanceof PlayerEntity p
            ? getWorld().getDamageSources().playerAttack(p)
            : getWorld().getDamageSources().thrown(this, getOwner());

        target.damage(src, damage);

        // Spark particles on hit
        if (getWorld() instanceof ServerWorld sw)
            sw.spawnParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1, target.getZ(),
                8, 0.3, 0.3, 0.3, 0.1);
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!getWorld().isClient) discard();
    }

    @Override
    public void tick() {
        super.tick();
        // Crit particle trail
        if (getWorld().isClient && !isOnGround())
            getWorld().addParticle(ParticleTypes.CRIT,
                getX(), getY(), getZ(), 0, 0, 0);
    }
}
