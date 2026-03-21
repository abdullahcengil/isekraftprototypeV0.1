package com.isekraft.client;

import com.isekraft.rpg.PlayerRpgManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * IseKraft HUD — compact, two panels:
 *
 * Panel A (top-left, always visible unless hud hidden):
 *   Tiny level badge | title | XP bar | kills | key hints
 *   Width: 120px, Height: 46px — much smaller than before
 *
 * Panel B (bottom-right, toggle with B):
 *   Active skill buffs + vanilla status effects from IseKraft
 *   Controlled by IseKraftHudState.showBuffPanel static flag
 */
public class RpgHudRenderer {

    public static void register() {
        HudRenderCallback.EVENT.register(RpgHudRenderer::render);
    }

    private static void render(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.options.hudHidden || mc.currentScreen != null) return;

        renderCompactRpgPanel(ctx, mc, player);
        if (IseKraftHudState.showBuffPanel) renderBuffPanel(ctx, mc, player);
    }

    // ── COMPACT RPG PANEL (top-left) ─────────────────────────────────────────
    private static void renderCompactRpgPanel(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity player) {
        int level   = PlayerRpgManager.getLevel(player);
        int xp      = PlayerRpgManager.getXp(player);
        int needed  = PlayerRpgManager.getXpRequired(level);
        int kills   = PlayerRpgManager.getTotalKills(player);
        String title = PlayerRpgManager.getLevelTitle(level);
        float pct   = Math.min(1f, (float) xp / Math.max(1, needed));
        int sp      = PlayerRpgManager.getAvailableSkillPoints(player);

        // Layout
        int x = 5, y = 5;
        int W = 118;   // compact width
        int H = 44;    // compact height

        // Background — semi-transparent dark
        ctx.fill(x-2, y-2, x+W+2, y+H+2, 0xBB080606);
        // Left accent
        ctx.fill(x-2, y-2, x, y+H+2, 0xFFAA0000);
        // Top border line only
        ctx.fill(x, y-2, x+W+2, y-1, 0xFF660000);

        // Level badge (small)
        ctx.fill(x, y, x+22, y+14, 0xDD1A0000);
        ctx.fill(x, y, x+22, y+1, 0xFFCC0000);
        ctx.fill(x+21, y, x+22, y+14, 0xFFCC0000);
        ctx.drawCenteredTextWithShadow(mc.textRenderer,
            Text.literal(String.valueOf(level)).formatted(Formatting.BOLD),
            x+11, y+3, 0xFFFF3300);

        // Title (right of badge)
        ctx.drawTextWithShadow(mc.textRenderer,
            Text.literal(title).formatted(Formatting.RED),
            x+26, y+3, 0xFFFFFFFF);

        // Kills (small, right-aligned)
        ctx.drawTextWithShadow(mc.textRenderer,
            Text.literal("☠" + kills).formatted(Formatting.DARK_GRAY),
            x + W - 30, y+3, 0xFFFFFFFF);

        // XP bar — thin (4px)
        ctx.fill(x, y+17, x+W, y+21, 0xFF0D0808);
        int fw = (int)(W * pct);
        for (int i = 0; i < fw; i++) {
            float t = (float) i / Math.max(1, W);
            int r = (int)(70 + t*160);
            ctx.fill(x+i, y+17, x+i+1, y+21, 0xFF000000|(r<<16));
        }
        ctx.fill(x, y+17, x+fw, y+18, 0x55FF3300); // shine
        ctx.fill(x, y+16, x+W, y+17, 0xFF440000);  // border top
        ctx.fill(x, y+21, x+W, y+22, 0xFF440000);  // border bot

        // XP numbers — tiny
        ctx.drawTextWithShadow(mc.textRenderer,
            Text.literal(xp + "/" + needed).formatted(Formatting.DARK_RED),
            x, y+24, 0xFFFFFFFF);

        // Skill points indicator (if any)
        if (sp > 0) {
            ctx.drawTextWithShadow(mc.textRenderer,
                Text.literal("★" + sp).formatted(Formatting.GOLD),
                x + W - 18, y+24, 0xFFFFFFFF);
        }

        // Key hints — very small, bottom row
        ctx.drawTextWithShadow(mc.textRenderer,
            Text.literal("[H] [K] [B]").formatted(Formatting.DARK_GRAY),
            x, y+35, 0xFFFFFFFF);
    }

    // ── BUFF PANEL (bottom-right, toggleable) ────────────────────────────────
    private static void renderBuffPanel(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity player) {
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        // Gather active IseKraft skill buffs
        List<String> buffLines = new ArrayList<>();
        for (String id : PlayerRpgManager.ALL_SKILLS) {
            if (PlayerRpgManager.isSkillActive(player, id)) {
                buffLines.add("✦ " + PlayerRpgManager.getSkillDisplayName(id));
            }
        }

        // Also show relevant vanilla effects (from our mod)
        Collection<StatusEffectInstance> effects = player.getStatusEffects();
        for (StatusEffectInstance e : effects) {
            if (e.isAmbient()) continue; // skip beacon effects
            String name = e.getEffectType().getName().getString();
            int dur = e.getDuration() / 20; // seconds
            if (dur > 9999) continue; // skip permanent ones (not useful to show)
            String durStr = dur > 60 ? (dur/60) + "m" : dur + "s";
            buffLines.add("» " + name + " [" + durStr + "]");
        }

        if (buffLines.isEmpty()) {
            buffLines.add("No active buffs");
        }

        int panelW = 140;
        int lineH  = 10;
        int panelH = buffLines.size() * lineH + 18;
        int px = screenW - panelW - 5;
        int py = screenH - panelH - 50; // above hotbar

        // Background
        ctx.fill(px-2, py-2, px+panelW+2, py+panelH+2, 0xBB080606);
        ctx.fill(px-2, py-2, px, py+panelH+2, 0xFF8833FF); // purple accent
        ctx.fill(px-2, py-2, px+panelW+2, py-1, 0xFF551188);

        // Header
        ctx.drawTextWithShadow(mc.textRenderer,
            Text.literal("✦ Active Buffs [B]").formatted(Formatting.LIGHT_PURPLE),
            px+2, py+2, 0xFFFFFFFF);
        ctx.fill(px, py+12, px+panelW, py+13, 0xFF551188);

        // Buff lines
        for (int i = 0; i < buffLines.size(); i++) {
            String line = buffLines.get(i);
            Formatting color = line.startsWith("✦") ? Formatting.AQUA :
                               line.startsWith("»") ? Formatting.YELLOW : Formatting.DARK_GRAY;
            ctx.drawTextWithShadow(mc.textRenderer,
                Text.literal(line).formatted(color),
                px+2, py+15 + i*lineH, 0xFFFFFFFF);
        }
    }
}
