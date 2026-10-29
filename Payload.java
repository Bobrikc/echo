package com.echocity.velmar;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Передача данных мода клиенту обычными сообщениями чата.
 * Данные прячутся в невидимой «вставке» (insertion) стиля первого пустого элемента сообщения.
 * Клиент с модом перехватывает такое сообщение и открывает красивое окно, а без мода (или если
 * перехват не сработал) игрок видит обычный читаемый текст с кликабельными вариантами.
 */
public final class Payload {
    public static final char SEP = '\u001f';
    public static final String PREFIX = "echocity>";

    private Payload() {}

    /** Окно диалога: mode = n (обычный), e (мысль, услышанная эхом), s (рассказчик). */
    public static String dialogue(String speaker, String skin, String mode, String text, String extra, List<String> choices) {
        StringBuilder sb = new StringBuilder(PREFIX).append('D');
        sb.append(SEP).append(clean(speaker)).append(SEP).append(clean(skin)).append(SEP).append(clean(mode))
          .append(SEP).append(clean(text)).append(SEP).append(clean(extra));
        for (String c : choices) sb.append(SEP).append(clean(c));
        return sb.toString();
    }

    /** Катсцена: список титров (заголовок, подзаголовок, длительность в тиках). */
    public static String cutscene(List<String[]> captions) {
        StringBuilder sb = new StringBuilder(PREFIX).append('C');
        for (String[] c : captions) sb.append(SEP).append(clean(c[0])).append(SEP).append(clean(c[1])).append(SEP).append(c[2]);
        return sb.toString();
    }

    /** Мини-игра: type = fish | cups | pick | forge, assist = 1, если эхо или доверие облегчают игру. */
    public static String game(String type, String title, String intro, boolean assist) {
        return PREFIX + 'G' + SEP + clean(type) + SEP + clean(title) + SEP + clean(intro) + SEP + (assist ? "1" : "0");
    }

    /** Короткая реплика над панелью предметов. */
    public static String bubble(String speaker, String skin, String text, boolean echo) {
        return PREFIX + 'B' + SEP + clean(speaker) + SEP + clean(skin) + SEP + clean(text) + SEP + (echo ? "e" : "n");
    }

    public static String close() { return PREFIX + "X"; }

    private static String clean(String s) { return s == null ? "" : s.replace(SEP, ' '); }

    /** Возвращает поля сообщения или null, если строка не относится к моду. Поле 0 — тип: D, C или X. */
    public static String[] parse(String insertion) {
        if (insertion == null || !insertion.startsWith(PREFIX)) return null;
        return insertion.substring(PREFIX.length()).split(String.valueOf(SEP), -1);
    }

    /** Сообщение: невидимая метка с данными + читаемый запасной текст. */
    public static MutableComponent wrap(String payload, Component fallback) {
        MutableComponent root = Component.empty();
        root.append(Component.literal("").withStyle(s -> s.withInsertion(payload)));
        if (fallback != null) root.append(fallback);
        return root;
    }

    public static void send(ServerPlayer p, String payload, Component fallback) {
        p.sendSystemMessage(wrap(payload, fallback));
    }

    public static List<String> list(String... a) {
        List<String> l = new ArrayList<>();
        for (String s : a) l.add(s);
        return l;
    }
}
