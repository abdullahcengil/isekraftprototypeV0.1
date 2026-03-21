package com.isekraft.network;

import com.isekraft.IseKraftMod;
import com.isekraft.rpg.PlayerRpgManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import com.isekraft.screen.NpcDialogueScreen;

public class ModPackets {

    private static final int MAX_SKILL_ID_LEN = 32;
    private static final int MAX_NPC_NAME_LEN = 64;

    public static final Identifier OPEN_NPC_DIALOGUE = new Identifier(IseKraftMod.MOD_ID, "open_npc_dialogue");
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

        IseKraftMod.LOGGER.info("IseKraft server packets registered.");
    }

    public static void registerClientPackets() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
            .registerGlobalReceiver(OPEN_NPC_DIALOGUE, (client, handler, buf, sender) -> {
                String npcName    = buf.readString(MAX_NPC_NAME_LEN);
                int    playerLevel = buf.readInt();
                client.execute(() ->
                    client.setScreen(new NpcDialogueScreen(npcName, playerLevel)));
            });
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

    private static boolean isValidSkillId(String id) {
        for (String s : PlayerRpgManager.ALL_SKILLS) if (s.equals(id)) return true;
        return false;
    }
}
