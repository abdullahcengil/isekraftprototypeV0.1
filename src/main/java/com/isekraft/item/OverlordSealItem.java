package com.isekraft.item;

import com.isekraft.entity.ModEntities;
import com.isekraft.entity.OverlordGuardEntity;
import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;
import net.minecraft.util.math.Box;

/**
 * Overlord's Seal — exclusive level-100 item. Dropped at level 100.
 *
 * RIGHT-CLICK (air/ground):
 *   < 4 guards → summon one guard near player
 *   = 4 guards → dismiss all guards
 *
 * RIGHT-CLICK on ENTITY:
 *   If mob → set as target for all guards
 *   If player → refused (guards never attack players)
 *
 * SNEAK+RIGHT-CLICK:
 *   Toggle "attack all hostiles" mode on all guards
 *
 * 3s cooldown enforced on both use() and useOnEntity().
 */
public class OverlordSealItem extends Item {

    private static final int MAX_GUARDS    = 4;
    private static final int COOLDOWN      = 60; // 3 seconds

    public OverlordSealItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);
        if (user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);

        if (PlayerRpgManager.getLevel(user) < 100) {
            user.sendMessage(Text.literal("☠ Only Isekai Overlords (level 100) may use this Seal.")
                .formatted(Formatting.DARK_RED), false);
            return TypedActionResult.fail(stack);
        }

        ServerPlayerEntity sp = (ServerPlayerEntity) user;
        ServerWorld sw = sp.getServerWorld();
        List<OverlordGuardEntity> myGuards = getMyGuards(sw, sp.getUuid());

        if (user.isSneaking()) {
            // Toggle attack-all mode
            if (myGuards.isEmpty()) {
                user.sendMessage(Text.literal("No guards active. Summon guards first (right-click).")
                    .formatted(Formatting.GRAY), false);
            } else {
                boolean newMode = !myGuards.get(0).isAttackingAllMobs();
                myGuards.forEach(g -> g.setAttackAllMobs(newMode));
                user.sendMessage(Text.literal(newMode
                    ? "☠ Guards: Attack all hostiles!"
                    : "○ Guards: Follow mode (target specific enemies with right-click on mob)")
                    .formatted(newMode ? Formatting.RED : Formatting.GRAY), false);
                sw.playSound(null, user.getBlockPos(),
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS,
                    1f, newMode ? 0.6f : 1.2f);
            }
        } else if (myGuards.size() >= MAX_GUARDS) {
            // Dismiss all
            myGuards.forEach(Entity::discard);
            user.sendMessage(Text.literal("Your Overlord Guards have been dismissed.")
                .formatted(Formatting.DARK_GRAY), false);
            sw.playSound(null, user.getBlockPos(),
                SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 0.5f, 1.5f);
        } else {
            // Summon one guard
            OverlordGuardEntity guard = ModEntities.OVERLORD_GUARD.create(sw);
            if (guard != null) {
                int existing = myGuards.size();
                double angle  = existing * (Math.PI * 2.0 / MAX_GUARDS);
                double spawnX = user.getX() + Math.sin(angle) * 2.5;
                double spawnZ = user.getZ() + Math.cos(angle) * 2.5;

                guard.setPos(spawnX, user.getY(), spawnZ);
                guard.setOwner(sp.getUuid());
                guard.setCustomName(Text.literal("⚔ Guard of " + user.getName().getString())
                    .formatted(Formatting.DARK_PURPLE));
                guard.setCustomNameVisible(true);
                guard.initialize(sw, sw.getLocalDifficulty(user.getBlockPos()),
                    SpawnReason.COMMAND, null, null);
                sw.spawnEntity(guard);

                sw.spawnParticles(ParticleTypes.SOUL, spawnX, user.getY() + 1, spawnZ,
                    12, 0.3, 0.5, 0.3, 0.05);
                sw.playSound(null, user.getBlockPos(),
                    SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 0.6f, 1.3f);

                int count = existing + 1;
                user.sendMessage(Text.literal("⚔ Guard summoned! (" + count + "/" + MAX_GUARDS + ")")
                    .formatted(Formatting.LIGHT_PURPLE), false);
                if (count == 1)
                    user.sendMessage(Text.literal(
                        "  Look at a mob + right-click to target. Sneak+right-click = attack all hostiles.")
                        .formatted(Formatting.DARK_GRAY), false);
            }
        }

        user.getItemCooldownManager().set(this, COOLDOWN);
        return TypedActionResult.success(stack, false);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getWorld().isClient) return ActionResult.PASS;
        // FIX: also check cooldown in useOnEntity
        if (user.getItemCooldownManager().isCoolingDown(this)) return ActionResult.PASS;
        if (!(user instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
        if (PlayerRpgManager.getLevel(user) < 100) return ActionResult.PASS;

        // Never target players
        if (entity instanceof PlayerEntity) {
            user.sendMessage(Text.literal("Guards will never target players.")
                .formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        }

        ServerWorld sw = sp.getServerWorld();
        List<OverlordGuardEntity> myGuards = getMyGuards(sw, sp.getUuid());

        if (myGuards.isEmpty()) {
            user.sendMessage(Text.literal("No guards active. Summon guards first.")
                .formatted(Formatting.GRAY), false);
            return ActionResult.FAIL;
        }

        UUID targetId = entity.getUuid();
        myGuards.forEach(g -> g.setTargetEntity(targetId));
        user.sendMessage(Text.literal("⚔ Guards targeting: ")
            .append(entity.getDisplayName()).append(Text.literal("!"))
            .formatted(Formatting.RED), false);
        sw.playSound(null, user.getBlockPos(),
            SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.8f, 0.5f);

        user.getItemCooldownManager().set(this, COOLDOWN);
        return ActionResult.SUCCESS;
    }

    /**
     * Finds all guards owned by this player.
     * Scans a generous 512-block radius around world origin center — covers any reasonable
     * play area without the overhead of iterating every loaded entity in the world.
     * Guards teleport to owner if >24 blocks away, so they're always nearby.
     */
    private static List<OverlordGuardEntity> getMyGuards(ServerWorld sw, UUID ownerUuid) {
        // Use a large centered search box. Guards are always near their owner (teleport mechanic).
        return sw.getEntitiesByClass(OverlordGuardEntity.class,
            new Box(-30_000_000, -64, -30_000_000,
                                             30_000_000, 320,  30_000_000),
            g -> ownerUuid.equals(g.getOwnerUuid()));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Seal of the Isekai Overlord.").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        tooltip.add(Text.literal("Requires level 100.").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("✦ Right-click: Summon Guard (max 4) / at 4: Dismiss all").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("✦ Right-click on mob: Guards target that mob").formatted(Formatting.RED));
        tooltip.add(Text.literal("✦ Sneak+right-click: Toggle attack-all hostiles mode").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("  Guards last 10 minutes then fade.").formatted(Formatting.DARK_GRAY));
    }
}
