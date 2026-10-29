package com.echocity.velmar;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Отдельные мини-игры: fish (рыбалка), cups (напёрстки), pick (карманник), forge (кузница).
 * Результат уходит на сервер командой /echocity game win|lose|quit.
 */
public final class MiniGameScreen extends Screen {
    private final String type, title, intro;
    private final boolean assist;
    private final Random rnd = new Random();

    private int phase = 0;          // 0 — инструкция, 1 — игра, 2 — итог
    private int t = 0;              // тики с начала игры
    private int doneTimer = 0;
    private boolean won = false, sent = false;
    private String resultText = "";

    // раскладка панели
    private int px, py, pw, ph;

    // --- рыбалка ---
    private int waitTarget, biteAt = -1;

    // --- напёрстки ---
    private final int[] slotOf = {0, 1, 2};
    private int ballCup;
    private final List<int[]> swaps = new ArrayList<>();
    private int swapIdx = 0, swapT = 0, cupSub = 0, cupTimer = 0, swapDur;
    private int pickedCup = -1;

    // --- карманник ---
    private float zoneC;
    private int hits = 0, misses = 0, flash = 0;
    private boolean flashGood = false;
    private final float zoneW;

    // --- кузница ---
    private static final int[] NOTE_AT = {20, 44, 68, 84, 108, 132, 148, 172};
    private final boolean[] noteDone = new boolean[NOTE_AT.length];
    private int forgeHits = 0, sparks = 0;
    private static final float SPEED = 3.0F;

    public MiniGameScreen(String[] f) {
        super(Component.empty());
        this.type = get(f, 1);
        this.title = get(f, 2);
        this.intro = get(f, 3);
        this.assist = "1".equals(get(f, 4));
        this.zoneW = assist ? 56F : 38F;
        initGame();
    }

    private static String get(String[] f, int i) { return i < f.length ? f[i] : ""; }

    private void initGame() {
        waitTarget = 50 + rnd.nextInt(100);
        ballCup = rnd.nextInt(3);
        int n = assist ? 5 : 7;
        swapDur = assist ? 20 : 14;
        for (int i = 0; i < n; i++) {
            int a = rnd.nextInt(3), b = (a + 1 + rnd.nextInt(2)) % 3;
            swaps.add(new int[]{a, b});
        }
        zoneC = 0.2F + rnd.nextFloat() * 0.6F;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float pt) { /* рисуем сами */ }

