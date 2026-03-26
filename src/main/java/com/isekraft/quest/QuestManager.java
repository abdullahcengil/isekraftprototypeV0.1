package com.isekraft.quest;

import com.isekraft.rpg.PlayerRpgManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

/**
 * Manages per-player quest state.
 *
 * NBT layout (inside existing IseKraftRPG compound):
 *   "ActiveQuests"    — NbtList of quest id strings
 *   "QuestProgress"   — NbtCompound: questId → int progress
 *   "CompletedQuests" — NbtList of quest id strings
 *
 * Integration:
 *   • KillEventHandler calls QuestManager.onKill(player, entityTypeId)
 *   • QuestManager.onFetchCheck(player) called from /isekraft turnin
 *   • QuestManager.onLevelUp(player, level) called from PlayerRpgManager.onLevelUp
 *
 * Max active quests at once: 3 (encourages choices without overwhelming).
 */
public class QuestManager {

    public static final int MAX_ACTIVE = 3;

    private static final String KEY_ACTIVE    = "ActiveQuests";
    private static final String KEY_PROGRESS  = "QuestProgress";
    private static final String KEY_COMPLETED = "CompletedQuests";

    // ── QUERY ─────────────────────────────────────────────────────────────────

    /** Returns active quest id list — used by packet serialisation. */
    public static List<String> getActiveIds(ServerPlayerEntity p) { return getActive(p); }
    /** Returns completed quest id list — used by packet serialisation. */
    public static List<String> getCompletedIds(ServerPlayerEntity p) { return getCompleted(p); }

    public static List<String> getActive(ServerPlayerEntity p) {
        return readList(p, KEY_ACTIVE);
    }

    public static List<String> getCompleted(ServerPlayerEntity p) {
        return readList(p, KEY_COMPLETED);
    }

    public static int getProgress(ServerPlayerEntity p, String questId) {
        return getProgressMap(p).getInt(questId);
    }

    public static boolean isActive(ServerPlayerEntity p, String questId) {
        return getActive(p).contains(questId);
    }

    public static boolean isCompleted(ServerPlayerEntity p, String questId) {
        return getCompleted(p).contains(questId);
    }

    // ── ACCEPT ────────────────────────────────────────────────────────────────

    public static void acceptQuest(ServerPlayerEntity p, String questId) {
        Quest q = QuestRegistry.get(questId);
        if (q == null) {
            send(p, "Unknown quest: " + questId, Formatting.RED);
            return;
        }
        if (isCompleted(p, questId)) {
            send(p, "You already completed \"" + q.title + "\".", Formatting.GRAY);
            return;
        }
        if (isActive(p, questId)) {
            send(p, "\"" + q.title + "\" is already active.", Formatting.YELLOW);
            return;
        }
        if (getActive(p).size() >= MAX_ACTIVE) {
            send(p, "Active quest limit (" + MAX_ACTIVE + ") reached. Complete one first.", Formatting.RED);
            return;
        }
        if (PlayerRpgManager.getLevel(p) < q.levelRequired) {
            send(p, "Requires level " + q.levelRequired + ".", Formatting.RED);
            return;
        }

        addToList(p, KEY_ACTIVE, questId);
        send(p, "✦ Quest accepted: §e" + q.title, Formatting.GREEN);
        send(p, "  " + q.description, Formatting.GRAY);
        send(p, "  Goal: " + goalText(q), Formatting.AQUA);
    }

    // ── ABANDON ───────────────────────────────────────────────────────────────

    public static void abandonQuest(ServerPlayerEntity p, String questId) {
        if (!isActive(p, questId)) {
            send(p, "That quest isn't active.", Formatting.RED);
            return;
        }
        removeFromList(p, KEY_ACTIVE, questId);
        // Remove progress
        NbtCompound prog = getProgressMap(p);
        prog.remove(questId);
        setProgressMap(p, prog);
        Quest q = QuestRegistry.get(questId);
        send(p, "Quest abandoned: " + (q != null ? q.title : questId), Formatting.GRAY);
    }

    // ── PROGRESS HOOKS ────────────────────────────────────────────────────────

