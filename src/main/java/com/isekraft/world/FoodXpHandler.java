package com.isekraft.world;

import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

/**
 * Food XP System.
 *
 * Called from PlayerEntityMixin when the player finishes eating.
 * Only cooked/high-value foods grant XP.
 *
 * Variety bonus: tracks last 20 foods eaten per player (FoodHistory NBT key).
 * Each unique food type = +1 XP/meal bonus, up to +10 max.
 * Same food back-to-back = half XP (anti-exploit).
 *
 * Example:
 *   Cooked beef alone:      8 XP/meal, no variety bonus
 *   10+ different foods:    base + 10 variety bonus per meal
 */
public class FoodXpHandler {

    private static final int HISTORY_SIZE = 20;

    private static final Map<String, Integer> FOOD_XP = new HashMap<>();
    static {
        FOOD_XP.put("minecraft:cooked_beef",     8);
        FOOD_XP.put("minecraft:cooked_porkchop", 7);
        FOOD_XP.put("minecraft:cooked_mutton",   6);
        FOOD_XP.put("minecraft:cooked_chicken",  5);
        FOOD_XP.put("minecraft:cooked_rabbit",   5);
        FOOD_XP.put("minecraft:cooked_cod",      4);
        FOOD_XP.put("minecraft:cooked_salmon",   5);
        FOOD_XP.put("minecraft:bread",           3);
        FOOD_XP.put("minecraft:baked_potato",    3);
        FOOD_XP.put("minecraft:pumpkin_pie",     4);
        FOOD_XP.put("minecraft:cookie",          1);
        FOOD_XP.put("minecraft:golden_apple",   12);
        FOOD_XP.put("minecraft:golden_carrot",   8);
        FOOD_XP.put("minecraft:mushroom_stew",   5);
        FOOD_XP.put("minecraft:rabbit_stew",     7);
        FOOD_XP.put("minecraft:beetroot_soup",   4);
        FOOD_XP.put("minecraft:suspicious_stew", 5);
        FOOD_XP.put("isekraft:health_potion",    3);
    }

    /**
     * Called from PlayerEntityMixin when a food item finishes being eaten.
     * @param player the server-side player
     * @param item   the item that was just consumed
     */
    public static void onFoodEaten(ServerPlayerEntity player, Item item) {
        String itemId = Registries.ITEM.getId(item).toString();
        Integer baseXp = FOOD_XP.get(itemId);
        if (baseXp == null) return;

        NbtCompound d = PlayerRpgManager.getData(player);
        String raw = d.getString("FoodHistory");
        List<String> history = new ArrayList<>(
            raw.isEmpty() ? Collections.emptyList() : Arrays.asList(raw.split(","))
        );

        // Variety: unique foods in tracked history
        Set<String> unique = new HashSet<>(history);
        int varietyBonus = Math.min(unique.size(), 10);

        // Diminishing returns for same food back-to-back
        boolean repeated = !history.isEmpty() && history.get(history.size() - 1).equals(itemId);
        int xp = (repeated ? Math.max(1, baseXp / 2) : baseXp) + varietyBonus;

        // Update history
        history.add(itemId);
        if (history.size() > HISTORY_SIZE) history.remove(0);
        d.putString("FoodHistory", String.join(",", history));
        PlayerRpgManager.setData(player, d);

        // Grant XP
        PlayerRpgManager.addXp(player, xp);

        // Action bar notification on variety increase
        if (!repeated) {
            long newUnique = history.stream().distinct().count();
            if (newUnique > unique.size() && newUnique <= 10) {
                player.sendMessage(
                    Text.literal("✦ Food variety: " + newUnique + "/10  (+1 bonus XP/meal)")
                        .formatted(Formatting.GREEN),
                    true); // action bar
            }
        }
    }
}
