package com.isekraft.equipment;

import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * EquipmentManager — handles all server-side logic for the 3 RPG accessory slots.
 *
 * Data layout (inside the player's existing IseKraftRPG NbtCompound):
 *   "EquipGlove"    → NbtCompound — serialized ItemStack
 *   "EquipNecklace" → NbtCompound — serialized ItemStack
 *   "EquipRing"     → NbtCompound — serialized ItemStack
 *
 * Integration points:
 *   • PlayerRpgManager.applyStats() calls applyEquipmentStats() to add bonuses
 *     on top of the base level/skill values already set there.
 *   • PlayerRpgManager.applyStats() also calls autoEquipBest() so any
 *     equippable item picked up will auto-slot within 10 seconds.
 *
 * Attribute modifiers use stable, named UUIDs so they never stack.
 */
public class EquipmentManager {

    // Stable UUIDs for attribute modifiers — must never change between sessions
    private static final UUID UUID_DMG   = UUID.fromString("4a7b3c2d-1e9f-4a0b-8c7d-6e5f4a3b2c1d");
    private static final UUID UUID_SPD   = UUID.fromString("5b8c4d3e-2f0a-5b1c-9d8e-7f6a5b4c3d2e");
    private static final String MOD_NAME = "isekraft_equipment";

    // ── QUERY ─────────────────────────────────────────────────────────────────

    /**
     * Returns the currently equipped stack for a slot.
     * Returns ItemStack.EMPTY if nothing is equipped.
     */
    public static ItemStack getEquipped(ServerPlayerEntity player, EquipSlot slot) {
        NbtCompound data = PlayerRpgManager.getData(player);
        if (!data.contains(slot.nbtKey)) return ItemStack.EMPTY;
        NbtCompound itemNbt = data.getCompound(slot.nbtKey);
        if (itemNbt.isEmpty()) return ItemStack.EMPTY;
        return ItemStack.fromNbt(itemNbt);
    }

    public static boolean hasEquipped(ServerPlayerEntity player, EquipSlot slot) {
        return !getEquipped(player, slot).isEmpty();
    }

    // ── EQUIP / UNEQUIP ───────────────────────────────────────────────────────

    /**
     * Equips an item into the specified slot.
     * If the slot already has an item it is returned to the player's inventory.
     * Returns the previously equipped stack (may be EMPTY).
     */
    public static ItemStack equip(ServerPlayerEntity player, ItemStack newStack, EquipSlot slot) {
        ItemStack old = getEquipped(player, slot);

        NbtCompound data = PlayerRpgManager.getData(player);
        NbtCompound itemNbt = new NbtCompound();
        newStack.writeNbt(itemNbt);
        data.put(slot.nbtKey, itemNbt);
        PlayerRpgManager.setData(player, data);

        // Return old item to inventory if there was one
        if (!old.isEmpty()) {
            player.getInventory().insertStack(old.copy());
        }
        return old;
    }

    /** Unequips the slot and returns the item to the player's inventory. */
    public static void unequip(ServerPlayerEntity player, EquipSlot slot) {
        ItemStack equipped = getEquipped(player, slot);
        if (equipped.isEmpty()) return;

        NbtCompound data = PlayerRpgManager.getData(player);
        data.remove(slot.nbtKey);
        PlayerRpgManager.setData(player, data);
        player.getInventory().insertStack(equipped.copy());
        player.sendMessage(Text.literal("✦ " + equipped.getName().getString()
            + " unequipped.").formatted(Formatting.GRAY), true);
    }

    // ── AUTO-EQUIP ────────────────────────────────────────────────────────────

    /**
     * Scans the player's inventory for EquipmentItems.
     * For each slot: if empty, equips the best found item.
     * If an item with higher tierPriority is found than what's equipped, upgrades.
     *
     * Called from PlayerRpgManager.applyStats() every 200 ticks (10 seconds).
     */
    public static void autoEquipBest(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        int playerLevel = PlayerRpgManager.getLevel(player);

        for (EquipSlot slot : EquipSlot.values()) {
            ItemStack currentEquipped = getEquipped(player, slot);
            int currentTier = tierOf(currentEquipped);

            ItemStack bestCandidate = ItemStack.EMPTY;
            int bestTier = currentTier; // only upgrade if strictly better
            int bestInvSlot = -1;

            // Scan full inventory
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (stack.isEmpty()) continue;
                if (!(stack.getItem() instanceof EquipmentItem eq)) continue;
                if (eq.slot != slot) continue;
                if (eq.levelRequired > playerLevel) continue; // level gate

                if (eq.tierPriority > bestTier) {
                    bestTier     = eq.tierPriority;
                    bestCandidate = stack;
                    bestInvSlot  = i;
                }
            }

            if (!bestCandidate.isEmpty() && bestInvSlot >= 0) {
                // Remove from inventory before equipping
                ItemStack toEquip = bestCandidate.copy();
                inv.removeStack(bestInvSlot, 1);
                equip(player, toEquip, slot);
                player.sendMessage(
                    Text.literal("✦ " + toEquip.getName().getString() + " auto-equipped!")
                        .formatted(Formatting.GOLD), true);
            }
        }
    }

    private static int tierOf(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() instanceof EquipmentItem eq) return eq.tierPriority;
        return 0;
    }

    // ── STAT APPLICATION ──────────────────────────────────────────────────────

    /**
     * Applies stat bonuses from all 3 equipped slots.
     * Called from PlayerRpgManager.applyStats() AFTER base stats are set,
     * so equipment bonuses stack cleanly on top.
     *
     * HP bonus: added to the baseHp already set via setBaseValue()
     * Damage bonus: EntityAttributeModifier with ADDITION on GENERIC_ATTACK_DAMAGE
     * Speed bonus: EntityAttributeModifier with ADDITION on GENERIC_MOVEMENT_SPEED
     * Passive effects: applied with infinite duration (silent)
     */
    public static void applyEquipmentStats(ServerPlayerEntity player) {
        int   totalHp     = 0;
        float totalDmg    = 0f;
        float totalSpeed  = 0f;

        for (EquipSlot slot : EquipSlot.values()) {
            ItemStack stack = getEquipped(player, slot);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof EquipmentItem eq)) continue;

            int playerLevel = PlayerRpgManager.getLevel(player);
            if (eq.levelRequired > playerLevel) continue; // don't apply if underlevelled

            totalHp    += eq.hpBonus;
            totalDmg   += eq.damageBonus;
            totalSpeed += eq.speedBonus;

            // Passive effect
            if (eq.passiveEffect != null) {
                player.addStatusEffect(new StatusEffectInstance(
                    eq.passiveEffect, 300, eq.effectAmp, true, false));
            }
        }

        // HP: add on top of whatever applyStats just set as base value
        if (totalHp > 0) {
            EntityAttributeInstance hp = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (hp != null) {
                // Remove old modifier first to prevent stacking
                hp.removeModifier(UUID_DMG); // reuse UUID for HP too (stable id per category)
                UUID hpUuid = UUID.fromString("3a6b2c1d-0e8f-3a9b-7c6d-5e4f3a2b1c0d");
                hp.removeModifier(hpUuid);
                if (totalHp > 0) {
                    hp.addPersistentModifier(new EntityAttributeModifier(
                        hpUuid, MOD_NAME + "_hp", totalHp,
                        EntityAttributeModifier.Operation.ADDITION));
                }
            }
        }

        // Attack damage
        EntityAttributeInstance dmg = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.removeModifier(UUID_DMG);
            if (totalDmg > 0) {
                dmg.addPersistentModifier(new EntityAttributeModifier(
                    UUID_DMG, MOD_NAME + "_dmg", totalDmg,
                    EntityAttributeModifier.Operation.ADDITION));
            }
        }

        // Speed
        EntityAttributeInstance spd = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (spd != null) {
            spd.removeModifier(UUID_SPD);
            if (totalSpeed > 0) {
                spd.addPersistentModifier(new EntityAttributeModifier(
                    UUID_SPD, MOD_NAME + "_spd", totalSpeed,
                    EntityAttributeModifier.Operation.ADDITION));
            }
        }
    }

    // ── COMMAND HELPERS ───────────────────────────────────────────────────────

    /** Sends a formatted equipment status message to chat. Used by /isekraft equipment */
    public static void printEquipmentStatus(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("═══ Equipment ═══").formatted(Formatting.GOLD), false);
        for (EquipSlot slot : EquipSlot.values()) {
            ItemStack eq = getEquipped(player, slot);
            String itemName = eq.isEmpty() ? "§8— empty —" : "§f" + eq.getName().getString();
            player.sendMessage(Text.literal("  " + slot.label + ": " + itemName), false);
        }
    }
}
