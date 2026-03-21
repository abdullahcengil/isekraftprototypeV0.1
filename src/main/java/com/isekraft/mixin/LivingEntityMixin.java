package com.isekraft.mixin;

import com.isekraft.world.FoodXpHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects into LivingEntity.eatFood — the correct target in 1.20.1 Yarn.
 * Only fires XP for ServerPlayerEntity instances.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(
        method = "eatFood",
        at = @At("TAIL")
    )
    private void onEatFood(World world, ItemStack stack,
                           CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;
        if (!((Object)this instanceof ServerPlayerEntity player)) return;
        FoodXpHandler.onFoodEaten(player, stack.getItem());
    }
}
