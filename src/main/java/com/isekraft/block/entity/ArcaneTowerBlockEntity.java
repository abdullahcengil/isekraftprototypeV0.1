package com.isekraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

/**
 * Arcane Tower Block Entity.
 * Every 20 ticks (1 second): scans 16 blocks for hostile mobs, shoots the closest one.
 */
public class ArcaneTowerBlockEntity extends BlockEntity {

    private static final int RANGE  = 16;
    private static final int PERIOD = 20;

    private int tick = 0;
    private int totalShots = 0;
    private int lastTargets = 0;

    public ArcaneTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARCANE_TOWER_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ArcaneTowerBlockEntity be) {
        if (++be.tick < PERIOD) return;
        be.tick = 0;

        Box box = Box.of(Vec3d.ofCenter(pos), RANGE * 2, RANGE * 2, RANGE * 2);
        List<HostileEntity> mobs = world.getEntitiesByClass(
            HostileEntity.class, box, e -> e.isAlive() && !e.isInvisible());

        be.lastTargets = mobs.size();
        if (mobs.isEmpty()) return;

        Vec3d center = Vec3d.ofCenter(pos);
        HostileEntity target = mobs.stream()
            .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(center)))
            .orElse(null);

        if (target == null) return;

        // Shoot arrow from top of tower toward target
        Vec3d origin = center.add(0, 1.5, 0);
        Vec3d dir = target.getPos().add(0, target.getHeight() * 0.5, 0)
            .subtract(origin).normalize();

        ArrowEntity arrow = new ArrowEntity(world, origin.x, origin.y, origin.z);
        arrow.setDamage(6.0);
        arrow.pickupType = ArrowEntity.PickupPermission.DISALLOWED;
        arrow.setVelocity(dir.x, dir.y, dir.z, 1.8f, 0.5f);
        world.spawnEntity(arrow);

        world.playSound(null, pos,
            SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.BLOCKS, 0.7f, 1.2f);
        be.totalShots++;
    }

    public int getTargetCount() { return lastTargets; }
    public int getTotalShots()  { return totalShots; }
}
