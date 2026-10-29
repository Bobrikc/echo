package com.echocity.velmar;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Реплика вне диалога: короткие слова прохожих и мысли, услышанные эхом.
 * Появляется над панелью предметов: голова, имя и текст в мягкой рамке, затем плавно гаснет.
 */
@Mod.EventBusSubscriber(modid = EchoCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BubbleHud {
    private static String name = "", skin = "", text = "";
    private static boolean echo = false;
    private static long start = 0, showMs = 5000;
    private static final long FADE_MS = 450;

    private BubbleHud() {}

    /** Поля: 0 тип, 1 имя, 2 скин, 3 текст, 4 режим (n или e). */
    static void show(String[] f) {
        name = f.length > 1 ? f[1] : "";
        skin = f.length > 2 ? f[2] : "";
        text = f.length > 3 ? f[3] : "";
        echo = f.length > 4 && f[4].equals("e");
        start = System.currentTimeMillis();
        showMs = Mth.clamp(4000L + text.length() * 55L, 4500L, 14000L);
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post e) {
        if (text.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui) return;
        long age = System.currentTimeMillis() - start;
        if (age > showMs) { text = ""; return; }
        float k = age < 250 ? age / 250F : age > showMs - FADE_MS ? (showMs - age) / (float) FADE_MS : 1F;
        k = Mth.clamp(k, 0.02F, 1F);

        GuiGraphics g = e.getGuiGraphics();
        int sw = g.guiWidth(), sh = g.guiHeight();
        int maxText = Math.min(sw - 120, 320);
        List<String> lines = new ArrayList<>();
        for (FormattedText ft : mc.font.getSplitter().splitLines(text, maxText, Style.EMPTY)) lines.add(ft.getString());
        int tw = 0;
        for (String l : lines) tw = Math.max(tw, mc.font.width(l));
        String title = echo ? "✦ " + name : name;
        tw = Math.max(tw, mc.font.width(title));
        int w = tw + 62, h = Math.max(36, 14 + 11 + lines.size() * 11 + 6);
        int x = (sw - w) / 2, y = sh - 74 - h + 14 - (int) ((1F - Math.min(1F, k * 4F)) * 6F);

        int border = echo ? SoftUi.AQUA : SoftUi.GOLD;
        SoftUi.panel(g, x, y, w, h, border, echo ? SoftUi.FILL_ECHO : SoftUi.FILL, k);
        if (!skin.isEmpty()) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(EchoCity.MODID, "textures/entity/" + skin + ".png");
            int fy = y + (h - 28) / 2;                       // по центру рамки по высоте
            SoftUi.panel(g, x + 8, fy, 28, 28, border, 0xFF120F1C, k);
            g.blit(rl, x + 10, fy + 2, 24, 24, 8.0F, 8.0F, 8, 8, 64, 64);
            g.blit(rl, x + 10, fy + 2, 24, 24, 40.0F, 8.0F, 8, 8, 64, 64);
        }
        int tx = x + 46, ty = y + 9;
        g.drawString(mc.font, title, tx, ty, SoftUi.a(echo ? SoftUi.AQUA : SoftUi.GOLD_B, k), true);
        ty += 12;
        for (String l : lines) {
            Component c = echo ? Component.literal(l).withStyle(net.minecraft.ChatFormatting.ITALIC) : Component.literal(l);
            g.drawString(mc.font, c, tx, ty, SoftUi.a(echo ? SoftUi.AQUA_TEXT : SoftUi.CREAM, k), true);
            ty += 11;
        }
    }
}
