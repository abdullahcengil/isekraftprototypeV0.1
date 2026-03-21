package com.isekraft.item;

import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * War Hammer — slow but powerful melee weapon.
 *
 * Stats vs equivalent sword tier:
 *   - Attack speed: -3.4 (very slow, sword is -2.4)
 *   - Attack damage: +4 above sword tier
 *   - On hit: 25% chance to Slow target (Slowness II, 2s) — "concussion"
 *   - Smash effect: knockback particles
 *
 * Craft: shaped recipe (H=material, S=stick):
 *   H H
 *   H H
 *     S
 *     S
 *
 * Available materials: WOOD / STONE / IRON / GOLD / DIAMOND
 * Durability: same as sword of same material.
 */
public class WarHammerItem extends SwordItem {

    public WarHammerItem(ToolMaterial material, int extraDamage, Settings settings) {
        // +4 damage over sword baseline, -3.4 speed (very slow)
        super(material, extraDamage + 4, -3.4f, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient && attacker instanceof PlayerEntity player) {
            // Concussion: 25% chance Slowness II (2 seconds)
            if (player.getRandom().nextFloat() < 0.25f)
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));

            // Smash knockback particles
            if (attacker.getWorld() instanceof ServerWorld sw)
                sw.spawnParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getHeight() * 0.5,
                    target.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("A heavy hammer forged for battle.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("✦ +4 damage over equivalent sword").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("✦ 25% chance: Concussion (Slowness II)").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("  Very slow attack speed (-3.4)").formatted(Formatting.DARK_GRAY));
    }
}
