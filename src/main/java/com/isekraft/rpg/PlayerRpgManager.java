package com.isekraft.rpg;

import com.isekraft.IseKraftMod;
import com.isekraft.item.ModItems;
import com.isekraft.equipment.EquipmentManager;
import com.isekraft.quest.QuestManager;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.item.Item;

/**
 * Central RPG data store.
 *
 * SKILL SYSTEM v3 — Unlock vs Active separation:
 *   "UnlockedSkills" = permanently learned skills (comma-separated)
 *   "ActiveSkills"   = currently active skills (subset of unlocked, max=5)
 *
 * Players can toggle any unlocked skill on/off via Skill Tree screen.
 * Only ACTIVE skills apply effects. Unlocked skills are never lost.
 * Max 5 active at once (interesting decisions, prevents stacking all passives).
 *
 * LEVEL IDENTITY:
 *   Each title threshold triggers a unique effect + server broadcast.
 *   Level 100 (Isekai Overlord): permanent Overlord buff, receives Overlord's Seal item.
 */
public class PlayerRpgManager {

    private static final Map<UUID, NbtCompound> DATA = new HashMap<>();
    private static final Map<UUID, Integer>     LAST_STAT_LEVEL = new HashMap<>();

    private static final String XP              = "XP";
    private static final String KILLS           = "Kills";
    private static final String UNLOCKED_SKILLS = "UnlockedSkills";
    private static final String ACTIVE_SKILLS   = "ActiveSkills";
    private static final String SKILL_PATH      = "SkillPath";
    public  static final String LEVEL           = "Level";
    public  static final int    MAX_LEVEL       = 100;
    public  static final int    MAX_ACTIVE      = 5; // max active skills at once

    public static final int PATH_NONE    = 0;
    public static final int PATH_WARRIOR = 1;
    public static final int PATH_MAGE    = 2;
    public static final int PATH_RANGER  = 3;

    public static final String[] ALL_SKILLS = {
        "warrior_1","warrior_2","warrior_3","warrior_4","warrior_5",
        "mage_1","mage_2","mage_3","mage_4","mage_5",
        "ranger_1","ranger_2","ranger_3","ranger_4","ranger_5"
    };

    // ── READ ─────────────────────────────────────────────────────────────────

    public static int getLevel(PlayerEntity p)      { return Math.max(1, get(p).getInt(LEVEL)); }
    public static int getXp(PlayerEntity p)         { return get(p).getInt(XP); }
    public static int getTotalKills(PlayerEntity p) { return get(p).getInt(KILLS); }
    public static int getXpRequired(int level)      { return 100 + level * 25; }

    public static String getLevelTitle(int level) {
        if (level < 5)   return "Summoned Soul";
        if (level < 10)  return "Wandering Hero";
        if (level < 20)  return "Adventurer";
        if (level < 30)  return "Knight";
        if (level < 40)  return "Warrior";
        if (level < 50)  return "Champion";
        if (level < 60)  return "Crusader";
        if (level < 70)  return "Warlord";
        if (level < 80)  return "Dragon Slayer";
        if (level < 90)  return "Legendary Hero";
        if (level < 100) return "Demon King's Rival";
        return "Isekai Overlord";
    }

    // ── SKILL SYSTEM ─────────────────────────────────────────────────────────

    public static int getTotalSkillPoints(PlayerEntity p) { return getLevel(p) / 5; }

    public static int getSpentSkillPoints(PlayerEntity p) {
        String s = get(p).getString(UNLOCKED_SKILLS);
        if (s.isEmpty()) return 0;
        int c = 0; for (String sk : s.split(",")) if (!sk.isEmpty()) c++;
        return c;
    }

    public static int getAvailableSkillPoints(PlayerEntity p) {
        return Math.max(0, getTotalSkillPoints(p) - getSpentSkillPoints(p));
    }

    public static int getChosenPath(PlayerEntity p) { return get(p).getInt(SKILL_PATH); }

    /** Skill has been permanently LEARNED (spent a point on it). */
    public static boolean isSkillUnlocked(PlayerEntity p, String skillId) {
        return containsSkill(get(p).getString(UNLOCKED_SKILLS), skillId);
    }

    /** Skill is ACTIVE (unlocked AND toggled on by player). Only active skills give bonuses. */
    public static boolean isSkillActive(PlayerEntity p, String skillId) {
        return containsSkill(get(p).getString(ACTIVE_SKILLS), skillId);
    }

    public static int getActiveCount(PlayerEntity p) {
        String s = get(p).getString(ACTIVE_SKILLS);
        if (s.isEmpty()) return 0;
        int c = 0; for (String sk : s.split(",")) if (!sk.isEmpty()) c++;
        return c;
    }

