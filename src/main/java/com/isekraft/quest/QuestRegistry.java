package com.isekraft.quest;

import com.isekraft.item.ModItems;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All quests available in IseKraft.
 * Keyed by quest id for O(1) lookup.
 *
 * Tiers:
 *   Early  (lv 1 ) — 4 quests  — vanilla mobs, basic items
 *   Mid    (lv 20) — 4 quests  — custom mobs, materials
 *   Late   (lv 50) — 4 quests  — bosses, endgame items
 */
public class QuestRegistry {

    private static final Map<String, Quest> ALL = new LinkedHashMap<>();

    static {
        // ── EARLY TIER ───────────────────────────────────────────────────────
        add(new Quest(
            "hunt_wolves", "Wolf Culling",
            "The forest roads are unsafe. Kill 5 wolves.",
            Quest.Type.KILL, "minecraft:wolf", 5,
            120, ModItems.RUNE_FRAGMENT, 2, 1
        ));
        add(new Quest(
            "hunt_zombies", "Undead Patrol",
            "Purge 10 zombies from the night.",
            Quest.Type.KILL, "minecraft:zombie", 10,
            150, ModItems.IRON_RUNE_SHARD, 5, 1
        ));
        add(new Quest(
            "fetch_iron", "Ironmonger's Request",
            "Bring 8 iron ingots to the Guild.",
            Quest.Type.FETCH, "minecraft:iron_ingot", 8,
            100, ModItems.ANCIENT_COIN, 3, 1
        ));
        add(new Quest(
            "fetch_bones", "Bone Collector",
            "Bring 10 bones to the Guild.",
            Quest.Type.FETCH, "minecraft:bone", 10,
            80, ModItems.SOUL_CRYSTAL, 2, 1
        ));

        // ── MID TIER ─────────────────────────────────────────────────────────
        add(new Quest(
            "hunt_dark_knights", "Knight Slayer",
            "Dark Knights terrorise the roads. Slay 3.",
            Quest.Type.KILL, "isekraft:dark_knight", 3,
            400, ModItems.STEEL_INGOT, 4, 20
        ));
        add(new Quest(
            "hunt_forest_wolves", "Spirit Hunt",
            "Hunt 5 Forest Wolves from the deep wood.",
            Quest.Type.KILL, "isekraft:forest_wolf", 5,
            350, ModItems.SPIRIT_ESSENCE, 2, 20
        ));
        add(new Quest(
            "fetch_soul_crystals", "Crystal Harvest",
            "The alchemist needs 5 Soul Crystals.",
            Quest.Type.FETCH, "isekraft:soul_crystal", 5,
            300, ModItems.MANA_CRYSTAL, 3, 20
        ));
        add(new Quest(
            "reach_champion", "Path of the Champion",
            "Reach level 50 — prove your worth.",
            Quest.Type.EXPLORE, "level", 50,
            500, ModItems.DEMON_CORE, 2, 20
        ));

        // ── LATE TIER ────────────────────────────────────────────────────────
        add(new Quest(
            "slay_goblin_king", "Goblin King's End",
            "The Goblin King must fall. Slay him once.",
            Quest.Type.KILL, "isekraft:goblin_king", 1,
            1000, ModItems.DEMON_CORE, 3, 50
        ));
        add(new Quest(
            "slay_witch_coven", "Witch Hunt",
            "The Witch Coven plagues the swamps. Destroy all 3.",
            Quest.Type.KILL, "isekraft:witch_coven", 3,
            1200, ModItems.RUNE_FRAGMENT, 8, 50
        ));
        add(new Quest(
            "fetch_demon_cores", "Demon Forging",
            "Bring 5 Demon Cores to forge the Overlord's weapon.",
            Quest.Type.FETCH, "isekraft:demon_core", 5,
            800, ModItems.OVERLORD_SEAL, 1, 50
        ));
        add(new Quest(
            "slay_shadow_demon", "Into the Void",
            "Face the Shadow Demon. End it. Become the Overlord.",
            Quest.Type.KILL, "isekraft:shadow_demon", 1,
            2000, ModItems.DEMON_LORD_CROWN, 1, 80
        ));
    }

    private static void add(Quest q) { ALL.put(q.id, q); }

    public static Quest get(String id)           { return ALL.get(id); }
    public static Map<String, Quest> getAll()    { return ALL; }
}
