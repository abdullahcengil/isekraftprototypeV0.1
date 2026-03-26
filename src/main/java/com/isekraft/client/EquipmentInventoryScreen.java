package com.isekraft.client;

import com.isekraft.equipment.EquipSlot;
import com.isekraft.equipment.EquipmentItem;
import com.isekraft.equipment.EquipmentManager;
import com.isekraft.rpg.PlayerRpgManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * RPG Equipment Inventory Screen — press I (registered in IseKraftClient).
 *
 * Layout:
 *   Left panel  — 3 equipment slots with symbols (✦ Glove / ◈ Necklace / ◉ Ring)
 *                 each shows item name + stats if equipped
 *   Right panel — player stats summary (HP, DMG, Speed, level)
 *   Bottom      — active quest tracker (compact, 3 lines max)
 *
 * Visual style: dark slate with gold borders, matching CharacterScreen aesthetic.
 */
@Environment(EnvType.CLIENT)
public class EquipmentInventoryScreen extends Screen {

    private static final int W = 380;
    private static final int H = 280;

    // Colours
    private static final int COL_BG       = 0xEE0B0909;
    private static final int COL_PANEL    = 0xDD0D0A0A;
    private static final int COL_BORDER   = 0xFFAA8800;
    private static final int COL_BORDER2  = 0xFF664400;
    private static final int COL_RED      = 0xFFCC2200;
    private static final int COL_GOLD     = 0xFFDAA520;
    private static final int COL_WHITE    = 0xFFEEEEEE;
    private static final int COL_GRAY     = 0xFF888888;
    private static final int COL_DARK     = 0xFF444444;
    private static final int COL_SLOT_EMP = 0xFF1A1410;
    private static final int COL_SLOT_FUL = 0xFF1F1A08;
    private static final int COL_GLOVE    = 0xFFCC6600;   // orange
    private static final int COL_NECK     = 0xFF9944CC;   // purple
    private static final int COL_RING     = 0xFF44AACC;   // cyan

    // Slot symbols & accent colours
    private static final String[] SLOT_SYMBOLS = { "✦", "◈", "◉" };
    private static final String[] SLOT_NAMES   = { "GLOVE", "NECKLACE", "RING" };
    private static final int[]    SLOT_COLORS  = { COL_GLOVE, COL_NECK, COL_RING };

    // Tooltip hover state
    private int hoveredSlot = -1;

    public EquipmentInventoryScreen() { super(Text.literal("Equipment")); }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;

