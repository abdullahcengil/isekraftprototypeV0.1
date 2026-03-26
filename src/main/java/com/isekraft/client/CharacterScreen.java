package com.isekraft.client;

import com.isekraft.rpg.PlayerRpgManager;
import com.isekraft.equipment.EquipmentManager;
import com.isekraft.equipment.EquipSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
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
 * Character Screen — press H.
 * Dark edgy aesthetic: mossy cobblestone bg, red accents, Akatsuki-inspired.
 */
@Environment(EnvType.CLIENT)
public class CharacterScreen extends Screen {

    private static final int W = 370;
    private static final int H = 290;

    // Red accent color used throughout
    private static final int RED    = 0xFFCC0000;
    private static final int RED2   = 0xFFFF2200;
    private static final int DARK   = 0xEE0A0808;
    private static final int DARKER = 0xDD060404;
    private static final int STONE  = 0xFF1A1614;
    private static final int GOLD   = 0xFFDAA520;
    private static final int WHITE  = 0xFFEEEEEE;
    private static final int GRAY   = 0xFF888888;
    private static final int GREEN  = 0xFF44CC44;

    public CharacterScreen() { super(Text.literal("Character")); }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        // Close button — dark red style
        addDrawableChild(ButtonWidget.builder(
            Text.literal("✕  CLOSE").formatted(Formatting.RED),
            b -> close()
        ).dimensions(px + W - 95, py + H - 24, 87, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Dark overlay
        ctx.fill(0, 0, width, height, 0xBB000000);

        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        int level  = PlayerRpgManager.getLevel(player);
        int xp     = PlayerRpgManager.getXp(player);
        int needed = PlayerRpgManager.getXpRequired(level);
        int kills  = PlayerRpgManager.getTotalKills(player);
        String title = PlayerRpgManager.getLevelTitle(level);
        float pct = Math.min(1f, (float)xp / Math.max(1, needed));

        int px = (width - W) / 2, py = (height - H) / 2;

        // ── MAIN PANEL ────────────────────────────────────────────────────────
        // Mossy cobblestone-ish background: dark with subtle texture
        drawStoneBg(ctx, px, py, W, H);

        // Red border — double line
        ctx.fill(px,   py,   px+W, py+2,   RED);
        ctx.fill(px,   py+H-2, px+W, py+H, RED);
        ctx.fill(px,   py,   px+2, py+H,   RED);
        ctx.fill(px+W-2, py, px+W, py+H,   RED);
        // Inner border slightly lighter
        ctx.fill(px+3, py+3, px+W-3, py+4,   0xFF660000);
        ctx.fill(px+3, py+H-4, px+W-3, py+H-3, 0xFF660000);

        // ── HEADER ────────────────────────────────────────────────────────────
        ctx.fill(px+2, py+2, px+W-2, py+24, 0xEE100808);
        // Red accent bar under header
        ctx.fill(px+2, py+23, px+W-2, py+25, RED);

        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("⚔  CHARACTER  ⚔").formatted(Formatting.RED, Formatting.BOLD),
            px+W/2, py+8, WHITE);

        // ── LEFT: AVATAR BLOCK ───────────────────────────────────────────────
        int avX = px+10, avY = py+30;
        drawPanel(ctx, avX, avY, 80, 100);

        // Big level number
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(String.valueOf(level)).formatted(Formatting.BOLD),
            avX+40, avY+8, 0xFFFF4400);

        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("LVL").formatted(Formatting.RED),
            avX+40, avY+22, WHITE);

        // Divider
        ctx.fill(avX+5, avY+34, avX+75, avY+35, RED);

        // Player name
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(player.getName().getString()).formatted(Formatting.WHITE),
            avX+40, avY+40, WHITE);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal(title).formatted(Formatting.RED),
            avX+40, avY+53, WHITE);

        // Kills
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("☠ " + kills).formatted(Formatting.GRAY),
            avX+40, avY+70, WHITE);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("kills").formatted(Formatting.DARK_GRAY),
            avX+40, avY+82, WHITE);

        // ── RIGHT: STATS ──────────────────────────────────────────────────────
        int stX = px+100, stY = py+30;
        drawPanel(ctx, stX, stY, 260, 100);

        ctx.drawTextWithShadow(textRenderer,
            Text.literal("STATS").formatted(Formatting.RED, Formatting.BOLD),
            stX+8, stY+6, WHITE);
        ctx.fill(stX+5, stY+18, stX+255, stY+19, 0xFF440000);

        int hp = (int)(20 + Math.min(level-1, 40));
        String[][] stats = {
            {"❤  Max HP",    hp + " HP"},
            {"⚡  Speed",     "+" + (level/10*2) + "%"},
            {"⚔  Damage",    "+" + (level/2) + " bonus"},
            {"☠  Kills",     String.valueOf(kills)},
        };

        for (int i = 0; i < stats.length; i++) {
            int sy = stY + 24 + i*16;
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(stats[i][0]).formatted(Formatting.GRAY),
                stX+8, sy, WHITE);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(stats[i][1]).formatted(Formatting.WHITE),
                stX+170, sy, WHITE);
        }

        // ── XP BAR ────────────────────────────────────────────────────────────
        int barY = py+138;
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("EXPERIENCE").formatted(Formatting.RED),
            px+10, barY, WHITE);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal(xp + " / " + needed).formatted(Formatting.DARK_GRAY),
            px+W-90, barY, WHITE);

        // Track
        ctx.fill(px+10, barY+13, px+W-10, barY+23, 0xFF0D0808);
        ctx.fill(px+11, barY+14, px+W-11, barY+22, 0xFF1A0A0A);
        // Fill — dark red gradient
        int fillW = (int)((W-22)*pct);
        for (int i = 0; i < fillW; i++) {
            float t = (float)i/Math.max(1,fillW);
            int r = (int)(100 + t*155);
            int g = (int)(t*20);
            ctx.fill(px+11+i, barY+14, px+12+i, barY+22, 0xFF000000|(r<<16)|(g<<8));
        }
        // Shine
        ctx.fill(px+11, barY+14, px+11+fillW, barY+15, 0x88FF4400);
        // Border
        ctx.fill(px+10, barY+13, px+W-10, barY+14, RED);
        ctx.fill(px+10, barY+22, px+W-10, barY+23, RED);
        ctx.fill(px+10, barY+13, px+11,   barY+23, RED);
        ctx.fill(px+W-11,barY+13,px+W-10, barY+23, RED);

        // ── MILESTONE TREE ────────────────────────────────────────────────────
        int milY = py+168;
        ctx.fill(px+10, milY, px+W-10, milY+1, RED);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("POWER MILESTONES").formatted(Formatting.RED, Formatting.BOLD),
            px+10, milY+5, WHITE);

        int[] lvls = {5,10,15,20,25,30,40,50,75,100};
        String[] rw = {"Rune Sword","Staff","Soul Bow","Spirit x3","Teleport","Berserker","Crystals","Cores x3","Cores x8","FINAL"};

        for (int i = 0; i < lvls.length; i++) {
            int col = i % 5, row = i / 5;
            int bx = px+10 + col*70, by = milY+18 + row*38;
            boolean done = level >= lvls[i];

            // Milestone box
            ctx.fill(bx, by, bx+64, by+32, done ? 0xEE1A0000 : 0xEE0D0808);
            // Border — red if done, dark if not
            int borderCol = done ? RED : 0xFF330000;
            ctx.fill(bx,    by,    bx+64, by+1,    borderCol);
            ctx.fill(bx,    by+31, bx+64, by+32,   borderCol);
            ctx.fill(bx,    by,    bx+1,  by+32,   borderCol);
            ctx.fill(bx+63, by,    bx+64, by+32,   borderCol);

            // Level badge
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Lv."+lvls[i]).formatted(done ? Formatting.RED : Formatting.DARK_GRAY),
                bx+32, by+5, WHITE);

            // Reward name (truncate)
            String r = rw[i].length() > 8 ? rw[i].substring(0,8) : rw[i];
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(r).formatted(done ? Formatting.WHITE : Formatting.DARK_GRAY),
                bx+32, by+18, WHITE);

            // Checkmark
            if (done) {
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("✓").formatted(Formatting.GREEN),
                    bx+54, by+5, WHITE);
            }
        }

        // ── ACHIEVEMENTS ──────────────────────────────────────────────────────
        boolean witchDone = com.isekraft.rpg.PlayerRpgManager.getData(player).getBoolean("WitchHunterEarned");
        boolean demonDone = com.isekraft.rpg.PlayerRpgManager.getData(player).getBoolean("DemonLordDone");
        int achY = py + H - 38;
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("☽ Witch Hunter").formatted(witchDone ? Formatting.GOLD : Formatting.DARK_GRAY),
            px + 10, achY, WHITE);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal(witchDone ? "✓" : "✗").formatted(witchDone ? Formatting.GREEN : Formatting.DARK_GRAY),
            px + 95, achY, WHITE);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("☠ Demon Slayer").formatted(demonDone ? Formatting.GOLD : Formatting.DARK_GRAY),
            px + 120, achY, WHITE);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal(demonDone ? "✓" : "✗").formatted(demonDone ? Formatting.GREEN : Formatting.DARK_GRAY),
            px + 205, achY, WHITE);

        // ── EQUIPMENT SLOTS ───────────────────────────────────────────────────
        int eqY = py + H - 60;
        ctx.fill(px+10, eqY-2, px+W-10, eqY-1, RED);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("EQUIPMENT").formatted(Formatting.RED, Formatting.BOLD),
            px+10, eqY+2, WHITE);

        // Only fetch from server if we have a ServerPlayerEntity (integrated server / LAN)
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.getServer() != null && player instanceof ServerPlayerEntity sp) {
            String[] slotLabels = {"✦ Glove", "✦ Necklace", "✦ Ring"};
            EquipSlot[] slots = EquipSlot.values();
            for (int i = 0; i < slots.length; i++) {
                ItemStack eq = EquipmentManager.getEquipped(sp, slots[i]);
                int eqX = px + 10 + i * 115;
                String label = slotLabels[i] + ": ";
                String val = eq.isEmpty() ? "§8—" : "§f" + eq.getName().getString();
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal(label).formatted(Formatting.GOLD),
                    eqX, eqY + 15, WHITE);
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal(val),
                    eqX, eqY + 25, WHITE);
            }
        } else {
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("(available in single-player / LAN)").formatted(Formatting.DARK_GRAY),
                px+10, eqY+15, WHITE);
        }

        // ── COMMANDS HINT ─────────────────────────────────────────────────────
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("/isekraft equipment  |  /isekraft unequip <slot>").formatted(Formatting.DARK_GRAY),
            px+W/2, py+H-28, WHITE);

        super.render(ctx, mx, my, delta);
    }

    /** Draw a dark stone panel with subtle texture */
    private void drawStoneBg(DrawContext ctx, int x, int y, int w, int h) {
        // Base dark fill
        ctx.fill(x, y, x+w, y+h, 0xEE0C0A09);
        // Subtle stone texture simulation — slightly lighter patches
        for (int ty = 0; ty < h; ty += 16) {
            for (int tx = 0; tx < w; tx += 16) {
                int offset = ((tx/16 + ty/16) % 3);
                int shade = 0xEE000000 | ((12+offset) << 16) | ((10+offset) << 8) | (8+offset);
                ctx.fill(x+tx, y+ty, Math.min(x+tx+15, x+w), Math.min(y+ty+15, y+h), shade);
            }
        }
        // Mortar lines
        for (int ty = 0; ty < h; ty += 16)
            ctx.fill(x, y+ty, x+w, y+ty+1, 0xFF060404);
        for (int tx = 0; tx < w; tx += 16)
            ctx.fill(x+tx, y, x+tx+1, y+h, 0xFF060404);
    }

    /** Draw a recessed dark panel */
    private void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x+w, y+h, 0xDD0A0606);
        ctx.fill(x, y, x+w, y+1, 0xFF330000);
        ctx.fill(x, y+h-1, x+w, y+h, 0xFF330000);
        ctx.fill(x, y, x+1, y+h, 0xFF330000);
        ctx.fill(x+w-1, y, x+w, y+h, 0xFF330000);
    }

    @Override public boolean keyPressed(int key, int scan, int mod) {
        if (key == 72) { close(); return true; }
        return super.keyPressed(key, scan, mod);
    }
    @Override public boolean shouldPause() { return false; }
}
