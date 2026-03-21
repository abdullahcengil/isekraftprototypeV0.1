package com.isekraft.screen;

import com.isekraft.rpg.PlayerRpgManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * NPC Dialogue Screen.
 * Purple-bordered panel showing NPC name, player level, and contextual dialogue.
 * Opened via network packet from the server when a player right-clicks an NPC.
 */
@Environment(EnvType.CLIENT)
public class NpcDialogueScreen extends Screen {

    private static final int W = 300;
    private static final int H = 150;

    private final String npcName;
    private final int    playerLevel;

    public NpcDialogueScreen(String npcName, int playerLevel) {
        super(Text.literal(npcName));
        this.npcName = npcName;
        this.playerLevel = playerLevel;
    }

    @Override
    protected void init() {
        int x = (width - W) / 2;
        int y = (height - H) / 2;
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Farewell"),
            btn -> close()
        ).dimensions(x + W / 2 - 40, y + H - 26, 80, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackgroundTexture(ctx);

        int x = (width - W) / 2;
        int y = (height - H) / 2;

        // Panel background
        ctx.fill(x, y, x + W, y + H, 0xDD0D0820);
        // Purple border (2px)
        ctx.fill(x,     y,     x + W, y + 2,     0xFF8B5CF6);
        ctx.fill(x,     y + H - 2, x + W, y + H, 0xFF8B5CF6);
        ctx.fill(x,     y,     x + 2, y + H,     0xFF8B5CF6);
        ctx.fill(x + W - 2, y, x + W, y + H,     0xFF8B5CF6);

        // Portrait box
        ctx.fill(x + 8, y + 8, x + 52, y + 52, 0xFF3B1A6E);
        ctx.fill(x + 9, y + 9, x + 51, y + 51, 0xFF5B2A9E);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("✦").formatted(Formatting.GOLD),
            x + 30, y + 25, 0xFFFFFF);

        // NPC name
        ctx.drawTextWithShadow(textRenderer,
            Text.literal(npcName).formatted(Formatting.AQUA, Formatting.BOLD),
            x + 60, y + 10, 0xFFFFFF);

        // Player level + title
        String title = PlayerRpgManager.getLevelTitle(playerLevel);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("Level " + playerLevel + "  —  " + title).formatted(Formatting.YELLOW),
            x + 60, y + 24, 0xFFFFFF);

        // Divider
        ctx.fill(x + 8, y + 56, x + W - 8, y + 57, 0xFF8B5CF6);

        // Dialogue lines
        for (int i = 0; i < dialogueLines().length; i++) {
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(dialogueLines()[i]).formatted(Formatting.GRAY),
                x + 12, y + 64 + i * 13, 0xFFFFFF);
        }

        super.render(ctx, mx, my, delta);
    }

    private String[] dialogueLines() {
        if (playerLevel < 5)  return new String[]{"You are new to this world.", "Seek the northern ruins when you are ready..."};
        if (playerLevel < 20) return new String[]{"Your power grows rapidly.", "The Dark Knights patrol the forest at night."};
        if (playerLevel < 50) return new String[]{"The Goblin King trembles at your name.", "Use the Soul Altar to enhance your equipment."};
        if (playerLevel < 80) return new String[]{"You carry the weight of a true hero.", "Ancient dragons stir in the eastern mountains."};
        return new String[]{"You have transcended mortal limits.", "The title of Isekai Overlord awaits you..."};
    }

    @Override
    public boolean shouldPause() { return false; }
}
