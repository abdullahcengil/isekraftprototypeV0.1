package com.isekraft.equipment;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for all IseKraft RPG accessories (gloves, necklaces, rings).
 *
 * Stat fields (applied by EquipmentManager.applyEquipmentStats each tick):
 *   hpBonus       — flat max HP addition
 *   damageBonus   — flat attack damage addition
 *   speedBonus    — movement speed addition (0.005 ≈ 5%)
 *   passiveEffect — optional permanent status effect while equipped
 *   effectAmp     — amplifier for passiveEffect (0 = level I)
 *   levelRequired — minimum player level to benefit from this item's stats
 *
 * Auto-equip priority is determined by: tierPriority (higher = better).
 */
public class EquipmentItem extends Item {

    public final EquipSlot slot;
    public final int       levelRequired;
    public final int       tierPriority;   // 1=basic, 2=mid, 3=endgame — used by auto-equip
    public final int       hpBonus;
    public final float     damageBonus;
    public final float     speedBonus;
    @Nullable
    public final StatusEffect passiveEffect;
    public final int          effectAmp;

    private final String flavourText;

    public EquipmentItem(Settings settings,
                         EquipSlot slot,
                         int levelRequired,
                         int tierPriority,
                         int hpBonus,
                         float damageBonus,
                         float speedBonus,
                         @Nullable StatusEffect passiveEffect,
                         int effectAmp,
                         String flavourText) {
        super(settings);
        this.slot           = slot;
        this.levelRequired  = levelRequired;
        this.tierPriority   = tierPriority;
        this.hpBonus        = hpBonus;
        this.damageBonus    = damageBonus;
        this.speedBonus     = speedBonus;
        this.passiveEffect  = passiveEffect;
        this.effectAmp      = effectAmp;
        this.flavourText    = flavourText;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world,
                              List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(slot.label).formatted(Formatting.GOLD));
        tooltip.add(Text.literal("Required Level: " + levelRequired).formatted(Formatting.DARK_GRAY));

        if (hpBonus > 0)
            tooltip.add(Text.literal("+" + hpBonus + " Max HP").formatted(Formatting.RED));
        if (damageBonus > 0)
            tooltip.add(Text.literal("+" + damageBonus + " Attack Damage").formatted(Formatting.RED));
        if (speedBonus > 0)
            tooltip.add(Text.literal("+" + Math.round(speedBonus * 1000f) / 10f + "% Speed")
                           .formatted(Formatting.GREEN));
        if (passiveEffect != null) {
            String effName = passiveEffect.getName().getString();
            tooltip.add(Text.literal("Passive: " + effName + " " + toRoman(effectAmp + 1))
                           .formatted(Formatting.LIGHT_PURPLE));
        }

        tooltip.add(Text.empty());
        tooltip.add(Text.literal(flavourText).formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        tooltip.add(Text.literal("Auto-equips when picked up.").formatted(Formatting.GRAY));
    }

    private static String toRoman(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; default -> String.valueOf(n); };
    }
}
