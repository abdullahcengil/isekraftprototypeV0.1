package com.isekraft.item;

import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Arcane Staff — Right-click shoots a magic fireball.
 * Power scales with RPG level. Base cooldown: 40 ticks (2s).
 *
 * SKILL INTERACTIONS:
 *   mage_1 (Arcane Focus)   — cooldown reduced to 30 ticks (1.5s)
 *   mage_5 (Arcane Barrage) — fires 3 fireballs in a spread pattern
 */
public class MagicStaffItem extends Item {

    private static final int COOLDOWN_BASE    = 40; // 2 seconds
    private static final int COOLDOWN_FOCUS   = 30; // 1.5 seconds (mage_1)

    public MagicStaffItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);

        if (!world.isClient) {
            int level = PlayerRpgManager.getLevel(user);
            Vec3d look   = user.getRotationVec(1f);
            Vec3d origin = user.getEyePos().add(look.multiply(1.2));

            boolean barrage = PlayerRpgManager.isSkillActive(user, "mage_5");

            if (barrage) {
                // Triple shot spread
                shootFireball(world, user, origin, look, level, 0, 0);
                shootFireball(world, user, origin, look, level, 8f, 0);
                shootFireball(world, user, origin, look, level, -8f, 0);
            } else {
                shootFireball(world, user, origin, look, level, 0, 0);
            }

            if (world instanceof ServerWorld sw)
                sw.spawnParticles(ParticleTypes.ENCHANT, origin.x, origin.y, origin.z,
                    barrage ? 20 : 10, 0.2, 0.2, 0.2, 0.05);

            world.playSound(null, user.getBlockPos(),
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1f, 1.4f);

            stack.damage(1, user, p -> p.sendToolBreakStatus(hand));
        }

        boolean hasFocus = PlayerRpgManager.isSkillActive(user, "mage_1");
        user.getItemCooldownManager().set(this, hasFocus ? COOLDOWN_FOCUS : COOLDOWN_BASE);
        return TypedActionResult.success(stack, world.isClient());
    }

    private void shootFireball(World world, PlayerEntity user, Vec3d origin,
                               Vec3d look, int level,
                               float pitchOffset, float yawOffset) {
        // Apply small pitch/yaw offset for spread
        float pitch = user.getPitch() + pitchOffset;
        float yaw   = user.getYaw()   + yawOffset;
        double rad  = Math.PI / 180.0;
        double dx   = -Math.sin(yaw * rad) * Math.cos(pitch * rad);
        double dy   = -Math.sin(pitch * rad);
        double dz   =  Math.cos(yaw * rad) * Math.cos(pitch * rad);

        double speed = 1.5 + level * 0.01;
        SmallFireballEntity ball = new SmallFireballEntity(world, user,
            dx * speed, dy * speed, dz * speed);
        ball.setPos(origin.x, origin.y, origin.z);
        world.spawnEntity(ball);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("A staff crackling with arcane energy.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("✦ Right-click: launch a magic fireball").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("  Power scales with RPG Level").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("  mage_1: -0.5s cooldown").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("  mage_5: triple shot spread").formatted(Formatting.DARK_GRAY));
    }
}