    private static boolean containsSkill(String csv, String skillId) {
        if (csv.isEmpty()) return false;
        for (String s : csv.split(",")) if (s.equals(skillId)) return true;
        return false;
    }

    public static boolean isPrerequisiteMet(PlayerEntity p, String skillId) {
        int idx = -1;
        for (int i = 0; i < ALL_SKILLS.length; i++)
            if (ALL_SKILLS[i].equals(skillId)) { idx = i; break; }
        if (idx < 0) return false;
        if (idx % 5 == 0) return true;
        return isSkillUnlocked(p, ALL_SKILLS[idx - 1]);
    }

    public static int getSkillPathId(String skillId) {
        if (skillId.startsWith("warrior")) return PATH_WARRIOR;
        if (skillId.startsWith("mage"))    return PATH_MAGE;
        if (skillId.startsWith("ranger"))  return PATH_RANGER;
        return PATH_NONE;
    }

    /** UNLOCK a new skill (spends a point, permanently learned, auto-activates if slot free). */
    public static boolean unlockSkill(ServerPlayerEntity p, String skillId) {
        if (getAvailableSkillPoints(p) <= 0) {
            p.sendMessage(Text.literal("No skill points available!").formatted(Formatting.RED), false);
            return false;
        }
        if (isSkillUnlocked(p, skillId)) return false;

        int skillPath  = getSkillPathId(skillId);
        int chosenPath = getChosenPath(p);
        if (chosenPath != PATH_NONE && chosenPath != skillPath) {
            p.sendMessage(Text.literal("Path locked to " + pathName(chosenPath)
                + ". Use /isekraft resetskills to reset.").formatted(Formatting.RED), false);
            return false;
        }
        if (!isPrerequisiteMet(p, skillId)) {
            p.sendMessage(Text.literal("Unlock the previous skill first!").formatted(Formatting.RED), false);
            return false;
        }

        NbtCompound d = get(p);
        // Unlock (permanent)
        String unlocked = d.getString(UNLOCKED_SKILLS);
        d.putString(UNLOCKED_SKILLS, unlocked.isEmpty() ? skillId : unlocked + "," + skillId);
        // Lock path
        if (chosenPath == PATH_NONE) d.putInt(SKILL_PATH, skillPath);
        // Auto-activate if slot available
        if (getActiveCount(p) < MAX_ACTIVE) {
            String active = d.getString(ACTIVE_SKILLS);
            d.putString(ACTIVE_SKILLS, active.isEmpty() ? skillId : active + "," + skillId);
        }
        put(p, d);
        applyStats(p);

        boolean autoActivated = isSkillActive(p, skillId);
        p.sendMessage(Text.literal("✦ Skill unlocked: " + getSkillDisplayName(skillId))
            .formatted(Formatting.GREEN), false);
        p.sendMessage(Text.literal("  " + getSkillDesc(skillId)).formatted(Formatting.GRAY), false);
        if (!autoActivated)
            p.sendMessage(Text.literal("  ★ Active slots full — toggle in Skill Tree (K)")
                .formatted(Formatting.YELLOW), false);
        return true;
    }

    /** TOGGLE an already-unlocked skill on/off. */
    public static void toggleSkill(ServerPlayerEntity p, String skillId) {
        if (!isSkillUnlocked(p, skillId)) {
            p.sendMessage(Text.literal("Skill not unlocked!").formatted(Formatting.RED), false);
            return;
        }
        NbtCompound d = get(p);
        if (isSkillActive(p, skillId)) {
            // Deactivate
            d.putString(ACTIVE_SKILLS, removeSkill(d.getString(ACTIVE_SKILLS), skillId));
            put(p, d);
            applyStats(p);
            p.sendMessage(Text.literal("○ " + getSkillDisplayName(skillId) + " — deactivated")
                .formatted(Formatting.GRAY), false);
        } else {
            // Activate (if slot available)
            if (getActiveCount(p) >= MAX_ACTIVE) {
                p.sendMessage(Text.literal("Active skill slots full (" + MAX_ACTIVE
                    + "). Deactivate another skill first.").formatted(Formatting.RED), false);
                return;
            }
            String active = d.getString(ACTIVE_SKILLS);
            d.putString(ACTIVE_SKILLS, active.isEmpty() ? skillId : active + "," + skillId);
            put(p, d);
            applyStats(p);
            p.sendMessage(Text.literal("★ " + getSkillDisplayName(skillId) + " — activated!")
                .formatted(Formatting.GREEN), false);
        }
    }

