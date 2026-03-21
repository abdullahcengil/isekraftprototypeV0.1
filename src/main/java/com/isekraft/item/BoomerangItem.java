package com.isekraft.item;

import com.isekraft.entity.BoomerangEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
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
 * Boomerang — throwable weapon that returns to the thrower.
 *
 * Throw: right-click. Flies forward, damages enemies on the way out,
 * then arcs back. Catches if player holds right-click (or just flies back).
 *
 * Damage: 6 on outbound hit, 4 on return hit.
 * Cooldown: 2 seconds (only applies once the boomerang returns/lands).
 * Stackable: no (maxCount=1 — it's a returning weapon).
 *
 * Craft: 2 Steel Ingots + 1 Stick in an L shape:
 *   S I
 *   I
 * where S=Stick, I=Steel Ingot
 */
public class BoomerangItem extends Item {

    public BoomerangItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if (world.isClient) return TypedActionResult.success(stack);

        BoomerangEntity boomerang = new BoomerangEntity(world, user);
        boomerang.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 1.8f, 0.5f);
        world.spawnEntity(boomerang);

        world.playSound(null, user.getBlockPos(),
            SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.6f);

        // Remove from inventory while in flight (restored on return/land)
        if (!user.getAbilities().creativeMode)
            stack.decrement(1);

        return TypedActionResult.success(stack, false);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("A curved blade of hardened steel.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("✦ Thrown and returns to thrower").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("  6 damage outbound, 4 damage return").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("  Crafted from Steel Ingots").formatted(Formatting.DARK_GRAY));
    }
}
