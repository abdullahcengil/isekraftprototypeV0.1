package com.isekraft.item;
import net.minecraft.util.math.BlockPos;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * Teleport Stone — right-click to teleport to your spawn point.
 * 30 second cooldown.
 */
public class TeleportStoneItem extends Item {

    public TeleportStoneItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);

        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            // Teleport to spawn
            ServerWorld sw = sp.getServer().getOverworld();
            BlockPos spawn = sp.getSpawnPointPosition();
            if (spawn == null) spawn = sw.getSpawnPos();

            sp.teleport(sw, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, sp.getYaw(), sp.getPitch());
            sp.sendMessage(Text.literal("✦ Teleported to spawn!").formatted(Formatting.LIGHT_PURPLE), false);
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
        }

        user.getItemCooldownManager().set(this, 600); // 30 sec cooldown
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("A stone humming with ancient magic.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("✦ Right-click: Teleport to spawn").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("  30 second cooldown").formatted(Formatting.DARK_GRAY));
    }
}
