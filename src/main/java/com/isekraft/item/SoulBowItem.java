package com.isekraft.item;

import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
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
 * Soul Bow — right-click to shoot a glowing arrow. Infinite ammo.
 *
 * SKILL INTERACTIONS:
 *   mage_2  (Soul Surge)  — +2 bonus damage per arrow
 *   ranger_5 (Eagle Eye) — +3 damage AND piercing (goes through 1 entity)
 */
public class SoulBowItem extends Item {

    private static final int COOLDOWN_BASE = 10;

    public SoulBowItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);

        if (!world.isClient) {
            boolean soulSurge = PlayerRpgManager.isSkillActive(user, "mage_2");
            boolean eagleEye  = PlayerRpgManager.isSkillActive(user, "ranger_5");

            ArrowEntity arrow = new ArrowEntity(world, user);
            arrow.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 2.5f, 0.5f);

            double damage = 4.0;
            if (soulSurge) damage += 2.0;
            if (eagleEye)  damage += 3.0;
            arrow.setDamage(damage);
            arrow.setGlowing(true);
            arrow.pickupType = ArrowEntity.PickupPermission.DISALLOWED;

            // Eagle Eye: pierce through 1 entity
            if (eagleEye) arrow.setPierceLevel((byte) 1);

            world.spawnEntity(arrow);
            world.playSound(null, user.getBlockPos(),
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1f, eagleEye ? 1.3f : 1f);
        }

        user.getItemCooldownManager().set(this, COOLDOWN_BASE);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Infused with soul energy.").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("✦ Right-click: shoot a glowing arrow").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("✦ Infinite ammo, no draw needed").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("  mage_2: +2 damage").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("  ranger_5: +3 dmg, pierce 1 enemy").formatted(Formatting.DARK_GRAY));
    }
}