    /**
     * Call from KillEventHandler on every mob kill.
     * entityTypeId = full registry id, e.g. "minecraft:zombie" or "isekraft:dark_knight"
     */
    public static void onKill(ServerPlayerEntity p, String entityTypeId) {
        for (String qId : new ArrayList<>(getActive(p))) {
            Quest q = QuestRegistry.get(qId);
            if (q == null || q.type != Quest.Type.KILL) continue;
            if (!q.targetId.equals(entityTypeId)) continue;

            int prog = getProgress(p, qId) + 1;
            setProgress(p, qId, prog);
            sendActionBar(p, "Quest [" + q.title + "]: " + prog + "/" + q.goal);

            if (prog >= q.goal) completeQuest(p, q);
        }
    }

    /**
     * Call from PlayerRpgManager.onLevelUp for EXPLORE quests.
     */
    public static void onLevelUp(ServerPlayerEntity p, int newLevel) {
        for (String qId : new ArrayList<>(getActive(p))) {
            Quest q = QuestRegistry.get(qId);
            if (q == null || q.type != Quest.Type.EXPLORE) continue;

            setProgress(p, qId, newLevel);
            if (newLevel >= q.goal) completeQuest(p, q);
        }
    }

    /**
     * Call from /isekraft turnin — checks FETCH quests against inventory.
     */
    public static void onFetchTurnIn(ServerPlayerEntity p) {
        boolean found = false;
        for (String qId : new ArrayList<>(getActive(p))) {
            Quest q = QuestRegistry.get(qId);
            if (q == null || q.type != Quest.Type.FETCH) continue;

            // Count matching items in inventory
            int count = 0;
            for (int i = 0; i < p.getInventory().size(); i++) {
                ItemStack stack = p.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    String itemId = net.minecraft.registry.Registries.ITEM
                        .getId(stack.getItem()).toString();
                    if (itemId.equals(q.targetId)) count += stack.getCount();
                }
            }

            if (count >= q.goal) {
                // Consume items
                int toRemove = q.goal;
                for (int i = 0; i < p.getInventory().size() && toRemove > 0; i++) {
                    ItemStack stack = p.getInventory().getStack(i);
                    if (stack.isEmpty()) continue;
                    String itemId = net.minecraft.registry.Registries.ITEM
                        .getId(stack.getItem()).toString();
                    if (!itemId.equals(q.targetId)) continue;
                    int remove = Math.min(toRemove, stack.getCount());
                    stack.decrement(remove);
                    toRemove -= remove;
                }
                completeQuest(p, q);
                found = true;
            } else if (count > 0) {
                send(p, "Quest [" + q.title + "]: " + count + "/" + q.goal + " — keep gathering.", Formatting.YELLOW);
                found = true;
            }
        }
        if (!found) {
            send(p, "No fetch quests active, or you don't have the required items.", Formatting.GRAY);
        }
    }

    // ── COMPLETION ────────────────────────────────────────────────────────────

    private static void completeQuest(ServerPlayerEntity p, Quest q) {
        removeFromList(p, KEY_ACTIVE, q.id);
        addToList(p, KEY_COMPLETED, q.id);

        p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
            Text.literal("Quest Complete!").formatted(Formatting.GOLD, Formatting.BOLD)));
        p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
            Text.literal(q.title).formatted(Formatting.YELLOW)));

        send(p, "════ Quest Complete: §e" + q.title + " §r════", Formatting.GOLD);
        send(p, "  +" + q.xpReward + " XP", Formatting.GREEN);

        PlayerRpgManager.addXp(p, q.xpReward);

        if (q.itemReward != null) {
            ItemStack reward = new ItemStack(q.itemReward, q.itemCount);
            p.getInventory().insertStack(reward);
            send(p, "  +" + q.itemCount + "x " + reward.getName().getString(), Formatting.AQUA);
        }
    }

    // ── STATUS DISPLAY ────────────────────────────────────────────────────────

    public static void printQuestStatus(ServerPlayerEntity p) {
        List<String> active = getActive(p);
        int completed = getCompleted(p).size();
        int total     = QuestRegistry.getAll().size();

        send(p, "════ Active Quests (" + active.size() + "/" + MAX_ACTIVE + ") ════", Formatting.GOLD);

        if (active.isEmpty()) {
            send(p, "  No active quests. Use /isekraft quest accept <id>", Formatting.GRAY);
        } else {
            for (String qId : active) {
                Quest q = QuestRegistry.get(qId);
                if (q == null) continue;
                int prog = getProgress(p, qId);
                send(p, "  §e" + q.title + " §7[" + prog + "/" + q.goal + "]", Formatting.WHITE);
                send(p, "    " + q.description, Formatting.DARK_GRAY);
            }
        }
        send(p, "  Completed: " + completed + "/" + total + "  |  /isekraft quest list", Formatting.GRAY);
    }

    public static void printQuestList(ServerPlayerEntity p) {
        int playerLevel = PlayerRpgManager.getLevel(p);
        send(p, "════ Quest Board ════", Formatting.GOLD);
        for (Quest q : QuestRegistry.getAll().values()) {
            boolean done   = isCompleted(p, q.id);
            boolean active = isActive(p, q.id);
            boolean locked = playerLevel < q.levelRequired;

            Formatting color = done ? Formatting.DARK_GRAY
                             : active ? Formatting.YELLOW
                             : locked ? Formatting.RED
                             : Formatting.WHITE;
            String prefix = done ? "✓" : active ? "►" : locked ? "✗" : "○";
            send(p, "  " + prefix + " §r[" + q.id + "] §r" + q.title
                 + (locked ? " §c(Lv." + q.levelRequired + ")" : ""), color);
        }
        send(p, "  /isekraft quest accept <id>  to start", Formatting.GRAY);
    }

    // ── NBT HELPERS ───────────────────────────────────────────────────────────

    private static List<String> readList(ServerPlayerEntity p, String key) {
        NbtCompound data = PlayerRpgManager.getData(p);
        if (!data.contains(key)) return new ArrayList<>();
        NbtList list = data.getList(key, 8); // 8 = NbtString
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    private static void addToList(ServerPlayerEntity p, String key, String value) {
        NbtCompound data = PlayerRpgManager.getData(p);
        NbtList list = data.contains(key) ? data.getList(key, 8) : new NbtList();
        list.add(NbtString.of(value));
        data.put(key, list);
        PlayerRpgManager.setData(p, data);
    }

    private static void removeFromList(ServerPlayerEntity p, String key, String value) {
        NbtCompound data = PlayerRpgManager.getData(p);
        if (!data.contains(key)) return;
        NbtList old = data.getList(key, 8);
        NbtList fresh = new NbtList();
        for (int i = 0; i < old.size(); i++) {
            String s = old.getString(i);
            if (!s.equals(value)) fresh.add(NbtString.of(s));
        }
        data.put(key, fresh);
        PlayerRpgManager.setData(p, data);
    }

    private static NbtCompound getProgressMap(ServerPlayerEntity p) {
        NbtCompound data = PlayerRpgManager.getData(p);
        return data.contains(KEY_PROGRESS) ? data.getCompound(KEY_PROGRESS) : new NbtCompound();
    }

    private static void setProgressMap(ServerPlayerEntity p, NbtCompound prog) {
        NbtCompound data = PlayerRpgManager.getData(p);
        data.put(KEY_PROGRESS, prog);
        PlayerRpgManager.setData(p, data);
    }

    private static void setProgress(ServerPlayerEntity p, String questId, int value) {
        NbtCompound prog = getProgressMap(p);
        prog.putInt(questId, value);
        setProgressMap(p, prog);
    }

    // ── UTIL ─────────────────────────────────────────────────────────────────

    private static String goalText(Quest q) {
        return switch (q.type) {
            case KILL    -> "Kill " + q.goal + "x " + q.targetId.replace("minecraft:", "").replace("isekraft:", "").replace("_", " ");
            case FETCH   -> "Collect " + q.goal + "x " + q.targetId.replace("minecraft:", "").replace("isekraft:", "").replace("_", " ");
            case EXPLORE -> "Reach level " + q.goal;
        };
    }

    private static void send(ServerPlayerEntity p, String msg, Formatting f) {
        p.sendMessage(Text.literal(msg).formatted(f), false);
    }

    private static void sendActionBar(ServerPlayerEntity p, String msg) {
        p.sendMessage(Text.literal(msg).formatted(Formatting.YELLOW), true);
    }
}