    private void snd(net.minecraft.sounds.SoundEvent e, float pitch) {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(e, pitch));
    }

    // ---------- управление ----------
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { send("quit"); return true; }
        if (phase == 0 && (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER)) { phase = 1; t = 0; return true; }
        if (phase == 1) {
            if (type.equals("cups")) {
                if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_3) { chooseSlot(key - GLFW.GLFW_KEY_1); return true; }
            } else if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) { press(); return true; }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase == 0) { phase = 1; t = 0; return true; }
        if (phase != 1) return true;
        if (type.equals("cups")) {
            for (int s = 0; s < 3; s++) {
                int cx = slotX(s);
                if (mx >= cx - 28 && mx <= cx + 28 && my >= cupBaseY() - 56 && my <= cupBaseY() + 24) { chooseSlot(s); return true; }
            }
        } else press();
        return true;
    }

    private void finish(boolean win, String text) {
        if (phase == 2) return;
        phase = 2; won = win; resultText = text; doneTimer = 0;
        snd(win ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_NO, win ? 1.0F : 1.0F);
    }

    private void send(String res) {
        if (sent) return;
        sent = true;
        if (this.minecraft.getConnection() != null) this.minecraft.getConnection().sendCommand("echocity game " + res);
        this.minecraft.setScreen(null);
    }

    private void press() {
        switch (type) {
            case "fish" -> {
                if (biteAt < 0) finish(false, "Слишком рано. Рыба ушла.");
                else if (t - biteAt <= (assist ? 28 : 20)) { snd(SoundEvents.FISHING_BOBBER_SPLASH, 1.0F); finish(true, "Есть! Клюнуло!"); }
            }
            case "pick" -> {
                float mark = markerPos();
                boolean hit = Math.abs(mark - zoneC) <= (zoneW / 2F) / barW();
                flash = 10; flashGood = hit;
                if (hit) { hits++; snd(SoundEvents.NOTE_BLOCK_HAT, 1.8F); } else { misses++; snd(SoundEvents.VILLAGER_NO, 1.4F); }
                zoneC = 0.15F + rnd.nextFloat() * 0.7F;
                if (hits >= 3) finish(true, "Кошелёк у тебя!");
                else if (misses >= 2) finish(false, "Стражник заметил!");
            }
            case "forge" -> {
                int idx = -1; float best = 9999;
                for (int i = 0; i < NOTE_AT.length; i++) {
                    if (noteDone[i]) continue;
                    float d = Math.abs(noteY(i) - hitY());
                    if (d < best) { best = d; idx = i; }
                }
                if (idx >= 0 && best <= (assist ? 24 : 17)) {
                    noteDone[idx] = true; forgeHits++; sparks = 8; snd(SoundEvents.ANVIL_LAND, 1.4F);
                } else {
                    snd(SoundEvents.NOTE_BLOCK_HAT, 0.6F);
                }
            }
            default -> { }
        }
    }

    // ---------- логика по тикам ----------
    @Override
    public void tick() {
        blink++;
        if (phase == 1) {
            t++;
            switch (type) {
                case "fish" -> {
                    if (biteAt < 0 && t >= waitTarget) { biteAt = t; snd(SoundEvents.FISHING_BOBBER_SPLASH, 1.5F); }
                    else if (biteAt >= 0 && t - biteAt > (assist ? 28 : 20)) finish(false, "Слишком поздно. Рыба ушла.");
                }
                case "cups" -> tickCups();
                case "forge" -> {
                    if (sparks > 0) sparks--;
                    boolean allDone = true;
                    for (int i = 0; i < NOTE_AT.length; i++) {
                        if (!noteDone[i] && noteY(i) > hitY() + (assist ? 24 : 17)) noteDone[i] = true;
                        if (!noteDone[i]) allDone = false;
                    }
                    if (allDone) finish(forgeHits >= 6, forgeHits >= 6 ? "Клинок готов! (" + forgeHits + " из 8)" : "Металл остыл. (" + forgeHits + " из 8)");
                }
                case "pick" -> { if (flash > 0) flash--; }
                default -> { }
            }
        } else if (phase == 2) {
            if (++doneTimer > 50) send(won ? "win" : "lose");
        }
    }

    private void tickCups() {
        cupTimer++;
        switch (cupSub) {
            case 0 -> { if (cupTimer > 50) { cupSub = 1; cupTimer = 0; } }          // показываем монету
            case 1 -> { if (cupTimer > 12) { cupSub = 2; cupTimer = 0; swapT = 0; } } // опускаем чашки
            case 2 -> {
                if (++swapT >= swapDur) {
                    int[] s = swaps.get(swapIdx);
                    int tmp = slotOf[s[0]]; slotOf[s[0]] = slotOf[s[1]]; slotOf[s[1]] = tmp;
                    snd(SoundEvents.NOTE_BLOCK_HAT, 0.8F);
                    swapIdx++; swapT = 0;
                    if (swapIdx >= swaps.size()) { cupSub = 3; cupTimer = 0; }
                }
            }
            case 4 -> { /* итог: чашки подняты, ждём doneTimer */ }
            default -> { }
        }
    }

    private void chooseSlot(int s) {
        if (cupSub != 3) return;
        for (int j = 0; j < 3; j++) if (slotOf[j] == s) pickedCup = j;
        cupSub = 4;
        boolean win = pickedCup == ballCup;
        finish(win, win ? "Угадал!" : "Пусто!");
    }

    // ---------- геометрия ----------
    private int slotX(int s) { return this.width / 2 + (s - 1) * 84; }
    private int cupBaseY() { return py + ph / 2 + 18; }
    private float barW() { return 260F; }
    private float markerPos() {          // 0..1 туда-обратно
        float v = (t * (assist ? 0.012F : 0.017F)) % 2F;
        return v <= 1F ? v : 2F - v;
    }
    private float noteY(int i) { return hitY() + (t - NOTE_AT[i]) * SPEED; }
    private float hitY() { return py + ph - 46; }

    // ---------- рисование ----------
    private void box(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, border);
        g.fill(x, y, x + w, y + h, fill);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0xB0000000);
        pw = Math.min(340, this.width - 20);
        ph = Math.min(210, this.height - 20);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        SoftUi.panel(g, px, py, pw, ph, SoftUi.GOLD, SoftUi.FILL, 1.0F);
        g.drawCenteredString(this.font, title, this.width / 2, py + 10, 0xFFFFD27A);

        float time = t + pt;
        switch (type) {
            case "fish" -> drawFish(g, time);
            case "cups" -> drawCups(g, pt);
            case "pick" -> drawPick(g, time);
            case "forge" -> drawForge(g, time);
            default -> { }
        }

        if (phase == 0) {
            g.fill(px, py + 22, px + pw, py + ph, 0xDD000000);
            int y = py + 40;
            for (FormattedText ft : this.font.getSplitter().splitLines(intro, pw - 40, Style.EMPTY)) {
                String line = ft.getString();
                g.drawString(this.font, line, (this.width - this.font.width(line)) / 2, y, 0xFFFFFFFF, true);
                y += 12;
            }
            if (assist) g.drawCenteredString(this.font, "✦ Эхо помогает: игра чуть проще", this.width / 2, y + 8, 0xFFD58CFF);
            if ((blink / 10) % 2 == 0) g.drawCenteredString(this.font, "Пробел или клик: начать", this.width / 2, py + ph - 30, 0xFFAAAAAA);
        }
        if (phase == 2) {
            int col = won ? 0xFF7CFF9B : 0xFFFF7C7C;
            g.pose().pushPose();
            g.pose().scale(2.0F, 2.0F, 1.0F);
            g.drawCenteredString(this.font, won ? "Победа!" : "Неудача", (int) (this.width / 4.0F), (int) ((py + ph - 52) / 2.0F), col);
            g.pose().popPose();
            g.drawCenteredString(this.font, resultText, this.width / 2, py + ph - 28, 0xFFFFFFFF);
        }
        g.drawString(this.font, "Esc: выйти", px + 6, py + ph - 12, 0xFF777777, false);
    }

    private int blink = 0;

    private void drawFish(GuiGraphics g, float time) {
        int wy = py + 96;
        g.fill(px + 6, py + 26, px + pw - 6, wy, 0xFF8ECBF5);                  // небо
        g.fill(px + 6, wy, px + pw - 6, py + ph - 52, 0xFF2F6FCF);             // вода
        g.fill(px + 6, wy + 22, px + pw - 6, py + ph - 52, 0xFF2A5FB5);
        int cx = this.width / 2;
        float bob = (float) Math.sin(time * 0.15F) * 1.5F;
        boolean bite = biteAt >= 0 && phase == 1;
        int by = wy - 4 + (int) bob + (bite ? 8 : 0);
        for (int i = 0; i < 3; i++) {                                           // круги на воде
            float r = (time * 0.4F + i * 12) % 36;
            int rr = (int) r;
            g.fill(cx - 10 - rr, wy + 2, cx + 10 + rr, wy + 3, 0x40FFFFFF);
        }
        g.fill(cx - 1, py + 30, cx, by - 3, 0xFFEEEEEE);                        // леска
        box(g, cx - 4, by - 2, 8, 8, 0xFFE53935, 0xFFFFFFFF);                  // поплавок
        if (bite) g.drawCenteredString(this.font, "!", cx, by - 22, 0xFFFF4444);
        if (phase == 2 && won) {                                                // рыба
            g.fill(cx - 14, wy - 30, cx + 14, wy - 20, 0xFFB0BEC5);
            g.fill(cx + 14, wy - 28, cx + 20, wy - 22, 0xFF78909C);
            g.fill(cx - 10, wy - 27, cx - 8, wy - 25, 0xFF000000);
        }
        if (phase == 1 && !bite) g.drawCenteredString(this.font, "Жди...", cx, py + ph - 70, 0xFFCCCCCC);
        if (phase == 1) g.drawCenteredString(this.font, "ПРОБЕЛ, когда поплавок нырнёт", cx, py + ph - 46, 0xFFAAAAAA);
    }

    private void drawCups(GuiGraphics g, float pt) {
        int base = cupBaseY();
        g.fill(px + 14, base + 22, px + pw - 14, base + 34, 0xFF7B4E2B);        // стол
        g.fill(px + 14, base + 22, px + pw - 14, base + 25, 0xFFA8703F);
        for (int j = 0; j < 3; j++) {
            float x = slotX(slotOf[j]); float y = base;
            if (cupSub == 2) {
                int[] s = swaps.get(Math.min(swapIdx, swaps.size() - 1));
                if (j == s[0] || j == s[1]) {
                    int other = j == s[0] ? s[1] : s[0];
                    float f = Mth.clamp((swapT + pt) / swapDur, 0F, 1F);
                    float e = f * f * (3F - 2F * f);
                    x = slotX(slotOf[j]) + (slotX(slotOf[other]) - slotX(slotOf[j])) * e;
                    y = base + (j == s[0] ? -1 : 1) * (float) Math.sin(f * Math.PI) * 16F;
                }
            }
            boolean lifted = cupSub == 0 || cupSub == 4 || (cupSub == 1 && cupTimer < 6);
            if (cupSub == 4 && phase == 2) lifted = true;
            int cx = (int) x, cy = (int) y;
            if (j == ballCup && (cupSub == 0 || cupSub == 1 || cupSub == 4)) {
                box(g, cx - 5, base + 8, 10, 10, 0xFFFFD54F, 0xFFB8860B);       // монета
            }
            int ly = lifted ? cy - 28 : cy;
            box(g, cx - 22, ly - 20, 44, 36, j == pickedCup ? 0xFFD98B3A : 0xFFB5651D, 0xFF5D3513);   // чашка
            g.fill(cx - 26, ly - 24, cx + 26, ly - 18, 0xFFE0A15A);            // кромка
            g.fill(cx - 16, ly - 14, cx - 10, ly + 12, 0x30FFFFFF);
        }
        String hint = switch (cupSub) {
            case 0 -> "Запомни, где монета!";
            case 2, 1 -> "Следи за чашками...";
            case 3 -> "Выбери чашку: клик или 1, 2, 3";
            default -> "";
        };
        if (phase == 1 || phase == 2) g.drawCenteredString(this.font, hint, this.width / 2, py + 30, 0xFFCCCCCC);
    }

    private void drawPick(GuiGraphics g, float time) {
        int bw = (int) barW();
        int bx = this.width / 2 - bw / 2, by = py + 84;
        // кошель и стражник
        box(g, this.width / 2 - 18, py + 32, 36, 30, 0xFF5E6B7A, 0xFF2B323A);
        box(g, this.width / 2 + 24, py + 50, 14, 16, 0xFF8B5A2B, 0xFF4A2F15);
        g.drawString(this.font, "кошель", this.width / 2 + 42, py + 54, 0xFFAAAAAA, false);
        box(g, bx, by, bw, 18, 0xFF222222, 0xFF555555);
        int zx = bx + (int) (zoneC * bw - zoneW / 2F);
        g.fill(zx, by, zx + (int) zoneW, by + 18, flash > 0 ? (flashGood ? 0xFF7CFF9B : 0xFFFF7C7C) : 0xFF3EBF5E);
        float mk = phase == 1 ? markerPos() : 0.5F;
        int mx = bx + (int) (mk * bw);
        g.fill(mx - 2, by - 6, mx + 2, by + 24, 0xFFFFFFFF);
        g.drawString(this.font, "Удачно: " + "●".repeat(hits) + "○".repeat(Math.max(0, 3 - hits)), bx, by + 40, 0xFF7CFF9B, false);
        g.drawString(this.font, "Промахи: " + "●".repeat(misses) + "○".repeat(Math.max(0, 2 - misses)), bx + 140, by + 40, 0xFFFF7C7C, false);
        if (phase == 1) g.drawCenteredString(this.font, "ПРОБЕЛ, когда белая черта в зелёной зоне", this.width / 2, py + ph - 46, 0xFFAAAAAA);
    }

    private void drawForge(GuiGraphics g, float time) {
        int cx = this.width / 2;
        int hy = (int) hitY();
        int top = py + 26;
        box(g, cx - 20, top, 40, hy + 10 - top, 0xFF1E1E26, 0xFF444455);       // дорожка
        g.fill(cx - 20, hy - 1, cx + 20, hy + 2, 0xFFFFA726);                  // линия удара
        for (int i = 0; i < NOTE_AT.length; i++) {
            if (noteDone[i]) continue;
            float y = noteY(i);
            if (y < top || y > hy + 12) continue;
            box(g, cx - 14, (int) y - 4, 28, 8, 0xFFFF7043, 0xFFFFCCBC);
        }
        box(g, cx + 50, hy - 6, 60, 14, 0xFF616161, 0xFF212121);              // наковальня
        box(g, cx + 58, hy + 8, 44, 10, 0xFF424242, 0xFF212121);
        if (sparks > 0) for (int i = 0; i < 6; i++) g.fill(cx + 62 + i * 6, hy - 12 - (sparks + i * 3) % 10, cx + 65 + i * 6, hy - 9 - (sparks + i * 3) % 10, 0xFFFFEB3B);
        g.drawString(this.font, "Удары: " + forgeHits + " / 8 (нужно 6)", px + 12, py + 28, 0xFFFFD36B, false);
        if (phase == 1) g.drawCenteredString(this.font, "ПРОБЕЛ, когда нота на оранжевой линии", cx, py + ph - 26, 0xFFAAAAAA);
    }
}
