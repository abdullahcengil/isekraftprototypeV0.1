package com.isekraft.item;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Hand;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.List;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;

/**
 * Mount Whistle — right-click to summon a personal fast horse.
 * The horse is tamed and saddled automatically.
 * 10 second cooldown.
 */
public class MountCommandItem extends Item {

    public MountCommandItem(Settings s) { super(s); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);

        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            ServerWorld sw = (ServerWorld) world;

            // Spawn horse right next to player
            HorseEntity horse = EntityType.HORSE.create(sw);
            if (horse != null) {
                horse.setPos(sp.getX() + 2, sp.getY(), sp.getZ());

                // Make it fast and strong
                horse.getAttributeInstance(EntityAttributes.HORSE_JUMP_STRENGTH)
                    .setBaseValue(1.2);
                horse.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                    .setBaseValue(0.45); // vanilla max is ~0.3375

                // Tame it and saddle it
                horse.setTame(true);
                horse.setOwnerUuid(sp.getUuid());

                NbtCompound horseNbt = new NbtCompound();
                horse.writeNbt(horseNbt);
                horseNbt.putBoolean("SaddleItem", true);
                horse.readNbt(horseNbt);

                // Saddle the horse
                horse.equipStack(EquipmentSlot.CHEST,
                    new ItemStack(Items.SADDLE));

                sw.spawnEntity(horse);

                // Mount the player
                horse.startRiding(sp, true);
                sp.startRiding(horse, true);

                sp.sendMessage(Text.literal("✦ Your mount has arrived!").formatted(Formatting.YELLOW), false);
                world.playSound(null, sp.getBlockPos(),
                    SoundEvents.ENTITY_HORSE_AMBIENT, SoundCategory.NEUTRAL, 1f, 1f);
            }
        }

        user.getItemCooldownManager().set(this, 200); // 10s cooldown
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("A whistle carved from dragon bone.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("✦ Right-click: Summon a fast horse").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("  10 second cooldown").formatted(Formatting.DARK_GRAY));
    }
}
