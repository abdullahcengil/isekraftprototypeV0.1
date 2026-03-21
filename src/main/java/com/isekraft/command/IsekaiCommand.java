package com.isekraft.command;

import com.isekraft.entity.ModEntities;
import com.isekraft.entity.OverlordGuardEntity;
import com.isekraft.entity.WitchCovenEntity;
import com.isekraft.item.ModItems;
import com.isekraft.rpg.PlayerRpgManager;
import com.isekraft.world.BattleTowerGenerator;
import com.isekraft.world.DemonLordEvent;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.literal;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.random.Random;
import net.minecraft.block.Block;

/**
 * /isekraft commands:
 *   info                — show RPG status
 *   skills              — show path + unlocked skills
 *   kit                 — starter items
 *   resetskills         — reset all skills + path (keeps level)
 *   setlevel <n>        — admin: set level
 *   summon npc/boss/demon/tower/witches — admin spawns
 */
public class IsekaiCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(IsekaiCommand::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess access,
                                         CommandManager.RegistrationEnvironment env) {
        dispatcher.register(
            literal("isekraft")

                // /isekraft info
                .then(literal("info").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    int level  = PlayerRpgManager.getLevel(player);
                    int xp     = PlayerRpgManager.getXp(player);
                    int needed = PlayerRpgManager.getXpRequired(level);
                    int kills  = PlayerRpgManager.getTotalKills(player);
                    player.sendMessage(Text.literal("═══ IseKraft RPG Status ═══").formatted(Formatting.GOLD), false);
                    player.sendMessage(Text.literal("  Level: " + level + " — " + PlayerRpgManager.getLevelTitle(level)).formatted(Formatting.YELLOW), false);
                    player.sendMessage(Text.literal("  XP: " + xp + " / " + needed).formatted(Formatting.GREEN), false);
                    player.sendMessage(Text.literal("  Kills: " + kills).formatted(Formatting.RED), false);
                    player.sendMessage(Text.literal("  Next milestone: " + getNextMilestone(level)).formatted(Formatting.AQUA), false);
                    player.sendMessage(Text.literal("  Skill Points: " + PlayerRpgManager.getAvailableSkillPoints(player) + " available  |  Press K").formatted(Formatting.LIGHT_PURPLE), false);
                    return 1;
                }))

                // /isekraft skills
                .then(literal("skills").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    int path = PlayerRpgManager.getChosenPath(player);
                    String pathName = switch (path) {
                        case PlayerRpgManager.PATH_WARRIOR -> "⚔ Warrior";
                        case PlayerRpgManager.PATH_MAGE    -> "✦ Mage";
                        case PlayerRpgManager.PATH_RANGER  -> "🏹 Ranger";
                        default -> "None chosen";
                    };
                    player.sendMessage(Text.literal("═══ Skill Tree ═══").formatted(Formatting.LIGHT_PURPLE), false);
                    player.sendMessage(Text.literal("  Path: " + pathName).formatted(Formatting.GOLD), false);
                    player.sendMessage(Text.literal("  Points: " + PlayerRpgManager.getAvailableSkillPoints(player) +
                        " available / " + PlayerRpgManager.getSpentSkillPoints(player) + " spent").formatted(Formatting.AQUA), false);
                    for (String id : PlayerRpgManager.ALL_SKILLS) {
                        if (PlayerRpgManager.isSkillUnlocked(player, id)) {
                            player.sendMessage(Text.literal("  ✓ " + PlayerRpgManager.getSkillDisplayName(id)).formatted(Formatting.GREEN), false);
                        }
                    }
                    player.sendMessage(Text.literal("  Use /isekraft resetskills to reset path.").formatted(Formatting.DARK_GRAY), false);
                    return 1;
                }))

                // /isekraft resetskills
                .then(literal("resetskills").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    PlayerRpgManager.resetSkills(player);
                    return 1;
                }))

                // /isekraft setlevel <n>
                .then(literal("setlevel")
                    .then(CommandManager.argument("level",
                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            if (player == null) return 0;
                            int target = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level");
                            NbtCompound data = PlayerRpgManager.getData(player);
                            data.putInt(PlayerRpgManager.LEVEL, target);
                            data.putInt("XP", 0);
                            if (target >= 100) {
                                data.putBoolean("DemonLordDone", true);
                                DemonLordEvent.markTriggered(player.getName().getString());
                            }
                            PlayerRpgManager.setData(player, data);
                            PlayerRpgManager.applyStats(player);
                            player.sendMessage(Text.literal("Level set to " + target).formatted(Formatting.GOLD), false);
                            return 1;
                        })))

                // /isekraft kit — scales every 10 levels, safe inventory insertion
                .then(literal("kit").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player == null) return 0;
                    int level = PlayerRpgManager.getLevel(player);
                    int tier  = level / 10; // 0=lv0-9, 1=lv10-19, ... 9=lv90-99+

                    List<ItemStack> items = new ArrayList<>();

                    // Base kit (always available)
                    items.add(new ItemStack(ModItems.RUNE_FRAGMENT,  8 + tier * 4));
                    items.add(new ItemStack(ModItems.SOUL_CRYSTAL,   4 + tier * 2));
                    items.add(new ItemStack(ModItems.HEALTH_POTION,  2 + Math.max(0, tier)));

                    // Tier upgrades — unlock progressively
                    if (tier >= 1) { items.add(new ItemStack(ModItems.RUNE_SWORD));
                                     items.add(new ItemStack(Items.IRON_INGOT, 4 + tier * 2));
                                     items.add(new ItemStack(Items.GOLDEN_APPLE, 1 + tier / 3)); }
                    if (tier >= 2)   items.add(new ItemStack(ModItems.ARCANE_STAFF));
                    if (tier >= 3)   items.add(new ItemStack(ModItems.SPIRIT_ESSENCE, 2));
                    if (tier >= 4)   items.add(new ItemStack(ModItems.BERSERKER_SWORD));
                    if (tier >= 5) { items.add(new ItemStack(ModItems.SOUL_CRYSTAL, 8));
                                     items.add(new ItemStack(Items.DIAMOND, 2 + tier / 2)); }
                    if (tier >= 6)   items.add(new ItemStack(ModItems.SOUL_BOW));
                    if (tier >= 7)   items.add(new ItemStack(ModItems.DEMON_CORE, 2));
                    if (tier >= 8) { items.add(new ItemStack(ModItems.MANA_CRYSTAL, 4));
                                     items.add(new ItemStack(ModItems.WAR_HAMMER_DIAMOND)); }
                    if (tier >= 9)   items.add(new ItemStack(ModItems.DEMON_CORE, 4));

                    // Safe insertion — overflow drops at player feet
                    int given = 0, dropped = 0;
                    for (net.minecraft.item.ItemStack stack : items) {
                        if (!player.getInventory().insertStack(stack)) {
                            player.dropItem(stack, false);
                            dropped++;
                        } else {
                            given++;
                        }
                    }

                    player.sendMessage(Text.literal("✦ Tier " + tier + " kit! ("
                        + given + " in inventory"
                        + (dropped > 0 ? ", " + dropped + " dropped nearby" : "") + ")")
                        .formatted(Formatting.LIGHT_PURPLE), false);
                    if (tier < 9)
                        player.sendMessage(Text.literal("  Next tier upgrades at level " + ((tier+1)*10))
                            .formatted(Formatting.DARK_GRAY), false);
                    return 1;
                }))

                .then(literal("summon")

                    .then(literal("npc").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = player.getServerWorld();
                        Entity npc = ModEntities.ISEKAI_NPC.create(world);
                        if (npc != null) {
                            npc.setPos(player.getX() + 2, player.getY(), player.getZ() + 2);
                            world.spawnEntity(npc);
                            player.sendMessage(Text.literal("An Isekai Guide appears!").formatted(Formatting.AQUA), false);
                        }
                        return 1;
                    }))

                    .then(literal("boss").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = player.getServerWorld();
                        Entity boss = ModEntities.GOBLIN_KING.create(world);
                        if (boss != null) {
                            boss.setPos(player.getX() + 5, player.getY(), player.getZ() + 5);
                            world.spawnEntity(boss);
                            player.sendMessage(Text.literal("The Goblin King has arrived!").formatted(Formatting.DARK_RED), false);
                        }
                        return 1;
                    }))

                    .then(literal("demon").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = player.getServerWorld();
                        com.isekraft.entity.ShadowDemonEntity demon = ModEntities.SHADOW_DEMON.create(world);
                        if (demon != null) {
                            demon.setPos(player.getX(), player.getY() + 1, player.getZ() - 15);
                            world.spawnEntity(demon);
                            player.sendMessage(Text.literal("☠ Shadow Demon summoned!").formatted(Formatting.DARK_PURPLE), false);
                        }
                        return 1;
                    }))

                    .then(literal("tower").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = player.getServerWorld();
                        BlockPos pos = player.getBlockPos().add(10, 0, 10);
                        Random rng =
                            Random.create(world.getTime());
                        List<BattleTowerGenerator.BlockPlacement> placements = new ArrayList<>();
                        com.isekraft.world.BattleTowerGenerator.queue(pos, rng, placements);
                        for (BattleTowerGenerator.BlockPlacement bp : placements)
                            world.setBlockState(bp.pos(), bp.state(),
                                Block.FORCE_STATE | Block.SKIP_DROPS);
                        for (BattleTowerGenerator.BlockPlacement bp : placements)
                            world.setBlockState(bp.pos(), bp.state(),
                                Block.NOTIFY_ALL | Block.FORCE_STATE);
                        com.isekraft.world.BattleTowerGenerator.setupSpawnersAndChests(world, pos, rng, null);
                        player.sendMessage(Text.literal("Battle Tower spawned at " + pos).formatted(Formatting.GREEN), false);
                        return 1;
                    }))

                    // /isekraft summon guard — test spawn an Overlord Guard
                    .then(literal("guard").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = player.getServerWorld();
                        OverlordGuardEntity guard = ModEntities.OVERLORD_GUARD.create(world);
                        if (guard != null) {
                            guard.setPos(player.getX() + 2, player.getY(), player.getZ());
                            guard.setOwner(player.getUuid());
                            guard.setCustomName(Text.literal("⚔ Guard of " + player.getName().getString())
                                .formatted(Formatting.DARK_PURPLE));
                            guard.setCustomNameVisible(true);
                            guard.initialize(world, world.getLocalDifficulty(player.getBlockPos()),
                                net.minecraft.entity.SpawnReason.COMMAND, null, null);
                            world.spawnEntity(guard);
                            player.sendMessage(Text.literal("Overlord Guard spawned!")
                                .formatted(Formatting.LIGHT_PURPLE), false);
                        }
                        return 1;
                    }))

                    // /isekraft summon witches — spawns the full Witch Coven (all 3)
                    .then(literal("witches").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        ServerWorld world = player.getServerWorld();
                        BlockPos base = player.getBlockPos().add(0, 0, -12);

                        // Spawn MORVAINE — she will auto-spawn the others via initialize()
                        WitchCovenEntity morvaine = ModEntities.WITCH_COVEN.create(world);
                        if (morvaine != null) {
                            morvaine.setRole(WitchCovenEntity.WitchRole.MORVAINE);
                            morvaine.setPos(base.getX(), base.getY(), base.getZ());
                            morvaine.initialize(world, world.getLocalDifficulty(base),
                                SpawnReason.EVENT, null, null);
                            world.spawnEntity(morvaine);
                        }
                        player.sendMessage(Text.literal("☽ The Witch Coven awakens!")
                            .formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
                        player.sendMessage(Text.literal("  Morvaine · Seraphel · Hexara appear in the shadows.")
                            .formatted(Formatting.LIGHT_PURPLE), false);
                        return 1;
                    }))
                )
        );
    }

    private static String getNextMilestone(int level) {
        int[] milestones = {5,10,20,30,40,50,75,100};
        for (int m : milestones) if (m > level) return "Level " + m;
        return "MAX LEVEL REACHED!";
    }
}
