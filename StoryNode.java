package com.echocity.velmar;

import java.util.ArrayList;
import java.util.List;

/** Одна реплика сюжета: текст, условные добавки, эффекты (флаги, урон) и варианты ответа. */
public final class StoryNode {
    public record Choice(String label, String next, String require, String forbid) {}
    public record Extra(String flag, String text) {}

    public final String id, speaker, text;
    public final List<Extra> extras = new ArrayList<>();
    public final List<Choice> choices = new ArrayList<>();
    public final List<String> sets = new ArrayList<>();
    public final List<String> unsets = new ArrayList<>();
    public String endTitle = null;     // если не null — это концовка
    public String endSubtitle = "";
    public int hurt = 0;
    public boolean givesShell = false;
    public boolean takesShell = false;
    // --- мини-игры ---
    public record Route(String require, String forbid, String next) {}
    public final List<Route> routes = new ArrayList<>();       // узел-развилка: первый подходящий переход
    public int rollPct = -1;                                   // узел-кубик: шанс успеха в процентах
    public String rollBonusFlag = "";
    public int rollBonusPct = 0;
    public String rollOk = "", rollFail = "";
    public boolean stamp = false;                              // запомнить время (для игр на реакцию)
    public int timedMin = -1, timedMax = -1;                   // узел-таймер: окно в тиках с момента stamp
    public String timedOk = "", timedEarly = "", timedLate = "";
    public int xp = 0;
    public String giveItem = "";
    public String gameType = "", gameTitle = "", gameIntro = "", gameOk = "", gameFail = "", gameQuit = "", gameAssistFlag = "";

    public StoryNode(String id, String speaker, String text) { this.id = id; this.speaker = speaker; this.text = text; }

    public StoryNode set(String... f) { sets.addAll(List.of(f)); return this; }
    public StoryNode unset(String... f) { unsets.addAll(List.of(f)); return this; }
    public StoryNode also(String flag, String t) { extras.add(new Extra(flag, t)); return this; }
    public StoryNode go(String label, String next) { choices.add(new Choice(label, next, "", "")); return this; }
    public StoryNode goIf(String req, String label, String next) { choices.add(new Choice(label, next, req, "")); return this; }
    public StoryNode goUnless(String forbid, String label, String next) { choices.add(new Choice(label, next, "", forbid)); return this; }
    public StoryNode goWhen(String req, String forbid, String label, String next) { choices.add(new Choice(label, next, req, forbid)); return this; }
    public StoryNode hurt(int h) { this.hurt = h; return this; }
    public StoryNode shell() { this.givesShell = true; return this; }
    public StoryNode loseShell() { this.takesShell = true; return this; }
    public StoryNode end(String title, String subtitle) { this.endTitle = title; this.endSubtitle = subtitle; return this; }

    public StoryNode route(String req, String forbid, String next) { routes.add(new Route(req, forbid, next)); return this; }
    public StoryNode roll(int pct, String bonusFlag, int bonusPct, String ok, String fail) {
        rollPct = pct; rollBonusFlag = bonusFlag; rollBonusPct = bonusPct; rollOk = ok; rollFail = fail; return this;
    }
    public StoryNode stamp() { this.stamp = true; return this; }
    public StoryNode timed(int minTicks, int maxTicks, String early, String ok, String late) {
        timedMin = minTicks; timedMax = maxTicks; timedEarly = early; timedOk = ok; timedLate = late; return this;
    }
    public StoryNode xp(int v) { this.xp = v; return this; }
    public StoryNode give(String itemId) { this.giveItem = itemId; return this; }
    /** Отдельная мини-игра с собственным экраном. Варианты ответа узла служат запасным вариантом в чате. */
    public StoryNode game(String type, String title, String intro, String ok, String fail, String quit, String assistFlag) {
        gameType = type; gameTitle = title; gameIntro = intro; gameOk = ok; gameFail = fail; gameQuit = quit; gameAssistFlag = assistFlag; return this;
    }
}
