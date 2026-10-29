package com.echocity.velmar;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Оформление катсцены: чёрные полосы сверху и снизу, крупный заголовок и плавные титры. */
public final class CutsceneScreen extends Screen {
    private final List<String[]> caps = new ArrayList<>();   // заголовок, подпись, длительность
    private int age = 0;

    public CutsceneScreen(String[] f) {
        super(Component.empty());
        for (int i = 1; i + 2 < f.length; i += 3) caps.add(new String[]{f[i], f[i + 1], f[i + 2]});
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) { /* мир остаётся видимым */ }

    @Override
    public void tick() { age++; }

    private static int argb(float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        return (a << 24) | 0x00FFFFFF;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float grow = Mth.clamp((age + pt) / 20.0F, 0.0F, 1.0F);
        int bar = (int) (this.height * 0.13F * grow);
        g.fill(0, 0, this.width, bar, 0xFF000000);
        g.fill(0, this.height - bar, this.width, this.height, 0xFF000000);

        // выбираем титр по времени
        int acc = 0;
        String[] cur = null;
        int local = 0, dur = 1;
        for (String[] c : caps) {
            int d;
            try { d = Integer.parseInt(c[2]); } catch (NumberFormatException e) { d = 100; }
            if (age < acc + d) { cur = c; local = age - acc; dur = d; break; }
            acc += d;
        }
        if (cur != null) {
            float fade = Math.min(Mth.clamp(local / 14.0F, 0.0F, 1.0F), Mth.clamp((dur - local) / 14.0F, 0.0F, 1.0F));
            int color = argb(fade);
            if (((color >>> 24) & 0xFF) > 8) {
                int y;
                if (!cur[0].isEmpty()) {
                    y = (int) (this.height * 0.38F);
                    g.pose().pushPose();
                    g.pose().scale(3.0F, 3.0F, 1.0F);
                    g.drawCenteredString(this.font, cur[0], (int) (this.width / 6.0F), (int) (y / 3.0F), color);
                    g.pose().popPose();
                    y += 36;
                } else {
                    y = this.height - bar - 14 - 12 * 3;
                }
                int w = Math.min(this.width - 120, 520);
                for (FormattedText ft : this.font.getSplitter().splitLines(cur[1], w, Style.EMPTY)) {
                    String line = ft.getString();
                    g.drawString(this.font, line, (this.width - this.font.width(line)) / 2, y, color, true);
                    y += 12;
                }
            }
        }
        if (age < 14) g.fill(0, 0, this.width, this.height, ((int) ((1.0F - age / 14.0F) * 255.0F) << 24));  // появление из темноты
        g.drawString(this.font, "Пробел: пропустить", this.width - this.font.width("Пробел: пропустить") - 8,
            this.height - bar + Math.max(2, (bar - 9) / 2), 0xFF777777, false);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            if (this.minecraft.getConnection() != null) this.minecraft.getConnection().sendCommand("echocity skip");
            return true;
        }
        return key == GLFW.GLFW_KEY_ESCAPE;   // Esc не закрывает катсцену
    }
}
