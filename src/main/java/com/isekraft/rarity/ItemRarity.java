package com.isekraft.rarity;

import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

/**
 * IseKraft Item Rarity System — v1.0
 *
 * Five tiers, each with a distinct color and weighted drop probability.
 * Rarity is rolled at loot generation time and stored as NBT on the ItemStack.
 * It affects tooltip color/display and stat bonuses (display-only in v1;
 * full stat application arrives with the Equipment Slot system).
 *
 * Drop weights total 100 for easy mental math:
 *   COMMON     60%
 *   UNCOMMON   25%
 *   RARE       10%
 *   EPIC        4%
 *   LEGENDARY   1%
 */
public enum ItemRarity {

    COMMON   (0, "Common",    Formatting.WHITE,        60),
    UNCOMMON (1, "Uncommon",  Formatting.GREEN,        25),
    RARE     (2, "Rare",      Formatting.AQUA,         10),
    EPIC     (3, "Epic",      Formatting.LIGHT_PURPLE,  4),
    LEGENDARY(4, "Legendary", Formatting.GOLD,           1);

    public final int       id;
    public final String    displayName;
    public final Formatting color;
    public final int       weight;         // out of 100

    ItemRarity(int id, String displayName, Formatting color, int weight) {
        this.id          = id;
        this.displayName = displayName;
        this.color       = color;
        this.weight      = weight;
    }

    /** Reverse-lookup by stored int id. Falls back to COMMON on unknown values. */
    public static ItemRarity fromId(int id) {
        for (ItemRarity r : values()) if (r.id == id) return r;
        return COMMON;
    }

    /**
     * Weighted random roll.
     * Thresholds:  0–0  = LEGENDARY (1 in 100)
     *              1–4  = EPIC
     *              5–14 = RARE
     *             15–39 = UNCOMMON
     *             40–99 = COMMON
     */
    public static ItemRarity roll(Random random) {
        int r = random.nextInt(100);
        if (r <  1)  return LEGENDARY;
        if (r <  5)  return EPIC;
        if (r < 15)  return RARE;
        if (r < 40)  return UNCOMMON;
        return COMMON;
    }

    /** Number of stat bonus lines granted at this tier. */
    public int statBonusCount() {
        return switch (this) {
            case UNCOMMON  -> 1;
            case RARE      -> 2;
            case EPIC      -> 3;
            case LEGENDARY -> 4;
            default        -> 0;
        };
    }

    /** Multiplier applied to base stat values for this tier. */
    public float statMultiplier() {
        return switch (this) {
            case UNCOMMON  -> 1.0f;
            case RARE      -> 1.8f;
            case EPIC      -> 3.0f;
            case LEGENDARY -> 5.0f;
            default        -> 0f;
        };
    }
}
