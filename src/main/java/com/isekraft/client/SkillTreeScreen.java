package com.isekraft.client;

import com.isekraft.network.ModPackets;
import com.isekraft.rpg.PlayerRpgManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Skill Tree Screen — press K.
 *
 * v3: Two-layer system:
 *   UNLOCK button — spends a skill point, permanently learns skill (gray → visible)
 *   ON/OFF toggle — activates/deactivates a learned skill (max 5 active at once)
 *
 * Visual states:
 *   LOCKED     — prereq not met or wrong path: dark card, ✗ icon
 *   UNLOCKABLE — meets prereq + has points: green tint, ★ icon, UNLOCK button
 *   LEARNED    — unlocked but inactive: white, ○ icon, [ON] button
 *   ACTIVE     — unlocked AND active: bright, ✓ icon, [OFF] button
 */
@Environment(EnvType.CLIENT)
public class SkillTreeScreen extends Screen {

    private static final int W = 460;
    private static final int H = 390;
    private static final int COL_W     = 140;
    private static final int CARD_H    = 52;
    private static final int HEADER_H  = 74;

    private static final int RED    = 0xFFCC0000;
    private static final int WHITE  = 0xFFEEEEEE;
    private static final int[] PATH_COLOR = { 0, 0xFFCC3300, 0xFF7722FF, 0xFF22AA44 };
    private static final String[] PATH_LABEL = { "", "⚔ WARRIOR", "✦ MAGE", "🏹 RANGER" };

    private static final Object[][] SKILL_DEF = {
        new Object[]{"warrior_1","Iron Will",      1, 0},
        new Object[]{"warrior_2","Veteran's Hide", 1, 1},
        new Object[]{"warrior_3","War Mastery",    1, 2},
        new Object[]{"warrior_4","Battle Cry",     1, 3},
        new Object[]{"warrior_5","Undying Rage",   1, 4},
        new Object[]{"mage_1",   "Arcane Focus",   2, 0},
        new Object[]{"mage_2",   "Soul Surge",     2, 1},
        new Object[]{"mage_3",   "Void Mastery",   2, 2},
        new Object[]{"mage_4",   "Mana Shield",    2, 3},
        new Object[]{"mage_5",   "Arcane Barrage", 2, 4},
        new Object[]{"ranger_1", "Swift Feet",     3, 0},
        new Object[]{"ranger_2", "Predator",       3, 1},
        new Object[]{"ranger_3", "Wind Walker",    3, 2},
        new Object[]{"ranger_4", "Pack Leader",    3, 3},
        new Object[]{"ranger_5", "Eagle Eye",      3, 4},
    };

    public SkillTreeScreen() { super(Text.literal("Skill Tree")); }

