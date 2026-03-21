package com.isekraft;

import com.isekraft.client.CharacterScreen;
import com.isekraft.client.IseKraftHudState;
import com.isekraft.client.RpgHudRenderer;
import com.isekraft.client.SkillTreeScreen;
import com.isekraft.entity.ModEntities;
import com.isekraft.entity.client.*;
import com.isekraft.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.entity.ArrowEntityRenderer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class IseKraftClient implements ClientModInitializer {

    private static KeyBinding characterKey;
    private static KeyBinding skillTreeKey;
    private static KeyBinding buffPanelKey;

    @Override
    public void onInitializeClient() {
        // Entity renderers
        EntityRendererRegistry.register(ModEntities.SPIRIT_BEAST,   SpiritBeastRenderer::new);
        EntityRendererRegistry.register(ModEntities.GOBLIN_KING,    GoblinKingRenderer::new);
        EntityRendererRegistry.register(ModEntities.DARK_KNIGHT,    DarkKnightRenderer::new);
        EntityRendererRegistry.register(ModEntities.FOREST_WOLF,    ForestWolfRenderer::new);
        EntityRendererRegistry.register(ModEntities.ISEKAI_NPC,     IsekaiNpcRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHADOW_DEMON,   ShadowDemonRenderer::new);
        EntityRendererRegistry.register(ModEntities.WITCH_COVEN,    WitchCovenRenderer::new);
        EntityRendererRegistry.register(ModEntities.OVERLORD_GUARD, OverlordGuardRenderer::new);

        // Projectile renderers — REQUIRED or render thread crashes when these entities exist
        EntityRendererRegistry.register(ModEntities.SHURIKEN_PROJECTILE, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.BOOMERANG_PROJECTILE, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.LIGHTNING_ARROW,      ArrowEntityRenderer::new);

        ModPackets.registerClientPackets();
        RpgHudRenderer.register();

        // H — Character screen
        characterKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.isekraft.character",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "IseKraft RPG"
        ));

        // K — Skill tree
        skillTreeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.isekraft.skilltree",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "IseKraft RPG"
        ));

        // B — Toggle buff panel
        buffPanelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.isekraft.buffpanel",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "IseKraft RPG"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (characterKey.wasPressed()) {
                if (client.currentScreen == null)
                    client.setScreen(new CharacterScreen());
                else if (client.currentScreen instanceof CharacterScreen)
                    client.setScreen(null);
            }
            while (skillTreeKey.wasPressed()) {
                if (client.currentScreen == null)
                    client.setScreen(new SkillTreeScreen());
                else if (client.currentScreen instanceof SkillTreeScreen)
                    client.setScreen(null);
            }
            while (buffPanelKey.wasPressed()) {
                // FIX: toggle buff panel visibility
                IseKraftHudState.showBuffPanel = !IseKraftHudState.showBuffPanel;
            }
        });
    }
}
