package com.isekraft.item;

import com.isekraft.entity.ShurikenEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * Shuriken — throwable projectile. Stackable, quick fire.
 *
 * Behaviour:
 *   - Thrown like a snowball (instant, no charge)
 *   - Close range: higher damage (entity explodes at close range)
 *   - Long range: lower damage (projectile travels full arc)
 *   - Each throw consumes 1 shuriken
 *
 * Craft: 2 shurikens per craft
 *   Steel Ingot Steel Ingot
 *   (shapeless: 2 steel = 2 shurikens)
 *   Steel Ingot = 3 Iron Ingots in a row
 *
 * Max stack: 16.
 */
public class ShurikenItem extends Item {

    private static final int COOLDOWN = 4; // 0.2s — fast throw

    public ShurikenItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);

        if (!world.isClient) {
            ShurikenEntity shuriken = new ShurikenEntity(world, user);
            shuriken.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 2.2f, 0.5f);
            world.spawnEntity(shuriken);
            world.playSound(null, user.getBlockPos(),
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.5f, 1.8f);

            // Consume one shuriken (unless creative)
            if (!user.getAbilities().creativeMode)
                stack.decrement(1);
        }

        user.getItemCooldownManager().set(this, COOLDOWN);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("A razor-sharp steel throwing star.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("✦ Right-click to throw").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("✦ Higher damage at close range").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("  Crafted from Steel Ingots").formatted(Formatting.DARK_GRAY));
    }
}
