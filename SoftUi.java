package com.echocity.velmar;

import net.minecraft.client.gui.GuiGraphics;

/** Мягкий стиль окон мода: тёмно-сливовая панель, тёплая кремово-золотая рамка со скруглёнными углами и мягкой тенью. */
final class SoftUi {
    static final int GOLD = 0xFFE3CE97, GOLD_B = 0xFFFFD27A, CREAM = 0xFFF4EBDD, SOFT = 0xFF6E6486;
    static final int FILL = 0xEC1C1828, FILL_ECHO = 0xEC122033, FILL_TAB = 0xF02A2236;
    static final int AQUA = 0xFFA9E8FF, AQUA_TEXT = 0xFFD2F3FF;

    private SoftUi() {}

    static int a(int c, float k) {
        int al = (int) (((c >>> 24) & 0xFF) * k);
        return (al << 24) | (c & 0x00FFFFFF);
    }

    /** Закруглённая заливка (углы срезаны в пиксель-арт стиле), строки не пересекаются, чтобы полупрозрачность не удваивалась. */
    static void rfill(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x + 2, y, x + w - 2, y + 1, c);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, c);
        g.fill(x, y + 2, x + w, y + h - 2, c);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, c);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, c);
    }

    static void routline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x + 2, y, x + w - 2, y + 1, c);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, c);
        g.fill(x, y + 2, x + 1, y + h - 2, c);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, c);
        g.fill(x + 1, y + 1, x + 2, y + 2, c);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, c);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, c);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, c);
    }

    /** k — общая прозрачность (для плавного появления и затухания). */
    static void panel(GuiGraphics g, int x, int y, int w, int h, int border, int fill, float k) {
        rfill(g, x + 1, y + 3, w, h, a(0x55000000, k));                      // мягкая тень
        rfill(g, x, y, w, h, a(fill, k));
        g.fillGradient(x + 3, y + 2, x + w - 3, y + Math.min(h / 2, 24), a(0x18FFFFFF, k), 0x00FFFFFF);   // тёплый отсвет сверху
        routline(g, x, y, w, h, a(border, k));
        if (w > 12 && h > 12) routline(g, x + 3, y + 3, w - 6, h - 6, a((border & 0x00FFFFFF) | 0x55000000, k));
        if (h >= 36) {                                                                  // «заклёпки» в углах внутренней рамки
            g.fill(x + 6, y + 6, x + 8, y + 8, a(border, k)); g.fill(x + w - 8, y + 6, x + w - 6, y + 8, a(border, k));
            g.fill(x + 6, y + h - 8, x + 8, y + h - 6, a(border, k)); g.fill(x + w - 8, y + h - 8, x + w - 6, y + h - 6, a(border, k));
        }
    }
}
