package com.isekraft.network;

import com.isekraft.IseKraftMod;
import com.isekraft.rpg.PlayerRpgManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import com.isekraft.screen.NpcDialogueScreen;
import com.isekraft.client.QuestBoardScreen;
import com.isekraft.quest.QuestManager;
import com.isekraft.quest.QuestRegistry;
import java.util.ArrayList;
import java.util.List;

public class ModPackets {

    private static final int MAX_SKILL_ID_LEN = 32;
    private static final int MAX_NPC_NAME_LEN = 64;

    public static final Identifier OPEN_NPC_DIALOGUE  = new Identifier(IseKraftMod.MOD_ID, "open_npc_dialogue");
    public static final Identifier OPEN_QUEST_BOARD   = new Identifier(IseKraftMod.MOD_ID, "open_quest_board");
    public static final Identifier QUEST_ACCEPT        = new Identifier(IseKraftMod.MOD_ID, "quest_accept");
    public static final Identifier QUEST_ABANDON       = new Identifier(IseKraftMod.MOD_ID, "quest_abandon");
    public static final Identifier UNLOCK_SKILL      = new Identifier(IseKraftMod.MOD_ID, "unlock_skill");
    public static final Identifier TOGGLE_SKILL      = new Identifier(IseKraftMod.MOD_ID, "toggle_skill");

    public static void register() {
        // UNLOCK_SKILL — C2S, validated
        ServerPlayNetworking.registerGlobalReceiver(UNLOCK_SKILL,
            (server, player, handler, buf, resp) -> {
                String skillId = buf.readString(MAX_SKILL_ID_LEN);
                if (!isValidSkillId(skillId)) {
                    IseKraftMod.LOGGER.warn("[IseKraft] {} sent invalid unlock skill: {}", player.getName().getString(), skillId);
                    return;
                }
                server.execute(() -> PlayerRpgManager.unlockSkill(player, skillId));
            });

        // TOGGLE_SKILL — C2S, validated
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_SKILL,
            (server, player, handler, buf, resp) -> {
                String skillId = buf.readString(MAX_SKILL_ID_LEN);
                if (!isValidSkillId(skillId)) {
                    IseKraftMod.LOGGER.warn("[IseKraft] {} sent invalid toggle skill: {}", player.getName().getString(), skillId);
                    return;
                }
                server.execute(() -> PlayerRpgManager.toggleSkill(player, skillId));
            });

        ServerPlayNetworking.registerGlobalReceiver(QUEST_ACCEPT,
            (server, player, handler, buf, resp) -> {
                String qId = buf.readString(64);
                if (QuestRegistry.get(qId) == null) return;
                server.execute(() -> QuestManager.acceptQuest(player, qId));
            });

        ServerPlayNetworking.registerGlobalReceiver(QUEST_ABANDON,
            (server, player, handler, buf, resp) -> {
                String qId = buf.readString(64);
                if (QuestRegistry.get(qId) == null) return;
                server.execute(() -> QuestManager.abandonQuest(player, qId));
            });

        IseKraftMod.LOGGER.info("IseKraft server packets registered.");
    }

    public static void registerClientPackets() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
            .registerGlobalReceiver(OPEN_QUEST_BOARD, (client, handler, buf, sender) -> {
                String npcName   = buf.readString(64);
                int playerLevel  = buf.readInt();
                int activeCount  = buf.readInt();
                List<String> active = new ArrayList<>();
                for (int i = 0; i < activeCount; i++) active.add(buf.readString(64));
                int doneCount = buf.readInt();
                List<String> done = new ArrayList<>();
                for (int i = 0; i < doneCount; i++) done.add(buf.readString(64));
                client.execute(() ->
                    client.setScreen(new QuestBoardScreen(npcName, playerLevel, active, done)));
            });

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
            .registerGlobalReceiver(OPEN_NPC_DIALOGUE, (client, handler, buf, sender) -> {
                String npcName    = buf.readString(MAX_NPC_NAME_LEN);
                int    playerLevel = buf.readInt();
                client.execute(() ->
                    client.setScreen(new NpcDialogueScreen(npcName, playerLevel)));
            });
    }

    public static void sendQuestBoardPacket(ServerPlayerEntity player, String npcName) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(npcName, 64);
        buf.writeInt(QuestManager.getActiveIds(player).size() > 0
            ? com.isekraft.rpg.PlayerRpgManager.getLevel(player) : 1);
        buf.writeInt(com.isekraft.rpg.PlayerRpgManager.getLevel(player));
        List<String> active = QuestManager.getActiveIds(player);
        buf.writeInt(active.size());
        for (String s : active) buf.writeString(s, 64);
        List<String> completed = QuestManager.getCompletedIds(player);
        buf.writeInt(completed.size());
        for (String s : completed) buf.writeString(s, 64);
        ServerPlayNetworking.send(player, OPEN_QUEST_BOARD, buf);
    }

    public static void sendNpcDialoguePacket(ServerPlayerEntity player, String npcName, int level) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(npcName, MAX_NPC_NAME_LEN);
        buf.writeInt(level);
        ServerPlayNetworking.send(player, OPEN_NPC_DIALOGUE, buf);
    }

    public static void sendUnlockSkill(String skillId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(skillId, MAX_SKILL_ID_LEN);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(UNLOCK_SKILL, buf);
    }

    public static void sendToggleSkill(String skillId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(skillId, MAX_SKILL_ID_LEN);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(TOGGLE_SKILL, buf);
    }

    public static void sendQuestAccept(String qId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(qId, 64);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(QUEST_ACCEPT, buf);
    }

    public static void sendQuestAbandon(String qId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(qId, 64);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(QUEST_ABANDON, buf);
    }

    private static boolean isValidSkillId(String id) {
        for (String s : PlayerRpgManager.ALL_SKILLS) if (s.equals(id)) return true;
        return false;
    }
}
