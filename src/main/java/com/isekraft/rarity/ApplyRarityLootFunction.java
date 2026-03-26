package com.isekraft.rarity;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Custom LootFunction that stamps an ItemRarity onto every eligible ItemStack
 * at the moment it is generated from a loot table.
 *
 * Registered as: "isekraft:apply_rarity"
 *
 * Injected into all pools of all loot tables via RarityDropHandler using
 * Fabric's LootTableEvents.MODIFY. Because it is a pool-level function it
 * fires for every item that pool produces, including vanilla loot tables.
 *
 * Guards:
 *   • Skips empty stacks.
 *   • Skips already-tagged stacks (prevents double-rolling on re-generation).
 *   • Skips items that fail the eligibility check (food, dirt, arrows, etc.).
 */
public class ApplyRarityLootFunction extends ConditionalLootFunction {

    /** Registered at startup by RarityDropHandler.register(). */
    public static LootFunctionType TYPE;

    protected ApplyRarityLootFunction(LootCondition[] conditions) {
        super(conditions);
    }

    @Override
    public LootFunctionType getType() {
        return TYPE;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        if (stack.isEmpty())                       return stack;
        if (RarityManager.hasRarity(stack))        return stack;
        if (!RarityManager.isRarityEligible(stack)) return stack;

        ItemRarity rarity = ItemRarity.roll(context.getRandom());
        RarityManager.applyRarityToStack(stack, rarity);
        return stack;
    }

    // ── Builder (used by RarityDropHandler) ──────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ConditionalLootFunction.Builder<Builder> {
        @Override
        protected Builder getThisBuilder() { return this; }

        @Override
        public ApplyRarityLootFunction build() {
            return new ApplyRarityLootFunction(getConditions());
        }
    }

    // ── Serializer (required for type registry) ───────────────────────────────

    public static class Serializer extends ConditionalLootFunction.Serializer<ApplyRarityLootFunction> {
        @Override
        public ApplyRarityLootFunction fromJson(JsonObject json,
                                                 JsonDeserializationContext ctx,
                                                 LootCondition[] conditions) {
            return new ApplyRarityLootFunction(conditions);
        }
    }

    // ── Registration ─────────────────────────────────────────────────────────

    /**
     * Must be called before any loot tables are loaded.
     * Called from RarityDropHandler.register() which is called in
     * IseKraftMod.onInitialize().
     */
    public static void register() {
        TYPE = Registry.register(
            Registries.LOOT_FUNCTION_TYPE,
            new Identifier("isekraft", "apply_rarity"),
            new LootFunctionType(new Serializer())
        );
    }
}