        addDrawableChild(ButtonWidget.builder(
            Text.literal("✕").formatted(Formatting.GOLD),
            b -> close()
        ).dimensions(px + W - 22, py + 4, 18, 14).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xAA000000);

        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) { super.render(ctx, mx, my, delta); return; }

        int px = (width - W) / 2, py = (height - H) / 2;

        // ── MAIN BACKGROUND ──────────────────────────────────────────────────
        ctx.fill(px, py, px + W, py + H, COL_BG);
        drawBorder(ctx, px, py, W, H, COL_BORDER);
        drawBorder(ctx, px + 3, py + 3, W - 6, H - 6, COL_BORDER2);

        // ── HEADER ───────────────────────────────────────────────────────────
        ctx.fill(px + 2, py + 2, px + W - 2, py + 22, 0xEE120E00);
        ctx.fill(px + 2, py + 21, px + W - 2, py + 23, COL_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("⚔  EQUIPMENT  ⚔").formatted(Formatting.GOLD, Formatting.BOLD),
            px + W / 2, py + 7, COL_WHITE);

        // ── LEFT: EQUIPMENT SLOTS (x3) ───────────────────────────────────────
        int slotPanelX = px + 10;
        int slotPanelY = py + 28;
        int slotPanelW = 180;
        int slotPanelH = 195;

        ctx.fill(slotPanelX, slotPanelY, slotPanelX + slotPanelW, slotPanelY + slotPanelH, COL_PANEL);
        drawBorder(ctx, slotPanelX, slotPanelY, slotPanelW, slotPanelH, COL_BORDER2);

        ctx.drawTextWithShadow(textRenderer,
            Text.literal("ACCESSORIES").formatted(Formatting.GOLD),
            slotPanelX + 6, slotPanelY + 5, COL_WHITE);
        ctx.fill(slotPanelX + 4, slotPanelY + 16, slotPanelX + slotPanelW - 4, slotPanelY + 17, COL_BORDER2);

        // Only render server-side data in singleplayer/LAN
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean canRead = mc.getServer() != null && player instanceof ServerPlayerEntity;
        ServerPlayerEntity sp = canRead ? (ServerPlayerEntity) player : null;

        hoveredSlot = -1;

        for (int i = 0; i < 3; i++) {
            int sy = slotPanelY + 22 + i * 56;
            EquipSlot slot = EquipSlot.values()[i];
            ItemStack equipped = (sp != null) ? EquipmentManager.getEquipped(sp, slot) : ItemStack.EMPTY;
            boolean filled = !equipped.isEmpty();
            boolean hovered = mx >= slotPanelX + 4 && mx <= slotPanelX + slotPanelW - 4
                           && my >= sy && my <= sy + 50;
            if (hovered) hoveredSlot = i;

            // Slot background
            int bgColor = filled ? COL_SLOT_FUL : COL_SLOT_EMP;
            int borderColor = hovered ? SLOT_COLORS[i] : (filled ? COL_BORDER2 : 0xFF221A10);
            ctx.fill(slotPanelX + 4, sy, slotPanelX + slotPanelW - 4, sy + 50, bgColor);
            drawBorder(ctx, slotPanelX + 4, sy, slotPanelW - 8, 50, borderColor);

            // Big slot symbol
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(SLOT_SYMBOLS[i]),
                slotPanelX + 10, sy + 8, SLOT_COLORS[i]);

            // Slot label
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(SLOT_NAMES[i]).formatted(Formatting.BOLD),
                slotPanelX + 24, sy + 6, SLOT_COLORS[i]);

            if (filled) {
                // Item name
                String name = equipped.getName().getString();
                if (name.length() > 18) name = name.substring(0, 17) + "…";
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal(name).formatted(Formatting.WHITE),
                    slotPanelX + 10, sy + 20, COL_WHITE);

                // Stats from EquipmentItem
                if (equipped.getItem() instanceof EquipmentItem eq) {
                    List<String> statLines = new ArrayList<>();
                    if (eq.hpBonus > 0)     statLines.add("+" + eq.hpBonus + " HP");
                    if (eq.damageBonus > 0) statLines.add("+" + eq.damageBonus + " DMG");
                    if (eq.speedBonus > 0)  statLines.add("+" + Math.round(eq.speedBonus * 1000f) / 10f + "% SPD");
                    if (eq.passiveEffect != null)
                        statLines.add(eq.passiveEffect.getName().getString());

                    String statsStr = String.join("  ", statLines);
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal(statsStr).formatted(Formatting.GRAY),
                        slotPanelX + 10, sy + 32, COL_GRAY);
                }

                // Level req badge
                if (equipped.getItem() instanceof EquipmentItem eq2) {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("Lv." + eq2.levelRequired).formatted(Formatting.DARK_GRAY),
                        slotPanelX + slotPanelW - 36, sy + 6, COL_GRAY);
                }
            } else {
                // Empty slot hint
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("— empty —").formatted(Formatting.DARK_GRAY),
                    slotPanelX + 10, sy + 20, COL_DARK);
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("drops from mobs").formatted(Formatting.DARK_GRAY),
                    slotPanelX + 10, sy + 32, 0xFF332800);
            }

            // Unequip hint on hover
            if (hovered && filled) {
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("§8/isekraft unequip " + slot.typeTag),
                    slotPanelX + 10, sy + 42, COL_DARK);
            }
        }

        // ── RIGHT: STATS PANEL ────────────────────────────────────────────────
        int stX = px + 200, stY = py + 28;
        int stW = 170, stH = 120;
        ctx.fill(stX, stY, stX + stW, stY + stH, COL_PANEL);
        drawBorder(ctx, stX, stY, stW, stH, COL_BORDER2);

        int level = PlayerRpgManager.getLevel(player);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("PLAYER STATS").formatted(Formatting.GOLD),
            stX + 6, stY + 5, COL_WHITE);
        ctx.fill(stX + 4, stY + 16, stX + stW - 4, stY + 17, COL_BORDER2);

        // Calculate totals including equipment
        int baseHp = 20 + Math.min(level - 1, 40);
        float totalDmg = level / 2f;
        if (sp != null) {
            for (EquipSlot slot : EquipSlot.values()) {
                ItemStack eq = EquipmentManager.getEquipped(sp, slot);
                if (!eq.isEmpty() && eq.getItem() instanceof EquipmentItem e) {
                    baseHp   += e.hpBonus;
                    totalDmg += e.damageBonus;
                }
            }
        }

        String[][] statRows = {
            { "❤", "Max HP",    baseHp + " hp" },
            { "⚔", "Attack",   "+" + (int)totalDmg + " dmg" },
            { "⚡", "Speed",    "+" + (level / 10 * 2) + "%" },
            { "★", "Level",    level + "  [" + PlayerRpgManager.getLevelTitle(level) + "]" },
            { "☠", "Kills",    PlayerRpgManager.getTotalKills(player) + "" },
        };

        for (int i = 0; i < statRows.length; i++) {
            int ry = stY + 22 + i * 18;
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(statRows[i][0] + " " + statRows[i][1]).formatted(Formatting.GRAY),
                stX + 8, ry, COL_WHITE);
            String val = statRows[i][2];
            int valX = stX + stW - 4 - textRenderer.getWidth(val);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(val).formatted(Formatting.WHITE),
                valX, ry, COL_WHITE);
        }

        // ── RIGHT LOWER: SKILL PATH ───────────────────────────────────────────
        int pathY = stY + stH + 8;
        ctx.fill(stX, pathY, stX + stW, pathY + 50, COL_PANEL);
        drawBorder(ctx, stX, pathY, stW, 50, COL_BORDER2);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("SKILL PATH").formatted(Formatting.GOLD),
            stX + 6, pathY + 5, COL_WHITE);
        ctx.fill(stX + 4, pathY + 16, stX + stW - 4, pathY + 17, COL_BORDER2);

        int path = PlayerRpgManager.getChosenPath(player);
        String pathName = switch (path) {
            case 1 -> "⚔ Warrior";
            case 2 -> "✦ Mage";
            case 3 -> "🏹 Ranger";
            default -> "— Not chosen";
        };
        int pathColor = switch (path) {
            case 1 -> 0xFFFF4422;
            case 2 -> 0xFF44AAFF;
            case 3 -> 0xFF44CC44;
            default -> COL_DARK;
        };
        ctx.drawTextWithShadow(textRenderer,
            Text.literal(pathName),
            stX + 8, pathY + 22, pathColor);
        int sp2 = PlayerRpgManager.getAvailableSkillPoints(player);
        if (sp2 > 0) {
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(sp2 + " pts available — press K").formatted(Formatting.YELLOW),
                stX + 8, pathY + 34, COL_WHITE);
        }

        // ── BOTTOM: QUEST TRACKER ─────────────────────────────────────────────
        int qY = py + 230;
        ctx.fill(px + 10, qY, px + W - 10, qY + 40, COL_PANEL);
        drawBorder(ctx, px + 10, qY, W - 20, 40, COL_BORDER2);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("✦ ACTIVE QUESTS").formatted(Formatting.GOLD),
            px + 16, qY + 4, COL_WHITE);

        // Quest data only available in singleplayer — show placeholder on remote
        if (sp != null) {
            List<String> active = com.isekraft.quest.QuestManager.getActive(sp);
            if (active.isEmpty()) {
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("No active quests — talk to an Isekai NPC or villager").formatted(Formatting.DARK_GRAY),
                    px + 16, qY + 17, COL_WHITE);
            } else {
                for (int i = 0; i < Math.min(2, active.size()); i++) {
                    com.isekraft.quest.Quest q = com.isekraft.quest.QuestRegistry.get(active.get(i));
                    if (q == null) continue;
                    int prog = com.isekraft.quest.QuestManager.getProgress(sp, active.get(i));
                    String line = "► " + q.title + "  §8[" + prog + "/" + q.goal + "]";
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal(line).formatted(Formatting.YELLOW),
                        px + 16, qY + 15 + i * 12, COL_WHITE);
                }
                if (active.size() > 2) {
                    ctx.drawTextWithShadow(textRenderer,
                        Text.literal("+" + (active.size() - 2) + " more").formatted(Formatting.DARK_GRAY),
                        px + W - 60, qY + 27, COL_WHITE);
                }
            }
        } else {
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("Quest data requires single-player or LAN").formatted(Formatting.DARK_GRAY),
                px + 16, qY + 17, COL_WHITE);
        }

        super.render(ctx, mx, my, delta);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y,         x + 1,     y + h,     color);
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color);
    }

    @Override public boolean keyPressed(int key, int scan, int mod) {
        // I key or Escape closes
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_I || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            close(); return true;
        }
        return super.keyPressed(key, scan, mod);
    }
    @Override public boolean shouldPause() { return false; }
}
