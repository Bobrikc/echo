package com.echocity.velmar;

import java.util.Random;

/** Описание жителей: имя, скин и суточное расписание (смещения от центра площади: {dx, dz}). */
public enum NpcDef {
    //            id        имя                скин       утро         день         вечер        ночь
    TAVI  ("tavi",   "Тави",           "tavi",   p(-15,-9), p(-4,2),   p(-12,-8), p(-15,-9)),
    NUNA  ("nuna",   "Бабушка Нуна",   "nuna",   p(12,-9),  p(14,-6),  p(2,2),    p(0,-14)),
    GARTH ("garth",  "Капитан Гарт",   "garth",  p(-15,12), p(0,6),    p(10,0),   p(-15,12)),
    ORSEN ("orsen",  "Мастер Орсен",   "orsen",  p(13,14),  p(10,-8),  p(13,14),  p(13,14)),
    MIRA  ("mira",   "Мира",           "mira",   p(30,18),  p(26,6),   p(-6,-2),  p(26,6)),
    BROK  ("brok",   "Брок",           "brok",   p(26,2),   p(26,4),   p(26,6),   p(26,8)),
    HVAT  ("hvat",   "Хват",           "thug",   p(25,0),   p(27,1),   p(25,9),   p(27,10)),
    RZHAVY("rzhavy", "Ржавый",         "thug",   p(27,5),   p(25,6),   p(27,3),   p(25,2)),
    DORN  ("dorn",   "Кузнец Дорн",   "dorn",   p(-36,12), p(-36,12), p(-14,-12), p(-14,-10)),
    LEYA  ("leya",   "Пекарша Лея",   "leya",   p(-36,-12),p(-36,-12),p(10,-6),   p(-36,-12)),
    OSIP  ("osip",   "Рыбак Осип",    "osip",   p(51,12),  p(51,12),  p(-17,-8),  p(-17,-8)),
    FOMA  ("foma",   "Писарь Фома",   "foma",   p(9,12),   p(9,12),   p(9,12),    p(9,12)),
    TIM   ("tim",    "Тим",           "tim",    p(0,0),    p(0,0),    p(0,0),     p(0,0)),
    GATE_N("guard_n","Стражник",      "guard",  p(0,-93),  p(0,-93),  p(0,-93),   p(0,-93)),
    GATE_S("guard_s","Стражник",      "guard",  p(0,93),   p(0,93),   p(0,93),    p(0,93)),
    GATE_E("guard_e","Стражник",      "guard",  p(93,0),   p(93,0),   p(93,0),    p(93,0)),
    GATE_W("guard_w","Стражник",      "guard",  p(-93,0),  p(-93,0),  p(-93,0),   p(-93,0)),
    PATROL_A("guard_a","Стражник",    "guard",  p(-20,3),  p(20,3),   p(30,-3),   p(-30,3)),
    PATROL_B("guard_b","Стражник",    "guard",  p(20,-3),  p(-20,-3), p(-30,3),   p(30,-3)),
    CITIZEN("citizen", "Горожанин",    "citizen1", p(0,0),  p(0,0),    p(0,0),    p(0,0));

    public final String id, name, skin;
    private final int[][] sched;

    NpcDef(String id, String name, String skin, int[]... sched) {
        this.id = id; this.name = name; this.skin = skin; this.sched = sched;
    }

    private static int[] p(int dx, int dz) { return new int[]{dx, dz}; }

    public boolean wanders() { return this == CITIZEN || this == TIM; }

    public static NpcDef byId(String id) {
        for (NpcDef d : values()) if (d.id.equals(id)) return d;
        return CITIZEN;
    }

    /** 0 утро, 1 день, 2 вечер, 3 ночь. */
    public static int slot(long dayTime) {
        long t = dayTime % 24000L;
        if (t < 6000) return 0;
        if (t < 12000) return 1;
        if (t < 15000) return 2;
        if (t < 23000) return 3;
        return 0;
    }

    public static boolean isNight(long dayTime) { return slot(dayTime) == 3; }

    public int[] waypoint(long dayTime) { return sched[slot(dayTime)]; }

    /** Точки на улицах, между которыми бродят горожане. */
    private static final int[][] STREET = {
        {0,0},{8,0},{-8,0},{16,0},{-16,0},{0,10},{0,-8},{0,18},{12,2},{-12,-2},{2,-12},
        {-30,0},{30,0},{44,0},{-44,0},{0,30},{0,-30},{0,45},{0,-45},{46,12},{-46,-12},{46,-30},{-46,30},
        {20,40},{-20,-40},{-30,-40},{30,40},
        {-78,0},{78,0},{0,60},{0,-60},{0,80},{0,-80},{46,56},{-46,-56},{78,30},{-78,-30},
        {20,72},{-20,-72},{-60,40},{60,-40},{-60,-72},{60,72},{90,0},{-90,0},{0,90},{0,-90},
        {-78,60},{78,-60},{-46,80},{46,-80},{-20,40},{20,-40},{-60,-40},{60,40}};
    private static final int[][] TAVERN = {{-15,-9},{-13,-12},{-17,-8},{-12,-8}};

    /** Выбирает точку прогулки не слишком далеко от текущей позиции (cx, cz — относительно центра площади). */
    public static int[] citizenSpot(long dayTime, Random r, int cx, int cz) {
        int[][] set = slot(dayTime) == 3 ? TAVERN : STREET;
        if (set == TAVERN) return set[r.nextInt(set.length)];
        for (int tries = 0; tries < 12; tries++) {
            int[] s = set[r.nextInt(set.length)];
            if (Math.abs(s[0] - cx) + Math.abs(s[1] - cz) <= 70) return s;
        }
        return set[r.nextInt(set.length)];
    }

    public static int[] citizenSpot(long dayTime, Random r) {
        int[][] set = slot(dayTime) == 3 ? TAVERN : STREET;
        return set[r.nextInt(set.length)];
    }
}
