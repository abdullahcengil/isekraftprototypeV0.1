package com.isekraft.rarity;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Central rarity utility class.
 *
 * NBT keys written to ItemStack:
 *   "isekraft_rarity"       — int (ItemRarity.id)
 *   "isekraft_rarity_stats" — newline-separated display strings, e.g. "+4 Attack Damage\n+8% XP Gain"
 *
 * Stat bonuses are display-only in v1. Full mechanical application (attribute
 * modifiers, effect hooks) will be implemented alongside the Equipment Slot
 * system in the next major feature update.
 */
public class RarityManager {

    public static final String RARITY_KEY = "isekraft_rarity";
    public static final String STATS_KEY  = "isekraft_rarity_stats";

    // ── QUERY ────────────────────────────────────────────────────────────────

    public static boolean hasRarity(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(RARITY_KEY);
    }

    public static ItemRarity getRarity(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return ItemRarity.COMMON;
        return ItemRarity.fromId(nbt.getInt(RARITY_KEY));
    }

    // ── APPLY ────────────────────────────────────────────────────────────────

    /**
     * Stamps a rarity tier onto an ItemStack via NBT.
     * Also generates and stores display stat strings.
     * No-ops on empty stacks.
     */
    public static void applyRarityToStack(ItemStack stack, ItemRarity rarity) {
        if (stack.isEmpty()) return;
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt(RARITY_KEY, rarity.id);

        if (rarity != ItemRarity.COMMON) {
            String stats = generateStatString(stack, rarity);
            nbt.putString(STATS_KEY, stats);
        }
    }

    // ── ELIGIBILITY ──────────────────────────────────────────────────────────

    /**
     * Only rarity-stamp items that are "valuable" — durable items (weapons,
     * armor, tools), items that stack to 1 (unique/special), and all custom
     * IseKraft items. Filters out bread, dirt, arrows, etc.
     */
    public static boolean isRarityEligible(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getMaxDamage() > 0)  return true;  // weapon / armor / tool
        if (stack.getMaxCount() == 1)  return true;  // unique item (totem, etc.)
        // All registered isekraft items are eligible (materials, crystals, etc.)
        return Registries.ITEM.getId(stack.getItem())
                              .getNamespace()
                              .equals("isekraft");
    }

    // ── TOOLTIP ──────────────────────────────────────────────────────────────

    /**
     * Returns the stat bonus lines stored in NBT, ready for tooltip insertion.
     * Returns empty list for COMMON or untagged items.
     */
    public static List<Text> getStatTooltip(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(STATS_KEY)) return List.of();
        String raw = nbt.getString(STATS_KEY);
        if (raw.isBlank()) return List.of();

        List<Text> lines = new ArrayList<>();
        for (String line : raw.split("\n")) {
            if (!line.isBlank())
                lines.add(Text.literal(line).formatted(Formatting.GRAY));
        }
        return lines;
    }

    // ── STAT GENERATION ──────────────────────────────────────────────────────

    /**
     * Generates a deterministic-ish set of stat bonus strings.
     *
     * Why deterministic? Items with the same type/rarity should feel consistent
     * rather than random each time you hover — it prevents the jarring experience
     * of tooltips changing on re-render. We use the item's registry path as a
     * seed offset so different items in the same category still vary.
     */
    private static String generateStatString(ItemStack stack, ItemRarity rarity) {
        String itemPath = Registries.ITEM.getId(stack.getItem()).getPath();
        int seed = itemPath.hashCode();

        List<String> pool = buildStatPool(itemPath);
        int count        = rarity.statBonusCount();
        float mult       = rarity.statMultiplier();

        List<String> chosen = new ArrayList<>();
        for (int i = 0; i < count && chosen.size() < pool.size(); i++) {
            // Rotate through pool deterministically by seed+i
            String candidate = pool.get(Math.abs((seed + i * 31) % pool.size()));
            if (!chosen.contains(candidate)) {
                chosen.add(candidate);
            } else {
                // Try next slot if duplicate
                for (int j = 1; j < pool.size(); j++) {
                    String alt = pool.get(Math.abs((seed + i * 31 + j) % pool.size()));
                    if (!chosen.contains(alt)) { chosen.add(alt); break; }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String stat : chosen) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(scaleStatValue(stat, mult));
        }
        return sb.toString();
    }

    /** Selects the stat pool appropriate to the item type. */
    private static List<String> buildStatPool(String itemPath) {
        boolean isWeapon = itemPath.contains("sword") || itemPath.contains("hammer")
                        || itemPath.contains("staff") || itemPath.contains("bow")
                        || itemPath.contains("shuriken") || itemPath.contains("boomerang")
                        || itemPath.contains("axe");
        boolean isArmor  = itemPath.contains("helmet") || itemPath.contains("chestplate")
                        || itemPath.contains("leggings") || itemPath.contains("boots")
                        || itemPath.contains("crown");

        if (isWeapon) return List.of(
            "+2 Attack Damage",
            "+5% Critical Strike",
            "+10% XP Gain",
            "+3% Attack Speed",
            "+8% Damage vs Undead"
        );
        if (isArmor) return List.of(
            "+4 Max HP",
            "+2 Armor",
            "+5% Knockback Resistance",
            "+3% Damage Reduction",
            "+8 Max HP"
        );
        // Materials, potions, misc
        return List.of(
            "+5% XP Gain",
            "+3% Luck",
            "+2% Drop Rate",
            "+1 Fortune",
            "+10% Potion Effect Duration"
        );
    }

    /**
     * Scales a numeric value inside a stat string by the rarity multiplier.
     * Handles both flat values ("+2 Attack Damage") and percentages ("+5% Crit").
     */
    private static String scaleStatValue(String stat, float multiplier) {
        if (!stat.startsWith("+") || stat.length() < 3) return stat;

        int firstSpace = stat.indexOf(' ', 1);
        if (firstSpace < 2) return stat;

        String numPart = stat.substring(1, firstSpace);
        boolean isPct  = numPart.endsWith("%");
        if (isPct) numPart = numPart.substring(0, numPart.length() - 1);

        try {
            float base    = Float.parseFloat(numPart);
            int   scaled  = Math.max(1, Math.round(base * multiplier));
            return "+" + scaled + (isPct ? "%" : "") + stat.substring(firstSpace);
        } catch (NumberFormatException e) {
            return stat; // fallback: return unchanged
        }
    }
}
