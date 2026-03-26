package com.isekraft.client;

import com.isekraft.network.ModPackets;
import com.isekraft.quest.Quest;
import com.isekraft.quest.QuestRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Quest Board Screen.
 *
 * Opened by server packet (OPEN_QUEST_BOARD) when player right-clicks:
 *   • An Isekai NPC entity
 *   • A vanilla Villager
 *
 * Layout:
 *   Left  — scrollable quest list, filtered by player level
 *   Right — quest detail panel: title, description, goal, rewards
 *   Bottom— Accept / Abandon / Close buttons
 *
 * Button actions send C2S packets: QUEST_ACCEPT / QUEST_ABANDON.
 * Server-side validation lives in QuestManager — the screen trusts nothing.
 */
@Environment(EnvType.CLIENT)
public class QuestBoardScreen extends Screen {

    private static final int W = 400;
    private static final int H = 300;

    // Colours
    private static final int BG       = 0xEE0D0A06;
    private static final int PANEL    = 0xDD120E08;
    private static final int BORDER   = 0xFFAA8800;
    private static final int BORDER2  = 0xFF664400;
    private static final int WHITE    = 0xFFEEEEEE;
    private static final int GOLD     = 0xFFDAA520;
    private static final int GRAY     = 0xFF888888;
    private static final int DARK     = 0xFF444444;
    private static final int GREEN    = 0xFF44CC44;
    private static final int RED      = 0xFFCC2200;
    private static final int YELLOW   = 0xFFFFDD00;
    private static final int DONE_BG  = 0xDD0A120A;
    private static final int LOCK_BG  = 0xDD0D0808;

    private final String npcName;
    private final int    playerLevel;
    private final List<String> activeQuestIds;
    private final List<String> completedQuestIds;

    private int selectedIndex = 0;
    private int scrollOffset  = 0;
    private static final int ROWS_VISIBLE = 9;
    private static final int ROW_H        = 22;

    private List<Quest> visibleQuests = new ArrayList<>();

    // Buttons
    private ButtonWidget btnAccept;
    private ButtonWidget btnAbandon;

    public QuestBoardScreen(String npcName, int playerLevel,
                            List<String> activeIds, List<String> completedIds) {
        super(Text.literal("Quest Board"));
        this.npcName           = npcName;
        this.playerLevel       = playerLevel;
        this.activeQuestIds    = activeIds;
        this.completedQuestIds = completedIds;

        // Build visible list: all quests, sorted by levelRequired
        visibleQuests = new ArrayList<>(QuestRegistry.getAll().values());
        visibleQuests.sort((a, b) -> a.levelRequired - b.levelRequired);
    }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;

        // Accept button
        btnAccept = ButtonWidget.builder(
            Text.literal("✦ Accept").formatted(Formatting.GREEN),
            b -> onAccept()
        ).dimensions(px + 210, py + H - 28, 80, 20).build();
        addDrawableChild(btnAccept);

        // Abandon button
        btnAbandon = ButtonWidget.builder(
            Text.literal("✗ Abandon").formatted(Formatting.RED),
            b -> onAbandon()
        ).dimensions(px + 298, py + H - 28, 80, 20).build();
        addDrawableChild(btnAbandon);

        // Close
        addDrawableChild(ButtonWidget.builder(
            Text.literal("✕ Close").formatted(Formatting.GOLD),
            b -> close()
        ).dimensions(px + W - 22, py + 4, 18, 14).build());

