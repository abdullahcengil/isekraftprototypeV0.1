package com.isekraft.item;

import com.isekraft.armor.ModArmorMaterials;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;
import net.minecraft.entity.Entity;

/**
 * Demon Lord Crown — reward for defeating the Shadow Demon.
 * Gives Strength III, Resistance II, Speed II while worn.
 * Uses the existing DEMON_KING armor material (safe, already registered).
 */
public class DemonLordCrownItem extends ArmorItem {

    public DemonLordCrownItem(Settings settings) {
        // Reuse DEMON_KING material — avoids creating a new unregistered material
        super(ModArmorMaterials.DEMON_KING, Type.HELMET, settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && entity instanceof PlayerEntity player) {
            if (player.getEquippedStack(EquipmentSlot.HEAD).isOf(this)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,  60, 2, true, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 1, true, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,      60, 1, true, false));
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Crown of the Demon Lord.").formatted(Formatting.DARK_RED, Formatting.BOLD));
        tooltip.add(Text.literal("Reward for defeating the Shadow Demon.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("✦ Strength III + Resistance II + Speed II").formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("  (while worn)").formatted(Formatting.DARK_GRAY));
    }
}