    private static String removeSkill(String csv, String skillId) {
        if (csv.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : csv.split(",")) {
            if (!s.isEmpty() && !s.equals(skillId)) {
                if (sb.length() > 0) sb.append(",");
                sb.append(s);
            }
        }
        return sb.toString();
    }

    public static void resetSkills(ServerPlayerEntity p) {
        NbtCompound d = get(p);
        d.putString(UNLOCKED_SKILLS, "");
        d.putString(ACTIVE_SKILLS, "");
        d.putInt(SKILL_PATH, PATH_NONE);
        put(p, d);
        applyStats(p);
        p.sendMessage(Text.literal("✦ All skills reset. Points refunded. Path unlocked.").formatted(Formatting.GOLD), false);
    }

    public static String getSkillDisplayName(String id) {
        return switch (id) {
            case "warrior_1" -> "Iron Will";
            case "warrior_2" -> "Veteran's Hide";
            case "warrior_3" -> "War Mastery";
            case "warrior_4" -> "Battle Cry";
            case "warrior_5" -> "Undying Rage";
            case "mage_1"    -> "Arcane Focus";
            case "mage_2"    -> "Soul Surge";
            case "mage_3"    -> "Void Mastery";
            case "mage_4"    -> "Mana Shield";
            case "mage_5"    -> "Arcane Barrage";
            case "ranger_1"  -> "Swift Feet";
            case "ranger_2"  -> "Predator";
            case "ranger_3"  -> "Wind Walker";
            case "ranger_4"  -> "Pack Leader";
            case "ranger_5"  -> "Eagle Eye";
            default -> id;
        };
    }

    public static String getSkillDesc(String id) {
        return switch (id) {
            case "warrior_1" -> "+10 max HP.";
            case "warrior_2" -> "+20 max HP, incoming damage -10%.";
            case "warrior_3" -> "Rune Sword +25% damage.";
            case "warrior_4" -> "Kill → Strength I for 3s.";
            case "warrior_5" -> "Berserker Rage threshold: 40% → 60% HP.";
            case "mage_1"    -> "Arcane Staff cooldown -0.5s.";
            case "mage_2"    -> "Soul Bow +2 damage.";
            case "mage_3"    -> "Permanent Regeneration I.";
            case "mage_4"    -> "15% chance to negate any hit.";
            case "mage_5"    -> "Arcane Staff fires 3 fireballs.";
            case "ranger_1"  -> "+3% movement speed.";
            case "ranger_2"  -> "+20% XP from kills.";
            case "ranger_3"  -> "+5% speed + Speed I.";
            case "ranger_4"  -> "Spirit Beast +10 attack damage.";
            case "ranger_5"  -> "Soul Bow pierces + +3 damage.";
            default -> "";
        };
    }

    private static String pathName(int path) {
        return switch (path) {
            case PATH_WARRIOR -> "Warrior";
            case PATH_MAGE    -> "Mage";
            case PATH_RANGER  -> "Ranger";
            default -> "None";
        };
    }

    // ── WRITE ────────────────────────────────────────────────────────────────