        updateButtons();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xAA000000);

        int px = (width - W) / 2, py = (height - H) / 2;

        // Main bg
        ctx.fill(px, py, px + W, py + H, BG);
        border(ctx, px, py, W, H, BORDER);
        border(ctx, px + 3, py + 3, W - 6, H - 6, BORDER2);

        // Header
        ctx.fill(px + 2, py + 2, px + W - 2, py + 22, 0xEE150F00);
        ctx.fill(px + 2, py + 21, px + W - 2, py + 23, BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("📜  " + npcName + "'s Quest Board  📜").formatted(Formatting.GOLD, Formatting.BOLD),
            px + W / 2, py + 7, WHITE);

        // ── LEFT: QUEST LIST ─────────────────────────────────────────────────
        int listX = px + 6, listY = py + 26;
        int listW = 170, listH = ROWS_VISIBLE * ROW_H + 2;
        ctx.fill(listX, listY, listX + listW, listY + listH, PANEL);
        border(ctx, listX, listY, listW, listH, BORDER2);

        for (int i = 0; i < ROWS_VISIBLE; i++) {
            int qi = i + scrollOffset;
            if (qi >= visibleQuests.size()) break;
            Quest q    = visibleQuests.get(qi);
            int   ry   = listY + 1 + i * ROW_H;
            boolean sel  = (qi == selectedIndex);
            boolean done = completedQuestIds.contains(q.id);
            boolean act  = activeQuestIds.contains(q.id);
            boolean lock = playerLevel < q.levelRequired;

            // Row bg
            int rowBg = done ? DONE_BG : lock ? LOCK_BG : sel ? 0xEE1F1800 : 0x00000000;
            ctx.fill(listX + 1, ry, listX + listW - 1, ry + ROW_H - 1, rowBg);
            if (sel) border(ctx, listX + 1, ry, listW - 2, ROW_H - 1, BORDER);

            // Check hover
            if (mx >= listX + 1 && mx <= listX + listW - 1 && my >= ry && my < ry + ROW_H - 1) {
                ctx.fill(listX + 1, ry, listX + listW - 1, ry + ROW_H - 1, 0x22FFAA00);
                if (net.minecraft.client.gui.screen.Screen.hasControlDown()) {
                    selectedIndex = qi;
                    updateButtons();
                }
                // Click detection done in mouseClicked
            }

            // Status icon
            String icon  = done ? "§a✓" : act ? "§e►" : lock ? "§c✗" : "§7○";
            ctx.drawTextWithShadow(textRenderer, Text.literal(icon), listX + 4, ry + 7, WHITE);

            // Quest title (truncated)
            String title = q.title.length() > 14 ? q.title.substring(0, 13) + "…" : q.title;
            int    tCol  = done ? DARK : lock ? RED : act ? 0xFFFFDD44 : WHITE;
            ctx.drawTextWithShadow(textRenderer, Text.literal(title), listX + 18, ry + 7, tCol);
        }

        // Scroll hint
        if (visibleQuests.size() > ROWS_VISIBLE) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("▲/▼ scroll").formatted(Formatting.DARK_GRAY),
                listX + listW / 2, listY + listH + 2, WHITE);
        }

        // ── RIGHT: DETAIL PANEL ───────────────────────────────────────────────
        int detX = px + 182, detY = py + 26;
        int detW = W - 188, detH = H - 58;
        ctx.fill(detX, detY, detX + detW, detY + detH, PANEL);
        border(ctx, detX, detY, detW, detH, BORDER2);

        if (selectedIndex < visibleQuests.size()) {
            Quest q    = visibleQuests.get(selectedIndex);
            boolean done = completedQuestIds.contains(q.id);
            boolean act  = activeQuestIds.contains(q.id);
            boolean lock = playerLevel < q.levelRequired;

            // Title
            int titleColor = done ? GREEN : lock ? RED : act ? YELLOW : GOLD;
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(q.title).formatted(Formatting.BOLD),
                detX + detW / 2, detY + 6, titleColor);
            ctx.fill(detX + 4, detY + 17, detX + detW - 4, detY + 18, BORDER2);

            // Status badge
            String badge = done ? "§a[ COMPLETED ]" : act ? "§e[ ACTIVE ]" : lock ? "§c[ LOCKED Lv." + q.levelRequired + " ]" : "§7[ AVAILABLE ]";
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(badge), detX + detW / 2, detY + 22, WHITE);

                        // Description (word-wrap manual, max 28 chars per line)
            List<String> descLines = wrapText(q.description, 26);
            for (int i = 0; i < descLines.size(); i++) {
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal(descLines.get(i)).formatted(Formatting.GRAY),
                    detX + 6, detY + 36 + i * 10, WHITE);
            }

            // Goal
            int goalY = detY + 80;
            ctx.fill(detX + 4, goalY, detX + detW - 4, goalY + 1, BORDER2);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("OBJECTIVE").formatted(Formatting.GOLD),
                detX + 6, goalY + 4, WHITE);
            String goalStr = switch (q.type) {
                case KILL    -> "Kill §f" + q.goal + "x §7" + friendlyName(q.targetId);
                case FETCH   -> "Collect §f" + q.goal + "x §7" + friendlyName(q.targetId);
                case EXPLORE -> "Reach §fLevel " + q.goal;
            };
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(goalStr), detX + 6, goalY + 16, WHITE);

            // Progress if active
            if (act) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.getServer() != null) {
                    net.minecraft.server.network.ServerPlayerEntity spx = 
                        mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());

                    if (spx != null) {
                        int prog = com.isekraft.quest.QuestManager.getProgress(spx, q.id);
                        ctx.drawTextWithShadow(textRenderer,
                            Text.literal("Progress: §e" + prog + "§7/" + q.goal),
                            detX + 6, goalY + 28, WHITE);
                    }
                }
            }

            // Rewards
            int rewY = goalY + 44;
            ctx.fill(detX + 4, rewY, detX + detW - 4, rewY + 1, BORDER2);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("REWARDS").formatted(Formatting.GOLD),
                detX + 6, rewY + 4, WHITE);
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§e+" + q.xpReward + " §7XP"),
                detX + 6, rewY + 16, WHITE);
            if (q.itemReward != null) {
                String itemName = new net.minecraft.item.ItemStack(q.itemReward).getName().getString();
                ctx.drawTextWithShadow(textRenderer,
                    Text.literal("§b+" + q.itemCount + "x §7" + itemName),
                    detX + 6, rewY + 28, WHITE);
            }

            // Level req
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("§8Requires Level " + q.levelRequired),
                detX + 6, detY + detH - 14, WHITE);
        }

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int listX = px + 6, listY = py + 26;
        int listW = 170;

        for (int i = 0; i < ROWS_VISIBLE; i++) {
            int qi = i + scrollOffset;
            if (qi >= visibleQuests.size()) break;
            int ry = listY + 1 + i * ROW_H;
            if (mx >= listX + 1 && mx <= listX + listW - 1 && my >= ry && my < ry + ROW_H - 1) {
                selectedIndex = qi;
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollOffset = Math.max(0, Math.min(
            scrollOffset - (int)Math.signum(delta),
            Math.max(0, visibleQuests.size() - ROWS_VISIBLE)));
        return true;
    }

    private void onAccept() {
        if (selectedIndex >= visibleQuests.size()) return;
        String qId = visibleQuests.get(selectedIndex).id;
        ModPackets.sendQuestAccept(qId);
        activeQuestIds.add(qId);
        updateButtons();
    }

    private void onAbandon() {
        if (selectedIndex >= visibleQuests.size()) return;
        String qId = visibleQuests.get(selectedIndex).id;
        ModPackets.sendQuestAbandon(qId);
        activeQuestIds.remove(qId);
        updateButtons();
    }

    private void updateButtons() {
        if (btnAccept == null || btnAbandon == null) return;
        if (selectedIndex >= visibleQuests.size()) {
            btnAccept.active = false;
            btnAbandon.active = false;
            return;
        }
        Quest q = visibleQuests.get(selectedIndex);
        boolean done  = completedQuestIds.contains(q.id);
        boolean act   = activeQuestIds.contains(q.id);
        boolean lock  = playerLevel < q.levelRequired;
        boolean full  = !act && activeQuestIds.size() >= 3;

        btnAccept.active  = !done && !act && !lock && !full;
        btnAbandon.active = act;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<String> wrapText(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            if (line.length() + w.length() + 1 > maxChars) {
                lines.add(line.toString().trim());
                line = new StringBuilder();
            }
            line.append(w).append(" ");
        }
        if (!line.isEmpty()) lines.add(line.toString().trim());
        return lines;
    }

    private static String friendlyName(String id) {
        return id.replace("minecraft:", "").replace("isekraft:", "").replace("_", " ");
    }

    private void border(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x + w, y + 1, c);
        ctx.fill(x, y + h - 1, x + w, y + h, c);
        ctx.fill(x, y, x + 1, y + h, c);
        ctx.fill(x + w - 1, y, x + w, y + h, c);
    }

    @Override public boolean shouldPause() { return false; }
}
