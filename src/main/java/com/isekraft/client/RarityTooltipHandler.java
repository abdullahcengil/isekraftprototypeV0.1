package com.isekraft.client;

import com.isekraft.rarity.ItemRarity;
import com.isekraft.rarity.RarityManager;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Client-side tooltip modifier for the IseKraft Rarity System.
 *
 * Registered once in IseKraftClient.onInitializeClient() via
 *   ItemTooltipCallback.EVENT.register(...)
 *
 * What it does to a rarity-tagged ItemStack's tooltip:
 *   [0] Item name  → replaced with BOLD colored name (rarity.color)
 *   [1] inserted   → "◆ Legendary" (or whatever tier) in rarity color
 *   [2..n] inserted → stat bonus lines (gray)
 *   [n+1] inserted → blank separator line (only when stats are present)
 *
 * COMMON items are skipped — they get no color change and no extra lines,
 * keeping the inventory clean for the vast majority of drops.
 */
public class RarityTooltipHandler {

    public static void register() {
        ItemTooltipCallback.EVENT.register(RarityTooltipHandler::onTooltip);
    }

    private static void onTooltip(ItemStack stack, TooltipContext context, List<Text> lines) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(RarityManager.RARITY_KEY)) return;

        ItemRarity rarity = ItemRarity.fromId(nbt.getInt(RarityManager.RARITY_KEY));
        if (rarity == ItemRarity.COMMON) return; // keep commons pristine

        // ── 1. Color the item name (index 0) ──────────────────────────────
        if (!lines.isEmpty()) {
            String rawName = lines.get(0).getString();
            lines.set(0, Text.literal(rawName).formatted(rarity.color, Formatting.BOLD));
        }

        // ── 2. Insert rarity tier badge after name ─────────────────────────
        int cursor = 1;
        lines.add(cursor, Text.literal("◆ " + rarity.displayName).formatted(rarity.color));
        cursor++;

        // ── 3. Insert stat bonus lines ─────────────────────────────────────
        List<Text> stats = RarityManager.getStatTooltip(stack);
        for (Text statLine : stats) {
            lines.add(cursor, statLine);
            cursor++;
        }

        // ── 4. Blank separator (visual breathing room below stats) ─────────
        if (!stats.isEmpty()) {
            lines.add(cursor, Text.literal(""));
        }
    }
}
