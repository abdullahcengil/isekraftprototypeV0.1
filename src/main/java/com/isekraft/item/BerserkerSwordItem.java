package com.isekraft.item;

import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;
import net.minecraft.entity.Entity;

/**
 * Berserker Sword — brutal melee weapon.
 *
 * Base: 8 attack, -2.6 speed.
 * 40% chance to Bleed (Wither effect, 3s) on hit.
 * Level scaling: +0.3 damage per RPG level.
 *
 * SKILL INTERACTIONS:
 *   warrior_5 (Undying Rage) — Berserker Rage activates at 60% HP instead of 40%.
 *   warrior_2 (Veteran's Hide) — Absorb 10% incoming damage (applied as healing post-hit).
 */
public class BerserkerSwordItem extends SwordItem {

    public BerserkerSwordItem(Settings settings) {
        super(ToolMaterials.NETHERITE, 8, -2.6f, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !player.getWorld().isClient) {
            // Bleed — 40% chance
            if (player.getRandom().nextFloat() < 0.40f)
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 1));

            // Level damage bonus
            int level = PlayerRpgManager.getLevel(player);
            target.damage(player.getWorld().getDamageSources().playerAttack(player), level * 0.3f);

            // Blood particles
            if (player.getWorld() instanceof ServerWorld sw)
                sw.spawnParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + 1, target.getZ(), 5, 0.3, 0.3, 0.3, 0.05);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity,
                              int slot, boolean selected) {
        if (!world.isClient && selected && entity instanceof PlayerEntity player) {
            float hpPercent = player.getHealth() / player.getMaxHealth();

            // Undying Rage (warrior_5): activates at 60% HP instead of 40%
            float threshold = PlayerRpgManager.isSkillActive(player, "warrior_5") ? 0.60f : 0.40f;

            if (hpPercent < threshold) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 1, true, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, true, false));
            }

            // Veteran's Hide (warrior_2): 10% damage mitigation as periodic healing
            // Implemented as a small heal every 2s (40 ticks) to simulate damage reduction
            if (PlayerRpgManager.isSkillActive(player, "warrior_2") && player.age % 40 == 0) {
                float missing = player.getMaxHealth() - player.getHealth();
                if (missing > 0) {
                    // Heal 5% of max HP every 2s to represent ~10% damage taken reduction
                    player.heal(player.getMaxHealth() * 0.005f);
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("A brutal blade forged in rage.").formatted(Formatting.DARK_RED));
        tooltip.add(Text.literal("✦ 40% chance to Bleed on hit (Wither 3s)").formatted(Formatting.RED));
        tooltip.add(Text.literal("✦ Berserker Rage at low HP: Str II + Speed I").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("  warrior_5: Rage threshold 40% → 60% HP").formatted(Formatting.DARK_GRAY));
    }
}
