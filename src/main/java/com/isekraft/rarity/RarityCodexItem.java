package com.isekraft.rarity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * The IseKraft Rarity Codex.
 *
 * A craftable reference item (paper + soul_crystal + rune_fragment).
 * Right-clicking places a WrittenBook version of the codex into the player's
 * inventory. The book contains six pages covering:
 *   Page 1 — Introduction
 *   Page 2 — Rarity tiers + drop rates
 *   Page 3 — Weapon stat bonuses
 *   Page 4 — Armor stat bonuses
 *   Page 5 — Best drop sources
 *   Page 6 — Tips
 *
 * Also accessible via: /isekraft codex
 */
public class RarityCodexItem extends Item {

    public RarityCodexItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            sp.giveItemStack(buildCodexBook());
            // Action bar message — less intrusive than chat
            sp.sendMessage(
                Text.literal("✦ Rarity Codex added to inventory — check your books!")
                    .formatted(Formatting.GOLD),
                true
            );
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    // ── Book construction ─────────────────────────────────────────────────────

    /**
     * Builds a signed WrittenBook ItemStack containing all codex pages.
     * Called both from use() and from /isekraft codex.
     */
    public static ItemStack buildCodexBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = book.getOrCreateNbt();
        nbt.putString("title", "IseKraft Rarity Codex");
        nbt.putString("author", "The Isekai Scribe");

        NbtList pages = new NbtList();

        // ── Page 1: Introduction ─────────────────────────────────────────────
        pages.add(page(
            "§6§lIseKraft\nRarity Codex\n\n"
          + "§rAll items in this world carry a hidden rarity — a measure of their "
          + "latent power, forged at the moment of their creation.\n\n"
          + "§rFrom §7Common§r tools to §6Legendary§r relics, each drop is a "
          + "chance at greatness. Read on."
        ));

        // ── Page 2: Rarity Tiers ─────────────────────────────────────────────
        pages.add(page(
            "§l§nRarity Tiers\n"
          + "§r(drop chance)\n\n"
          + "§7◆ Common\n  §r60%  — no bonuses\n\n"
          + "§a◆ Uncommon\n  §r25%  — 1 bonus\n\n"
          + "§9◆ Rare\n  §r10%  — 2 bonuses\n\n"
          + "§5◆ Epic\n   §r4%  — 3 bonuses\n\n"
          + "§6◆ Legendary\n   §r1%  — 4 bonuses"
        ));

        // ── Page 3: Weapon Stats ─────────────────────────────────────────────
        pages.add(page(
            "§l§nWeapon Stats\n\n"
          + "§rWeapons (swords,\nbows, hammers,\nstaves…) can roll:\n\n"
          + "§7+ Attack Damage\n"
          + "§7+ Critical Strike\n"
          + "§7+ XP Gain\n"
          + "§7+ Attack Speed\n"
          + "§7+ vs Undead\n\n"
          + "§rHigher rarity =\nhigher values.\n"
          + "§6Legendary§r scales\nup to ×5 base."
        ));

        // ── Page 4: Armor Stats ──────────────────────────────────────────────
        pages.add(page(
            "§l§nArmor Stats\n\n"
          + "§rArmor can roll:\n\n"
          + "§7+ Max HP\n"
          + "§7+ Armor\n"
          + "§7+ Knockback\n  Resistance\n"
          + "§7+ Damage\n  Reduction\n\n"
          + "§rMaterials roll:\n\n"
          + "§7+ XP Gain\n"
          + "§7+ Luck\n"
          + "§7+ Drop Rate\n"
          + "§7+ Fortune"
        ));

        // ── Page 5: Best Sources ─────────────────────────────────────────────
        pages.add(page(
            "§l§nBest Drop Sources\n\n"
          + "§c☠ Shadow Demon\n"
          + "§rHighest Legendary\nchance. Face it at\nlevel 100.\n\n"
          + "§dGoblin King\n"
          + "§rSolid Epic drop\nrate. Boss in the\noveworld.\n\n"
          + "§eWitch Coven\n"
          + "§rHexara drops\nunique Epic loot.\n\n"
          + "§bBattle Tower\n"
          + "§rConsistent Rare+\nchest loot across\n8 floors."
        ));

        // ── Page 6: Tips ─────────────────────────────────────────────────────
        pages.add(page(
            "§l§nTips\n\n"
          + "§r• §6Gold name\n= Legendary.\n  §5Purple\n= Epic.\n\n"
          + "§r• Stat bonuses\nshow in the\ntooltip below\nthe rarity tier.\n\n"
          + "§r• Stats are\ndisplay-only\nin v1. Full\napplication\ncomes with the\nEquipment Slot\nsystem.\n\n"
          + "§r• §l/isekraft\n  codex\n§rfor a fresh\ncopy anytime."
        ));

        nbt.put("pages", pages);
        return book;
    }

    /** Wraps a raw string page in the NbtString format Minecraft expects. */
    private static NbtString page(String content) {
        // Minecraft written book pages use JSON text — wrap in a plain string
        // literal JSON so the client renders it correctly.
        return NbtString.of("\"" + content.replace("\\", "\\\\")
                                          .replace("\"", "\\\"")
                                          .replace("\n", "\\n") + "\"");
    }
}
