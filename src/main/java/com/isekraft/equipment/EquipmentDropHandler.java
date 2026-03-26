package com.isekraft.equipment;

import com.isekraft.entity.ModEntities;
import com.isekraft.entity.WitchCovenEntity;
import com.isekraft.item.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/**
 * Rolls for equipment drops on every mob kill.
 *
 * Tier system:
 *   Tier 1 (mobLevel 1–5):   iron_gauntlet, bone_pendant, copper_ring    — 3% drop chance
 *   Tier 2 (mobLevel 6–19):  steel_gauntlet, soul_pendant, rune_ring     — 4% drop chance
 *   Tier 3 (mobLevel 20+):   shadow_gauntlet, demon_pendant, overlord_ring — 7% drop chance
 *
 * On a successful roll, a random slot (glove/necklace/ring) is selected
 * and the tier-appropriate item for that slot drops as a ground item.
 * The player gets an action-bar notification.
 *
 * Looting enchantment: each level of Looting adds +1% drop chance.
 */
public class EquipmentDropHandler {

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killedEntity) -> {
            if (!(killer instanceof ServerPlayerEntity player)) return;
            if (!(killedEntity instanceof MobEntity mob))       return;
            ServerWorld sw = (ServerWorld) world;   // or simply use "world" directly if you prefer

            int mobLevel = estimateMobLevel(mob);
            int tier     = tierForLevel(mobLevel);
            float chance = baseChance(tier) + lootingBonus(player);

            if (player.getRandom().nextFloat() >= chance) return;

            // Pick random slot
            EquipSlot slot = EquipSlot.values()[player.getRandom().nextInt(3)];
            ItemStack drop = itemForTierAndSlot(tier, slot);
            if (drop.isEmpty()) return;

            // Spawn as ground item at mob's death position
            Vec3d pos = killedEntity.getPos();
            sw.spawnEntity(new net.minecraft.entity.ItemEntity(
                sw, pos.x, pos.y + 0.5, pos.z, drop));

            // Action-bar notification (subtle, not chat spam)
            player.sendMessage(
                Text.literal("✦ " + drop.getName().getString() + " dropped!")
                    .formatted(Formatting.GOLD),
                true  // action bar = true
            );
        });
    }

    // ── Tier selection ────────────────────────────────────────────────────────

    private static int tierForLevel(int mobLevel) {
        if (mobLevel >= 20) return 3;
        if (mobLevel >=  6) return 2;
        return 1;
    }

    private static float baseChance(int tier) {
        return switch (tier) {
            case 3  -> 0.07f;
            case 2  -> 0.04f;
            default -> 0.03f;
        };
    }

    private static float lootingBonus(ServerPlayerEntity player) {
        // +0.01 per Looting level on currently held weapon
        int looting = net.minecraft.enchantment.EnchantmentHelper.getLooting(player);
        return looting * 0.01f;
    }

    // ── Item lookup ───────────────────────────────────────────────────────────

    private static ItemStack itemForTierAndSlot(int tier, EquipSlot slot) {
        return switch (slot) {
            case GLOVE    -> switch (tier) {
                case 1  -> new ItemStack(ModItems.IRON_GAUNTLET);
                case 2  -> new ItemStack(ModItems.STEEL_GAUNTLET);
                default -> new ItemStack(ModItems.SHADOW_GAUNTLET);
            };
            case NECKLACE -> switch (tier) {
                case 1  -> new ItemStack(ModItems.BONE_PENDANT);
                case 2  -> new ItemStack(ModItems.SOUL_PENDANT);
                default -> new ItemStack(ModItems.DEMON_PENDANT);
            };
            case RING     -> switch (tier) {
                case 1  -> new ItemStack(ModItems.COPPER_RING);
                case 2  -> new ItemStack(ModItems.RUNE_RING);
                default -> new ItemStack(ModItems.OVERLORD_RING);
            };
        };
    }

    // ── Mob level estimation (mirrors KillEventHandler.estimateLevel) ─────────

    private static int estimateMobLevel(MobEntity mob) {
        if (mob.getType() == ModEntities.SHADOW_DEMON)   return 50;
        if (mob.getType() == ModEntities.GOBLIN_KING)    return 20;
        if (mob.getType() == ModEntities.WITCH_COVEN) {
            if (mob instanceof WitchCovenEntity wc) {
                return switch (wc.getRole()) {
                    case HEXARA -> 22;
                    default     -> 18;
                };
            }
            return 18;
        }
        if (mob.getType() == ModEntities.DARK_KNIGHT)   return 6;
        if (mob.getType() == ModEntities.OVERLORD_GUARD) return 8;
        if (mob.getType() == ModEntities.SPIRIT_BEAST)  return 4;
        if (mob.getType() == ModEntities.FOREST_WOLF)   return 3;
        // Vanilla — estimate by max HP
        float hp = mob.getMaxHealth();
        if (hp >= 100) return 25;
        if (hp >=  40) return 10;
        if (hp >=  20) return  4;
        if (hp >=  10) return  2;
        return 1;
    }
}
