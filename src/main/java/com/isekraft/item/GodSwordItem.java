package com.isekraft.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * Admin-only God Sword. One-shots everything. No durability loss.
 * Use /give @s isekraft:god_sword to obtain.
 */
public class GodSwordItem extends SwordItem {

    public GodSwordItem(Settings settings) {
        super(ToolMaterials.NETHERITE, 999, -2.4f, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // kill() onDeath event'lerini ve loot drop'ları doğru tetikler.
        // setHealth(0f) bazı entity'lerde (boss, invulnerable) çalışmaz.
        target.kill();
        return true;
    }

    @Override
    public boolean isDamageable() { return false; } // no durability loss

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Admin Only — One-Shot Kill").formatted(Formatting.RED, Formatting.BOLD));
        tooltip.add(Text.literal("No durability loss").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("/give @s isekraft:god_sword").formatted(Formatting.DARK_GRAY));
    }
}