    @Override
    protected void init() {
        int px = (width - W) / 2;
        int py = (height - H) / 2;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        int available   = PlayerRpgManager.getAvailableSkillPoints(player);
        int chosenPath  = PlayerRpgManager.getChosenPath(player);
        int activeCount = PlayerRpgManager.getActiveCount(player);

        for (Object[] sk : SKILL_DEF) {
            String id   = (String) sk[0];
            int path    = (int) sk[2];
            int row     = (int) sk[3];
            int col     = path - 1;
            int cardX   = px + 8 + col * (COL_W + 6);
            int cardY   = py + HEADER_H + row * (CARD_H + 4);
            int btnY    = cardY + CARD_H - 16;

            boolean unlocked    = PlayerRpgManager.isSkillUnlocked(player, id);
            boolean active      = PlayerRpgManager.isSkillActive(player, id);
            boolean prereqMet   = PlayerRpgManager.isPrerequisiteMet(player, id);
            boolean pathOk      = (chosenPath == PlayerRpgManager.PATH_NONE || chosenPath == path);
            boolean canUnlock   = !unlocked && prereqMet && pathOk && available > 0;
            boolean canActivate = unlocked && !active && activeCount < PlayerRpgManager.MAX_ACTIVE;

            String capturedId = id;

            if (canUnlock) {
                addDrawableChild(ButtonWidget.builder(
                    Text.literal("UNLOCK").formatted(Formatting.YELLOW),
                    btn -> {
                        ModPackets.sendUnlockSkill(capturedId);
                        MinecraftClient.getInstance().execute(() -> { clearChildren(); init(); });
                    }
                ).dimensions(cardX + 4, btnY, 58, 13).build());
            } else if (unlocked) {
                // Toggle ON/OFF
                addDrawableChild(ButtonWidget.builder(
                    active ? Text.literal(" OFF").formatted(Formatting.RED)
                           : Text.literal(" ON ").formatted(Formatting.GREEN),
                    btn -> {
                        ModPackets.sendToggleSkill(capturedId);
                        MinecraftClient.getInstance().execute(() -> { clearChildren(); init(); });
                    }
                ).dimensions(cardX + 4, btnY, 36, 13).build());
            }
        }

        // Close
        addDrawableChild(ButtonWidget.builder(
            Text.literal("✕ CLOSE").formatted(Formatting.RED),
            b -> close()
        ).dimensions(px + W - 88, py + H - 24, 80, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xAA000000);
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) { super.render(ctx, mx, my, delta); return; }

        int level       = PlayerRpgManager.getLevel(player);
        int available   = PlayerRpgManager.getAvailableSkillPoints(player);
        int spent       = PlayerRpgManager.getSpentSkillPoints(player);
        int total       = PlayerRpgManager.getTotalSkillPoints(player);
        int activeCount = PlayerRpgManager.getActiveCount(player);
        int chosenPath  = PlayerRpgManager.getChosenPath(player);

        int px = (width - W) / 2;
        int py = (height - H) / 2;

        drawStoneBg(ctx, px, py, W, H);
        border(ctx, px, py, W, H, RED);

        // Header
        ctx.fill(px+2, py+2, px+W-2, py+26, 0xEE100808);
        ctx.fill(px+2, py+25, px+W-2, py+27, RED);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("⚔  SKILL TREE  ⚔").formatted(Formatting.RED, Formatting.BOLD),
            px+W/2, py+9, WHITE);

