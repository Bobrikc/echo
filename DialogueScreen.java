package com.echocity.velmar;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Окно диалога в духе визуальных новелл: портрет собеседника, табличка с именем, текст с печатью
 * по буквам и кнопки вариантов ответа. Ответы отправляются командой /echocity choose N.
 */
public final class DialogueScreen extends Screen {
    private String speaker = "", skin = "", mode = "n", text = "", extra = "";
    private List<String> choices = new ArrayList<>();
    private float shown = 0;
    private boolean waiting = false;
    private int waitTicks = 0;
    private int age = 0;
    private int totalChars = 0;
    private final List<int[]> hit = new ArrayList<>();   // x, y, w, h, индекс варианта

    public DialogueScreen(String[] fields) {
        super(Component.empty());
        apply(fields);
    }

    /** Поля: 0 тип, 1 имя, 2 скин, 3 режим, 4 текст, 5 пометки, 6+ варианты ответа. */
    public void apply(String[] f) {
        speaker = get(f, 1);
        skin = get(f, 2);
        mode = get(f, 3);
        text = get(f, 4);
        extra = get(f, 5);
        choices = new ArrayList<>();
        for (int i = 6; i < f.length; i++) if (!f[i].isEmpty()) choices.add(f[i]);
        shown = 0;
        waiting = false;
        waitTicks = 0;
    }

    private static String get(String[] f, int i) { return i < f.length ? f[i] : ""; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) { /* мир остаётся видимым */ }

