package com.isekraft.mixin;

import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void onWrite(NbtCompound nbt, CallbackInfo ci) {
        PlayerRpgManager.saveToNbt((ServerPlayerEntity)(Object)this, nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onRead(NbtCompound nbt, CallbackInfo ci) {
        PlayerRpgManager.loadFromNbt((ServerPlayerEntity)(Object)this, nbt);
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void onCopyFrom(ServerPlayerEntity old, boolean alive, CallbackInfo ci) {
        PlayerRpgManager.copyFrom((ServerPlayerEntity)(Object)this, old);
    }

    @Inject(method = "playerTick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        if (player.age % 200 == 0) PlayerRpgManager.applyStats(player);
    }

    /**
     * Mana Shield (mage_4): 15% chance to negate any incoming hit.
     */
    @Inject(
        method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDamage(DamageSource source, float amount,
                          CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        if (!PlayerRpgManager.isSkillActive(player, "mage_4")) return;
        if (player.getRandom().nextFloat() < 0.15f) {
            player.getWorld().sendEntityStatus(player,
                EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES);
            cir.setReturnValue(false);
        }
    }
}
