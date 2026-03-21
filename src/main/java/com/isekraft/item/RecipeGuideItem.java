package com.isekraft.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import java.util.List;

/**
 * Recipe Guide — in-inventory reference for all IseKraft crafting recipes.
 * Given to players on first join. Tooltip covers every craftable item.
 */
public class RecipeGuideItem extends Item {
    public RecipeGuideItem(Settings s) { super(s); }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("✦ IseKraft Recipe Guide ✦").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(sep());

        // ── MATERIALS ────────────────────────────────────────────────────────
        tooltip.add(head("MATERIALS"));
        tooltip.add(line("Steel Ingot",     "3 Iron Ingots in a row"));
        tooltip.add(line("Iron Rune Shard", "Iron Ingot + Lapis + Iron Ingot (row)"));
        tooltip.add(line("Rune Fragment",   "4x Iron Rune Shard (shapeless)"));
        tooltip.add(line("Soul Crystal",    "8x Amethyst Shard around Nether Star"));
        tooltip.add(line("Spirit Essence",  "Feather+Soul Sand ring around Soul Crystal"));
        tooltip.add(line("Mana Crystal",    "Amethyst Shard + Blue Dye alternating ring"));
        tooltip.add(sep());

        // ── WEAPONS ──────────────────────────────────────────────────────────
        tooltip.add(head("WEAPONS"));
        tooltip.add(line("Rune Sword",      "Rune Fragment / Diamond Sword / Soul Crystal"));
        tooltip.add(line("Berserker Sword", "Demon Core / Netherite Sword / Rune Fragment"));
        tooltip.add(line("Arcane Staff",    "Rune Fragment / Blaze Rod / Soul Crystal"));
        tooltip.add(line("Soul Bow",        "Rune+String in bow shape"));
        tooltip.add(line("War Hammer (Wood/Stone/Iron/Gold/Diamond)",
            "2x2 head material + 2 sticks below"));
        tooltip.add(line("Kirin (Lightning Bow)",
            "Diamond+Rune Fragment ring around Soul Crystal"));
        tooltip.add(line("Shuriken",        "2x Steel Ingot → 2 Shurikens"));
        tooltip.add(line("Boomerang",       "Stick + 2 Steel Ingots (L shape)"));
        tooltip.add(sep());

        // ── ARMOR ─────────────────────────────────────────────────────────────
        tooltip.add(head("ARMOR"));
        tooltip.add(line("Soul Crystal Set",  "Standard armor shape, Soul Crystals"));
        tooltip.add(line("Demon King Set",     "Standard armor shape, Demon Cores"));
        tooltip.add(sep());

        // ── UTILITY ──────────────────────────────────────────────────────────
        tooltip.add(head("UTILITY"));
        tooltip.add(line("Teleport Stone",  "Ender Pearl + Compass + Soul Crystal ring"));
        tooltip.add(line("Health Potion",   "Glass Bottle + Mana Crystal + Melon Slice ring"));
        tooltip.add(line("Mount Whistle",   "Soul Crystal + Bone ring around Ancient Coin"));
        tooltip.add(line("Arcane Tower",    "Soul Crystal + Iron Block ring around Blaze Rod"));
        tooltip.add(sep());

        tooltip.add(Text.literal("TIP: Install JEI to see recipes in-game!").formatted(Formatting.GREEN));
    }

    private static Text head(String s) {
        return Text.literal(s + ":").formatted(Formatting.YELLOW, Formatting.BOLD);
    }
    private static Text line(String name, String recipe) {
        return Text.literal("  " + name + ": ").formatted(Formatting.WHITE)
            .append(Text.literal(recipe).formatted(Formatting.GRAY));
    }
    private static Text sep() {
        return Text.literal("").formatted(Formatting.GRAY);
    }
}