        // Points + active slots
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("Points: ")
                .append(Text.literal(available > 0 ? available + " available" : "none").formatted(available > 0 ? Formatting.GREEN : Formatting.DARK_GRAY))
                .append(Text.literal("   Spent: " + spent + "/" + total).formatted(Formatting.GRAY))
                .append(Text.literal("   Active: " + activeCount + "/" + PlayerRpgManager.MAX_ACTIVE)
                    .formatted(activeCount >= PlayerRpgManager.MAX_ACTIVE ? Formatting.RED : Formatting.AQUA)),
            px+W/2, py+31, WHITE);

        // Path indicator
        String pathText = chosenPath == 0 ? "Choose a path by unlocking any skill"
            : "Path: " + PATH_LABEL[chosenPath] + "  ·  /isekraft resetskills to change";
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(pathText).formatted(chosenPath == 0 ? Formatting.GRAY : Formatting.GOLD),
            px+W/2, py+43, WHITE);
        ctx.fill(px+8, py+54, px+W-8, py+55, RED);

        // Column headers
        for (int col = 0; col < 3; col++) {
            int path  = col + 1;
            int colX  = px + 8 + col * (COL_W + 6);
            boolean locked = chosenPath != 0 && chosenPath != path;
            ctx.fill(colX, py+57, colX+COL_W+4, py+68, locked ? 0xEE080608 : 0xEE160A00);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(PATH_LABEL[path]).formatted(locked ? Formatting.DARK_GRAY : Formatting.BOLD),
                colX+(COL_W+4)/2, py+59,
                locked ? 0xFF444444 : PATH_COLOR[path]);
        }

        // Skill cards
        for (Object[] sk : SKILL_DEF) {
            String id      = (String) sk[0];
            String name    = (String) sk[1];
            int path       = (int) sk[2];
            int row        = (int) sk[3];
            int col        = path - 1;
            int cardX      = px + 8 + col * (COL_W + 6);
            int cardY      = py + HEADER_H + row * (CARD_H + 4);

            boolean unlocked   = PlayerRpgManager.isSkillUnlocked(player, id);
            boolean active     = PlayerRpgManager.isSkillActive(player, id);
            boolean prereqMet  = PlayerRpgManager.isPrerequisiteMet(player, id);
            boolean pathOk     = (chosenPath == PlayerRpgManager.PATH_NONE || chosenPath == path);
            boolean canUnlock  = !unlocked && prereqMet && pathOk && available > 0;

            // Card background
            int bg = active   ? 0xEE180800 :
                     unlocked ? 0xEE0A0A0A :
                     canUnlock? 0xEE0A1200 : 0xEE080808;
            int bdr = active   ? PATH_COLOR[path] :
                      unlocked ? 0xFF554400 :
                      canUnlock? 0xFF224400 : 0xFF330000;

            ctx.fill(cardX, cardY, cardX+COL_W+2, cardY+CARD_H,    bg);
            ctx.fill(cardX, cardY, cardX+COL_W+2, cardY+1,         bdr);
            ctx.fill(cardX, cardY+CARD_H-1, cardX+COL_W+2, cardY+CARD_H, bdr);
            ctx.fill(cardX, cardY, cardX+1, cardY+CARD_H,           bdr);
            ctx.fill(cardX+COL_W+1, cardY, cardX+COL_W+2, cardY+CARD_H, bdr);

            // Connector to next card
            if (row < 4) {
                ctx.fill(cardX+COL_W/2, cardY+CARD_H,
                    cardX+COL_W/2+1, cardY+CARD_H+4, bdr);
            }

            // Status icon
            String icon  = active   ? "✓" : unlocked ? "○" : canUnlock ? "★" : "✗";
            int iconClr  = active   ? 0xFF44FF44 : unlocked ? 0xFFCCAA00 :
                           canUnlock? 0xFFFFCC00 : 0xFF444444;
            ctx.drawTextWithShadow(textRenderer, Text.literal(icon), cardX+COL_W-8, cardY+4, iconClr);

            // Name
            Formatting nf = active ? Formatting.WHITE : unlocked ? Formatting.GRAY :
                            canUnlock ? Formatting.WHITE : Formatting.DARK_GRAY;
            ctx.drawTextWithShadow(textRenderer, Text.literal(name).formatted(nf), cardX+4, cardY+4, WHITE);

            // Desc
            String desc = PlayerRpgManager.getSkillDesc(id);
            if (desc.length() > 25) desc = desc.substring(0, 23) + "..";
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(desc).formatted(active ? Formatting.GRAY : Formatting.DARK_GRAY),
                cardX+4, cardY+15, WHITE);

            // Status label
            String status = active ? "ACTIVE" : unlocked ? "learned — toggle ON" :
                            canUnlock ? "Cost: 1 pt" : !pathOk ? "wrong path" :
                            !prereqMet ? "need prev" : "no points";
            Formatting sf = active ? Formatting.GREEN : unlocked ? Formatting.YELLOW :
                            canUnlock ? Formatting.YELLOW : Formatting.DARK_GRAY;
            ctx.drawTextWithShadow(textRenderer, Text.literal(status).formatted(sf), cardX+4, cardY+27, WHITE);
        }

        // Footer
        ctx.fill(px+8, py+H-28, px+W-8, py+H-27, RED);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("[K] Close  ·  Max " + PlayerRpgManager.MAX_ACTIVE + " active  ·  UNLOCK = spend point  ·  ON/OFF = free")
                .formatted(Formatting.DARK_GRAY),
            px+W/2, py+H-20, WHITE);

        super.render(ctx, mx, my, delta);
    }

    private void drawStoneBg(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x+w, y+h, 0xEE0C0A09);
        for (int ty=0;ty<h;ty+=16) for (int tx=0;tx<w;tx+=16) {
            int off=((tx/16+ty/16)%3);
            ctx.fill(x+tx,y+ty,Math.min(x+tx+15,x+w),Math.min(y+ty+15,y+h),
                0xEE000000|((12+off)<<16)|((10+off)<<8)|(8+off));
        }
        for (int ty=0;ty<h;ty+=16) ctx.fill(x,y+ty,x+w,y+ty+1,0xFF060404);
        for (int tx=0;tx<w;tx+=16) ctx.fill(x+tx,y,x+tx+1,y+h,0xFF060404);
    }
    private void border(DrawContext ctx,int x,int y,int w,int h,int c){
        ctx.fill(x,y,x+w,y+2,c);ctx.fill(x,y+h-2,x+w,y+h,c);
        ctx.fill(x,y,x+2,y+h,c);ctx.fill(x+w-2,y,x+w,y+h,c);
    }

    @Override public boolean keyPressed(int key, int scan, int mod) {
        if (key == 75) { close(); return true; }
        return super.keyPressed(key, scan, mod);
    }
    @Override public boolean shouldPause() { return false; }
}
