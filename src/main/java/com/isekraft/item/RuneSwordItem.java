package com.isekraft.item;

import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
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
 * Rune Sword — scales damage with player RPG level.
 * Base: 5 attack. Bonus: +0.5 per level.
 *
 * SKILL INTERACTIONS:
 *   warrior_3 (War Mastery) — +25% bonus damage multiplier applied on top of level scaling.
 */
public class RuneSwordItem extends SwordItem {

    public RuneSwordItem(ToolMaterial material, int atk, float speed, Settings settings) {
        super(material, atk, speed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !attacker.getWorld().isClient) {
            int level  = PlayerRpgManager.getLevel(player);
            float bonus = level * 0.5f;

            // War Mastery (warrior_3): +25% bonus damage
            if (PlayerRpgManager.isSkillActive(player, "warrior_3"))
                bonus *= 1.25f;

            target.damage(player.getWorld().getDamageSources().playerAttack(player), bonus);

            if (attacker.getWorld() instanceof ServerWorld sw)
                sw.spawnParticles(ParticleTypes.ENCHANT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    8, 0.3, 0.5, 0.3, 0.1);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("A blade etched with ancient runes.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("✦ Damage scales with RPG Level (+0.5/lvl)").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("  warrior_3: +25% bonus damage (War Mastery)").formatted(Formatting.DARK_GRAY));
    }
}