    @Override
    public void tick() {
        age++;
        if (shown < totalChars && age % 2 == 0)
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HAT, 1.5F + (age % 3) * 0.1F));
        shown += 1.8F;
        if (waiting && ++waitTicks > 60) { waiting = false; waitTicks = 0; }
    }

    private List<String> wrap(String s, int width) {
        List<String> out = new ArrayList<>();
        if (s.isEmpty()) return out;
        for (FormattedText ft : this.font.getSplitter().splitLines(s, width, Style.EMPTY)) out.add(ft.getString());
        return out;
    }

    private boolean typingDone(List<String> lines) {
        int total = 0;
        for (String l : lines) total += l.length();
        return shown >= total;
    }

    private static final int PORTRAIT = 72;

    /** Масштаб текста: целый, чтобы буквы оставались чёткими, как в самой игре. */
    private int sc() { return this.height >= 460 ? 2 : 1; }
    private int boxW() { return Math.min(this.width - 32, 640); }
    private int textWidth() { return (boxW() - PORTRAIT - 16 - 18 - 20) / sc(); }

    private void scaledText(GuiGraphics g, Component c, float x, float y, int color, boolean shadow, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(this.font, c, 0, 0, color, shadow);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        boolean echo = mode.equals("e");
        int sc = sc();
        int border = echo ? SoftUi.AQUA : SoftUi.GOLD;
        int fillCol = echo ? SoftUi.FILL_ECHO : SoftUi.FILL;
        int textCol = echo ? SoftUi.AQUA_TEXT : SoftUi.CREAM;
        int boxW = boxW();
        int bx = (this.width - boxW) / 2;
        int textX = bx + 16 + PORTRAIT + 18;
        int lineH = 9 * sc + (sc == 2 ? 4 : 3);
        int tabH = 9 * sc + 10;

        List<String> lines = wrap(text, textWidth());
        List<String> exLines = wrap(extra, textWidth());
        boolean done = typingDone(lines);
        totalChars = 0;
        for (String l : lines) totalChars += l.length();
        int top = tabH / 2 + 12;
        int innerH = top + lines.size() * lineH + (exLines.isEmpty() ? 0 : 6 + exLines.size() * lineH) + 14;
        int boxH = Math.max(PORTRAIT + 32, innerH);
        float appear = Mth.clamp((age + pt) / 8.0F, 0.0F, 1.0F);
        int by = this.height - boxH - 22 + (int) ((1.0F - appear * appear * (3.0F - 2.0F * appear)) * 40.0F);

        g.fillGradient(0, by - 140, this.width, this.height, 0x00000000, 0xA0000000);

        // варианты ответа над панелью, справа
        hit.clear();
        List<String> shownChoices = done ? choicesOrClose(echo) : new ArrayList<>();
        int cw = Math.min(boxW - 60, 520);
        int ch = 9 * sc + 14, gap = 6;
        int cx = bx + boxW - cw;
        int cy = by - tabH / 2 - 10 - shownChoices.size() * (ch + gap) + gap;
        for (int i = 0; i < shownChoices.size(); i++) {
            int y = cy + i * (ch + gap);
            boolean hover = !waiting && mx >= cx && mx <= cx + cw && my >= y && my <= y + ch;
            String label = shownChoices.get(i);
            boolean echoChoice = label.startsWith("[Эхо]");
            int bc = hover ? SoftUi.GOLD_B : (echoChoice ? SoftUi.AQUA : SoftUi.SOFT);
            SoftUi.panel(g, cx, y, cw, ch, bc, hover ? 0xF03A3152 : (echoChoice ? 0xE6162A40 : 0xDC221D30), 1.0F);
            int numCol = waiting ? 0xFF7A7A8A : (echoChoice ? SoftUi.AQUA : SoftUi.GOLD_B);
            int tc = waiting ? 0xFF8A8A98 : (hover ? 0xFFFFF0C8 : (echoChoice ? SoftUi.AQUA_TEXT : SoftUi.CREAM));
            String num = choices.isEmpty() ? "" : (i + 1) + "  ";
            scaledText(g, Component.literal(num), cx + 12, y + 7, numCol, true, sc);
            scaledText(g, Component.literal(label), cx + 12 + this.font.width(num) * sc, y + 7, tc, true, sc);
            hit.add(new int[]{cx, y, cw, ch, i});
        }

        // основная панель
        float pulse = echo ? 0.75F + 0.25F * (float) Math.sin((age + pt) / 6.0) : 1.0F;
        SoftUi.panel(g, bx, by, boxW, boxH, SoftUi.a(border, pulse), fillCol, 1.0F);

        // табличка с именем на верхней кромке
        String name = echo ? "✦ " + speaker + " (мысль)" : speaker;
        int tabW = this.font.width(name) * sc + 24;
        int tabX = bx + 22, tabY = by - tabH / 2;
        SoftUi.panel(g, tabX, tabY, tabW, tabH, border, SoftUi.FILL_TAB, 1.0F);
        scaledText(g, Component.literal(name), tabX + 12, tabY + 5, echo ? SoftUi.AQUA : SoftUi.GOLD_B, true, sc);

        // голова собеседника в рамке
        int px = bx + 16, py = by + (boxH - PORTRAIT) / 2;     // по центру панели по высоте
        SoftUi.panel(g, px - 5, py - 5, PORTRAIT + 10, PORTRAIT + 10, border, 0xFF120F1C, 1.0F);
        if (!skin.isEmpty()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(EchoCity.MODID, "textures/entity/" + skin + ".png");
            RenderSystem.enableBlend();
            g.blit(rl, px, py, PORTRAIT, PORTRAIT, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(rl, px, py, PORTRAIT, PORTRAIT, 40.0F, 8.0F, 8, 8, 64, 64);
        } else {
            g.fill(px, py, px + PORTRAIT, py + PORTRAIT, 0xFF1D1830);
            g.pose().pushPose();
            g.pose().translate(px + 8, py + 8, 0);
            g.pose().scale(3.5F, 3.5F, 1.0F);
            g.renderItem(new ItemStack(ModItems.ECHO_SHELL.get()), 0, 0);
            g.pose().popPose();
        }

        // текст реплики с печатью по буквам
        int remaining = (int) shown;
        int ty = by + top;
        for (String line : lines) {
            int n = Math.max(0, Math.min(remaining, line.length()));
            String part = line.substring(0, n);
            remaining -= line.length();
            Component c = echo ? Component.literal(part).withStyle(ChatFormatting.ITALIC) : Component.literal(part);
            scaledText(g, c, textX, ty, textCol, true, sc);
            ty += lineH;
        }
        if (done && !exLines.isEmpty()) {
            ty += 6;
            for (String line : exLines) {
                scaledText(g, Component.literal(line).withStyle(ChatFormatting.ITALIC), textX, ty, 0xFFE9C46A, true, sc);
                ty += lineH;
            }
        }
        if (done && !choices.isEmpty())
            g.drawString(this.font, "1-9 или клик: выбрать  |  Пробел: пропустить печать  |  Esc: закрыть", bx + 4, by + boxH + 7, 0xFFB8AFC8, true);
        if (!done && (age / 10) % 2 == 0) g.drawString(this.font, "▼", bx + boxW - 20, by + boxH - 18, border, true);
    }

    private List<String> choicesOrClose(boolean echo) {
        if (!choices.isEmpty()) return choices;
        List<String> l = new ArrayList<>();
        l.add(echo ? "Закрыть" : "Закончить разговор");
        return l;
    }

    private void completeTyping() { shown = 100000.0F; }

    private void select(int index) {
        if (waiting) return;
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (choices.isEmpty() || index >= choices.size()) {
            this.onClose();
            return;
        }
        waiting = true;
        waitTicks = 0;
        if (this.minecraft.getConnection() != null) this.minecraft.getConnection().sendCommand("echocity choose " + (index + 1));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        boolean done = typingDone(wrap(text, textWidth()));
        if (!done) {
            completeTyping();
            return true;
        }
        for (int[] h : hit) {
            if (mx >= h[0] && mx <= h[0] + h[2] && my >= h[1] && my <= h[1] + h[3]) {
                select(h[4]);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean done = typingDone(wrap(text, textWidth()));
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            if (!done) completeTyping();
            else if (choices.isEmpty()) this.onClose();
            else if (choices.size() == 1) select(0);
            return true;
        }
        if (done && key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            int idx = key - GLFW.GLFW_KEY_1;
            if (choices.isEmpty() ? idx == 0 : idx < choices.size()) select(idx);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
