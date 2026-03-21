package com.isekraft.item;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Hand;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.List;

/** Health Potion — right-click to heal 10 HP instantly. Cooldown 30s. */
public class HealthPotionItem extends Item {

    public HealthPotionItem(Settings s) { super(s); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);

        if (!world.isClient) {
            user.heal(10f);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
            world.playSound(null, user.getBlockPos(),
                SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1f, 1f);
            if (!user.getAbilities().creativeMode) stack.decrement(1);
        }
        user.getItemCooldownManager().set(this, 600);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Right-click: Heal 10 HP + Regen").formatted(Formatting.RED));
        tooltip.add(Text.literal("30 second cooldown").formatted(Formatting.DARK_GRAY));
    }
}
