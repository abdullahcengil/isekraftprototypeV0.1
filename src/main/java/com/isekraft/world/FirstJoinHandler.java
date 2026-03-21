package com.isekraft.world;

import com.isekraft.IseKraftMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import com.isekraft.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Sends a welcome guide message the first time a player joins.
 * Tracks via "FirstJoinDone" key inside PlayerRpgManager's NBT data
 * (saved/loaded by PlayerEntityMixin — kalıcı olarak disk'e yazılır).
 */
public class FirstJoinHandler {

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            // Flag'i PlayerRpgManager NBT'sine sakla — bu mixin ile save/load edilir
            NbtCompound rpgData = com.isekraft.rpg.PlayerRpgManager.getData(player);
            if (!rpgData.getBoolean("FirstJoinDone")) {
                rpgData.putBoolean("FirstJoinDone", true);
                com.isekraft.rpg.PlayerRpgManager.setData(player, rpgData);
                server.execute(() -> sendWelcome(player));
            }
        });
    }

    private static void sendWelcome(ServerPlayerEntity player) {
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("═══════════════════════════════════").formatted(Formatting.DARK_PURPLE), false);
        player.sendMessage(Text.literal("  ✦ Welcome to IseKraft RPG! ✦").formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("═══════════════════════════════════").formatted(Formatting.DARK_PURPLE), false);
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("You have been summoned to another world.").formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("Your level grows as you kill mobs.").formatted(Formatting.WHITE), false);
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("⚔  WEAPONS").formatted(Formatting.AQUA, Formatting.BOLD), false);
        player.sendMessage(Text.literal("  Kill zombies → get Rune Fragments → craft Rune Sword").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("🐺  MOBS").formatted(Formatting.GREEN, Formatting.BOLD), false);
        player.sendMessage(Text.literal("  Dark Knights spawn at night. Forest Wolves in forests.").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("  Spirit Beasts are tameable — use Spirit Essence on them.").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("  Goblin King is a rare boss with a health bar.").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("🏰  STRUCTURES").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
        player.sendMessage(Text.literal("  Isekai Castles spawn in the world. Explore to find them!").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("  They contain chests with rare loot and boss spawners.").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("📦  GIFTS").formatted(Formatting.YELLOW, Formatting.BOLD), false);
        player.sendMessage(Text.literal("  Reach levels 5, 10, 20, 30, 40, 50, 75, 100 for free items!").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("  /give @s isekraft:god_sword  ← Admin one-shot sword").formatted(Formatting.DARK_GRAY), false);
        player.sendMessage(Text.literal("═══════════════════════════════════").formatted(Formatting.DARK_PURPLE), false);
        player.sendMessage(Text.literal("  Good luck, Isekai Hero! ✦").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal("═══════════════════════════════════").formatted(Formatting.DARK_PURPLE), false);

        player.getInventory().insertStack(new ItemStack(ModItems.RECIPE_GUIDE));
        IseKraftMod.LOGGER.info("[IseKraft] Sent welcome guide to {}", player.getName().getString());
    }
}
