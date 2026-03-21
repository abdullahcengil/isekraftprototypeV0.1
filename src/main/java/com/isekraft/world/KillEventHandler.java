package com.isekraft.world;

import com.isekraft.entity.ModEntities;
import com.isekraft.entity.OverlordGuardEntity;
import com.isekraft.entity.WitchCovenEntity;
import com.isekraft.rpg.PlayerRpgManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Grants XP for ANY mob kill — all weapons, bows, magic, anything.
 * Also routes witch coven achievement tracking via WitchCovenEntity.onDeath.
 */
public class KillEventHandler {

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killedEntity) -> {
            if (!(killer instanceof ServerPlayerEntity player)) return;
            if (!(killedEntity instanceof MobEntity mob)) return;
            int mobLevel = estimateLevel(mob);
            PlayerRpgManager.recordKill(player, mobLevel);
        });
    }

    private static int estimateLevel(MobEntity mob) {
        if (mob.getType() == ModEntities.GOBLIN_KING)  return 20;
        if (mob.getType() == ModEntities.SHADOW_DEMON) return 50;
        if (mob.getType() == ModEntities.DARK_KNIGHT)  return 6;
        if (mob.getType() == ModEntities.FOREST_WOLF)  return 3;
        if (mob.getType() == ModEntities.SPIRIT_BEAST) return 4;
        // Witch Coven members: stronger than Goblin King per member
        if (mob.getType() == ModEntities.OVERLORD_GUARD) return 8; // friendly-ish, low xp
        if (mob.getType() == ModEntities.WITCH_COVEN) {
            if (mob instanceof WitchCovenEntity wc) {
                return switch (wc.getRole()) {
                    case MORVAINE -> 18;
                    case SERAPHEL -> 18;
                    case HEXARA   -> 22; // Hexara is strongest
                };
            }
            return 18;
        }
        // Vanilla mob estimate by HP
        float hp = mob.getMaxHealth();
        if (hp <= 10)  return 1;
        if (hp <= 20)  return 2;
        if (hp <= 40)  return 4;
        if (hp <= 100) return 8;
        return 15;
    }
}
