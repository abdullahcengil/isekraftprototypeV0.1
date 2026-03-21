package com.isekraft.item;

import com.isekraft.entity.LightningArrowEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * Kirin — legendary storm bow. Unbreakable. Dark purple.
 *
 * Shoots lightning-infused arrows — on hit, a real lightning bolt strikes the target.
 * Craft: expensive diamond + rune fragment + soul crystal recipe.
 * Unbreakable, stacksize 1, drops from no mob — craft only.
 */
public class KirinBowItem extends Item {

    private static final int COOLDOWN = 30; // 1.5s

    public KirinBowItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if (world.isClient) return TypedActionResult.success(stack);

        // Construct lightning arrow directly from shooter
        LightningArrowEntity arrow = new LightningArrowEntity(world, user);
        arrow.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 3.0f, 0.3f);
        arrow.setDamage(8.0);
        arrow.setGlowing(true);
        arrow.pickupType = ArrowEntity.PickupPermission.DISALLOWED;
        world.spawnEntity(arrow);

        world.playSound(null, user.getBlockPos(),
            SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.3f, 1.9f);
        world.playSound(null, user.getBlockPos(),
            SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1f, 0.7f);

        user.getItemCooldownManager().set(this, COOLDOWN);
        return TypedActionResult.success(stack, false);
    }

    @Override public boolean isDamageable() { return false; } // unbreakable

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Kirin").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        tooltip.add(Text.literal("A divine bow that calls down storms.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("✦ Arrows strike targets with lightning").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("✦ Unbreakable").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("  8 base damage + lightning on impact").formatted(Formatting.DARK_GRAY));
    }
}