    public static void addXp(PlayerEntity player, int amount) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        NbtCompound d = get(sp);
        int level = Math.max(1, d.getInt(LEVEL));
        if (level >= MAX_LEVEL) return;
        int xp = d.getInt(XP) + amount;
        while (xp >= getXpRequired(level) && level < MAX_LEVEL) {
            xp -= getXpRequired(level);
            level++;
            onLevelUp(sp, level);
        }
        d.putInt(LEVEL, level);
        d.putInt(XP, xp);
        put(sp, d);
        int newLevel = d.getInt(LEVEL);
        if (!LAST_STAT_LEVEL.getOrDefault(sp.getUuid(), 0).equals(newLevel)) {
            LAST_STAT_LEVEL.put(sp.getUuid(), newLevel);
            applyStats(sp);
        }
    }

    public static void recordKill(PlayerEntity player, int mobLevel) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        NbtCompound d = get(sp);
        d.putInt(KILLS, d.getInt(KILLS) + 1);
        put(sp, d);
        int base  = 10 + mobLevel * 5;
        float ratio = (float) mobLevel / Math.max(1, getLevel(sp));
        int xp    = (int)(base * Math.max(0.1f, Math.min(2f, ratio)));
        if (isSkillActive(sp, "ranger_2")) xp = (int)(xp * 1.2f);
        addXp(sp, xp);
        if (isSkillActive(sp, "warrior_4"))
            sp.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0, true, true));
    }

    public static void applyStats(ServerPlayerEntity player) {
        int level = getLevel(player);

        int baseHp = 20 + Math.min(level - 1, 40);
        if (isSkillActive(player, "warrior_1")) baseHp += 10;
        if (isSkillActive(player, "warrior_2")) baseHp += 20;
        EntityAttributeInstance hp = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (hp != null) hp.setBaseValue(baseHp);

        double baseSpeed = 0.1 + (level / 10) * 0.002;
        if (isSkillActive(player, "ranger_1")) baseSpeed += 0.003;
        if (isSkillActive(player, "ranger_3")) baseSpeed += 0.005;
        EntityAttributeInstance spd = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(baseSpeed);

        if (isSkillActive(player, "mage_3"))
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 300, 0, true, false));
        if (isSkillActive(player, "ranger_3"))
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 0, true, false));

        // Isekai Overlord (lv100): permanent silent buffs — renewed every 10s before expiry
        if (level >= 100) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,       400, 4, true, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,     400, 2, true, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,400, 0, true, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,   400, 0, true, false));
        }

        // Equipment: auto-equip upgrades + apply stat bonuses from all 3 slots
        EquipmentManager.autoEquipBest(player);
        EquipmentManager.applyEquipmentStats(player);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    public static void saveToNbt(ServerPlayerEntity player, NbtCompound nbt) {
        nbt.put("IseKraftRPG", DATA.getOrDefault(player.getUuid(), new NbtCompound()).copy());
    }
    public static void loadFromNbt(ServerPlayerEntity player, NbtCompound nbt) {
        if (nbt.contains("IseKraftRPG")) {
            NbtCompound d = nbt.getCompound("IseKraftRPG").copy();
            // Migration: old "Skills" key → new "UnlockedSkills" + "ActiveSkills"
            if (d.contains("Skills") && !d.contains("UnlockedSkills")) {
                String old = d.getString("Skills");
                d.putString("UnlockedSkills", old);
                d.putString("ActiveSkills", old); // all unlocked = active on first migrate
                d.remove("Skills");
            }
            DATA.put(player.getUuid(), d);
        }
        applyStats(player);
    }
    public static void copyFrom(ServerPlayerEntity newPlayer, ServerPlayerEntity oldPlayer) {
        DATA.put(newPlayer.getUuid(), DATA.getOrDefault(oldPlayer.getUuid(), new NbtCompound()).copy());
        applyStats(newPlayer);
    }

    private static NbtCompound get(PlayerEntity p) { return DATA.getOrDefault(p.getUuid(), new NbtCompound()); }
    private static void put(PlayerEntity p, NbtCompound d) { DATA.put(p.getUuid(), d); }
    public static NbtCompound getData(PlayerEntity p) { return get(p); }
    public static void setData(PlayerEntity p, NbtCompound d) { put(p, d); }

    // ── LEVEL UP ─────────────────────────────────────────────────────────────

    private static void onLevelUp(ServerPlayerEntity p, int lv) {
        p.networkHandler.sendPacket(new TitleS2CPacket(
            Text.literal("LEVEL UP!").formatted(Formatting.GOLD, Formatting.BOLD)));
        p.networkHandler.sendPacket(new SubtitleS2CPacket(
            Text.literal("Level " + lv + " — " + getLevelTitle(lv)).formatted(Formatting.YELLOW)));
        p.sendMessage(Text.literal("★ Level " + lv + " — " + getLevelTitle(lv)).formatted(Formatting.GOLD), false);
        QuestManager.onLevelUp(p, lv);

        if (lv % 5 == 0 && lv < MAX_LEVEL)
            p.sendMessage(Text.literal("✦ New skill point! Press K to open Skill Tree.")
                .formatted(Formatting.LIGHT_PURPLE), false);

        // LEVEL IDENTITY — title transitions get a server-wide broadcast + unique effect
        titleIdentityEvent(p, lv);

        // Item rewards
        if (lv == 5)   { give(p, ModItems.RUNE_SWORD, 1);      tip(p, "Level 5 Gift: Rune Sword!", Formatting.LIGHT_PURPLE); }
        if (lv == 10)  { give(p, ModItems.ARCANE_STAFF, 1);    give(p, ModItems.SOUL_CRYSTAL, 5);
                         p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 0));
                         tip(p, "Level 10: Arcane Staff + Soul Crystals + Strength!", Formatting.AQUA); }
        if (lv == 20)  { give(p, ModItems.SPIRIT_ESSENCE, 3);  give(p, ModItems.SOUL_CRYSTAL, 8);
                         tip(p, "Level 20: Spirit Essence — tame a Spirit Beast!", Formatting.GREEN); }
        if (lv == 30)  { give(p, ModItems.BERSERKER_SWORD, 1); give(p, ModItems.SOUL_CRYSTAL, 10);
                         tip(p, "Level 30: Berserker Sword!", Formatting.RED); }
        if (lv == 40)  { give(p, ModItems.SOUL_CRYSTAL, 32);   tip(p, "Level 40: 32 Soul Crystals!", Formatting.AQUA); }
        if (lv == 50)  { give(p, ModItems.DEMON_CORE, 3);      tip(p, "Level 50: Demon Cores!", Formatting.DARK_RED); }
        if (lv == 60)  { give(p, ModItems.SOUL_BOW, 1);        tip(p, "Level 60: Soul Bow!", Formatting.YELLOW); }
        if (lv == 75)  { give(p, ModItems.DEMON_CORE, 8);      tip(p, "Level 75: 8 Demon Cores!", Formatting.DARK_RED); }
        if (lv == 100) {
            give(p, ModItems.DEMON_CORE, 16);
            give(p, ModItems.RUNE_SWORD, 1);
            give(p, ModItems.BERSERKER_SWORD, 1);
            give(p, ModItems.OVERLORD_SEAL, 1);
            // Effects applied by titleIdentityEvent + applyStats maintenance
            tip(p, "LEVEL 100! The Demon Lord Transformation begins...", Formatting.DARK_PURPLE);
        }
        IseKraftMod.LOGGER.info("[IseKraft] {} reached level {}", p.getName().getString(), lv);
    }

    /**
     * LEVEL IDENTITY — each major title milestone triggers a server-wide broadcast
     * and a visual/mechanical effect that makes the player FEEL their new title.
     */
    private static void titleIdentityEvent(ServerPlayerEntity p, int lv) {
        var server = p.getServer();
        if (server == null) return;
        String name = p.getName().getString();

        switch (lv) {
            case 30 -> {
                // KNIGHT → WARRIOR: lightning strike at player position
                server.getPlayerManager().broadcast(
                    Text.literal("⚔ " + name + " has become a Warrior of this world! ⚔")
                        .formatted(Formatting.YELLOW, Formatting.BOLD), false);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 600, 1));
            }
            case 50 -> {
                // CHAMPION: explosion of power, near-players see it
                server.getPlayerManager().broadcast(
                    Text.literal("★ " + name + " — Champion of the Realm! ★")
                        .formatted(Formatting.GOLD, Formatting.BOLD), false);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1200, 2));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 3));
            }
            case 70 -> {
                // WARLORD: feared by all mobs — temporary fear aura
                server.getPlayerManager().broadcast(
                    Text.literal("☠ " + name + " rises as Warlord — the battlefield trembles! ☠")
                        .formatted(Formatting.RED, Formatting.BOLD), false);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 2400, 3));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1200, 2));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 1));
            }
            case 80 -> {
                // DRAGON SLAYER
                server.getPlayerManager().broadcast(
                    Text.literal("✦ " + name + " — Dragon Slayer! Ancient beasts quake! ✦")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 99999, 0));
            }
            case 90 -> {
                // LEGENDARY HERO
                server.getPlayerManager().broadcast(
                    Text.literal("✦ " + name + " — Legendary Hero! Songs will be sung! ✦")
                        .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 99999, 2));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 99999, 3));
            }
            case 99 -> {
                // DEMON KING'S RIVAL
                server.getPlayerManager().broadcast(
                    Text.literal("☠ " + name + " stands at the precipice. One final level remains... ☠")
                        .formatted(Formatting.DARK_PURPLE, Formatting.BOLD), false);
            }
            case 100 -> {
                // ISEKAI OVERLORD — the full treatment
                server.getPlayerManager().broadcast(
                    Text.literal("★★★ " + name + " HAS BECOME THE ISEKAI OVERLORD! ★★★")
                        .formatted(Formatting.GOLD, Formatting.BOLD), false);
                server.getPlayerManager().broadcast(
                    Text.literal("  The Demon Lord's power now flows through " + name + ".")
                        .formatted(Formatting.DARK_PURPLE), false);
                // Permanent overlord buffs (re-applied by applyStats every 10s)
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 999999, 4));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 999999, 2));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 999999, 0));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 999999, 0));
            }
        }
    }

    private static void give(ServerPlayerEntity p, Item item, int count) {
        p.getInventory().insertStack(new ItemStack(item, count));
    }
    private static void tip(ServerPlayerEntity p, String msg, Formatting f) {
        p.sendMessage(Text.literal("✦ " + msg).formatted(f), false);
    }
}
