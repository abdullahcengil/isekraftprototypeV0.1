package com.isekraft.entity;

import com.isekraft.entity.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Lightning Arrow — fired by the Kirin bow.
 * On entity hit: summons a real lightning bolt at the target's position.
 * The lightning interacts correctly with vanilla mechanics (mobs, creepers, beds, etc.)
 * Renders with portal particle trail for visual distinction.
 */
public class LightningArrowEntity extends ArrowEntity {

    public LightningArrowEntity(World world, LivingEntity shooter) {
        super(world, shooter);
    }

    /** Required constructor for entity type loading from NBT. */
    public LightningArrowEntity(EntityType<? extends ArrowEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void onEntityHit(EntityHitResult hitResult) {
        super.onEntityHit(hitResult); // apply normal arrow damage first

        Entity target = hitResult.getEntity();
        if (target.getWorld() instanceof ServerWorld sw) {
            // Spawn real lightning bolt at target
            LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(sw);
            if (lightning != null) {
                BlockPos pos = target.getBlockPos();
                lightning.refreshPositionAfterTeleport(pos.getX(), pos.getY(), pos.getZ());
                lightning.setCosmetic(false); // real lightning, does damage
                sw.spawnEntity(lightning);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Particle trail while in flight
        if (getWorld().isClient && !isOnGround()) {
            getWorld().addParticle(ParticleTypes.PORTAL,
                getX(), getY(), getZ(), 0, 0, 0);
        }
    }
}
