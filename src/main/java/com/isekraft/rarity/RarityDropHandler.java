package com.isekraft.rarity;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;

/**
 * Server-side handler that injects the ApplyRarityLootFunction into every
 * loot table pool at data-pack load time.
 *
 * Because we use LootTableEvents.MODIFY (a Fabric API event fired after loot
 * tables are loaded but before they are frozen), this works for:
 *   • All IseKraft entity loot tables (dark_knight, shadow_demon, etc.)
 *   • All IseKraft chest loot tables (battle_tower, isekai_castle)
 *   • Vanilla entity loot tables (zombie, skeleton, piglin, etc.)
 *   • Vanilla chest loot tables (stronghold, nether fortress, etc.)
 *
 * The function itself guards against:
 *   • Empty stacks
 *   • Already-tagged stacks
 *   • Non-eligible items (food, arrows, dirt, etc.)
 *
 * Call order in IseKraftMod.onInitialize():
 *   1. ApplyRarityLootFunction.register() — registers the LootFunctionType
 *   2. (Fabric fires LootTableEvents.MODIFY later, during data-pack load)
 */
public class RarityDropHandler {

    public static void register() {
        // Step 1: register the custom loot function type so Minecraft recognises
        // "isekraft:apply_rarity" in JSON and via the registry.
        ApplyRarityLootFunction.register();

        // Step 2: for every loot table pool across all data packs, append our
        // rarity function. Pool-level functions fire for each generated item.
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) ->
            tableBuilder.modifyPools(pool ->
                pool.apply(ApplyRarityLootFunction.builder())
            )
        );
    }
}
