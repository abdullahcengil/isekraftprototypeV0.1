package com.isekraft.mixin;

import com.isekraft.network.ModPackets;
import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts right-click on vanilla Villagers.
 *
 * On MAIN_HAND interact, server sends OPEN_QUEST_BOARD packet to the player.
 * The vanilla trading GUI is still opened AFTER our packet — we just prepend
 * the quest board. The player sees our screen first; closing it returns to
 * normal gameplay (vanilla trade screen is NOT suppressed).
 *
 * If you want to suppress the trade GUI entirely, change RETURN to
 * cir.setReturnValue(ActionResult.SUCCESS) and uncomment the cancel line.
 */
@Mixin(VillagerEntity.class)
public class VillagerInteractMixin {

    @Inject(
        method = "interactMob(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
        at = @At("HEAD")
    )
    private void onInteract(PlayerEntity player, Hand hand,
                            CallbackInfoReturnable<ActionResult> cir) {
        if (hand != Hand.MAIN_HAND) return;
        if (player.getWorld().isClient) return;
        if (!(player instanceof ServerPlayerEntity sp)) return;

        VillagerEntity villager = (VillagerEntity)(Object)this;
        String name = villager.hasCustomName()
            ? villager.getCustomName().getString()
            : "Villager";

        ModPackets.sendQuestBoardPacket(sp, name);
    }
}
