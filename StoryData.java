package com.echocity.velmar;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.server.level.ServerPlayer;

/** Реестр сюжетных реплик + выбор стартовой реплики по состоянию игрока. */
public final class StoryData {
    public static final Map<String, StoryNode> NODES = new HashMap<>();
    private static final Random RNG = new Random();

    static StoryNode n(String id, String speaker, String text) {
        StoryNode node = new StoryNode(id, speaker, text);
        NODES.put(id, node);
        return node;
    }

    static {
        StoryTown.register();
        StoryStreet.register();
        StoryMore.register();
        StoryGames.register();
    }

    public static StoryNode get(String id) { return NODES.get(id); }

    private static boolean f(ServerPlayer p, String flag) { return PlayerState.flag(p, flag); }

    public static boolean finished(ServerPlayer p) { return f(p, "finished"); }

    /** Какая реплика открывается, когда игрок заговорил с NPC. null — у NPC нет сюжетной реплики. */
    public static String entry(NpcDef d, ServerPlayer p, boolean night) {
        switch (d) {
            case TAVI:
                if (finished(p)) return "tavi_epilogue";
                if (!f(p, "intro_done")) return "tavi_intro";
                if (f(p, "reborn_new")) return "tavi_reborn";
                if (f(p, "has_ledger")) return "tavi_ledger";
                return "tavi_hub";
            case NUNA:
                if (finished(p)) return "nuna_after";
                if (night && f(p, "nuna_met")) return "nuna_tower";
                return f(p, "nuna_met") ? "nuna_hub" : "nuna_intro";
            case GARTH:
                if (finished(p)) return "garth_after";
                if (f(p, "has_ledger")) return "garth_ledger";
                return f(p, "garth_met") ? "garth_hub" : "garth_intro";
            case ORSEN:
                if (finished(p)) return "orsen_after";
                return f(p, "orsen_met") ? "orsen_hub" : "orsen_intro";
            case MIRA:
                if (finished(p)) return "mira_after";
                return f(p, "mira_met") ? "mira_hub" : "mira_intro";
            case BROK:
                if (finished(p)) return "brok_after";
                return f(p, "gang_met") ? "brok_hub" : "brok_intro";
            case DORN: return "dorn_hub";
            case LEYA: return "leya_hub";
            case OSIP: return "osip_hub";
            case FOMA: return "foma_hub";
            case TIM: return "tim_hub";
            case GATE_N: case GATE_S: case GATE_E: case GATE_W: case PATROL_A: case PATROL_B: return "guard_hub";
            case HVAT: return "thug_hvat";
            case RZHAVY: return "thug_rzhavy";
            default: return null;
        }
    }

    /** Скрытая мысль, которую слышно через раковину. */
    public static String thought(NpcDef d) {
        switch (d) {
            case TAVI: return "«Только бы он не ушёл, как все. Я так устала хоронить надежду».";
            case NUNA: return "«Зерно не пропало. Оно спит под колоколом. Я сама таскала туда мешки по ночам... для Орсена. Мне стыдно».";
            case GARTH: return "«Орсен держит мой долг. Если он падёт — падёт и моя честь. Закон... или мой кошель?»";
            case ORSEN: return "«Если книга всплывёт, меня повесят. Зерно в башне, ключ на моей шее. Ещё две недели голода — и цена удвоится».";
            case MIRA: return "«Я украла книгу не ради золота. Квартал голодает, а доказательство у меня. Только бы не опоздать».";
            case BROK: return "«Мне заплатили, я не знал за что. Человек в красном. Мира меня убьёт, если узнает, что я брал его монеты».";
            case HVAT: return "«Какой же я голодный».";
            case RZHAVY: return "«Нож тупой, духу нет. Лишь бы не пришлось пускать его в ход».";
            case DORN: return "«Если бы я знал, откуда у Орсена такие запасы железа и зерна... Лучше не знать».";
            case LEYA: return "«Я пекла бы вдвое больше, если бы мне давали муку. Но мука идёт не мне».";
            case OSIP: return "«Рыба давно ушла. Видно, и ей страшно в нашем городе».";
            case FOMA: return "«Цифры не сходятся. Я это знаю. И никому не скажу. Страшно».";
            case TIM: return "«Хочу стать стражником. Или вором. Или и тем и другим».";
            case GATE_N: case GATE_S: case GATE_E: case GATE_W: case PATROL_A: case PATROL_B:
                return "«Жалованье задерживают второй месяц. Долго ли я буду стоять тут за обещания?»";
            default:
                String[] c = {
                    "«Опять пустой горшок на ужин. Дети не плачут — это хуже».",
                    "«Колокол звонил ночью. Я слышала. Никто не поднимался на башню».",
                    "«Хоть бы цена на хлеб упала. Хоть на медяк».",
                    "«Орсен снова смотрел на меня так, будто я должна ему жизнь».",
                    "«Если сегодня обойдётся без драки, я зажгу свечу».",
                    "«Этот чужак с раковиной... ему можно верить? Или нет?»",
                    "«Мне бы только дожить до весны».",
                    "«Ночью опять слышала звон. Это не к добру».",
                    "«Хлеб дорог, а жалованье — прежнее».",
                    "«Сын просит рассказать про Мирину шайку. Что я ему скажу?»"};
                return c[RNG.nextInt(c.length)];
        }
    }

    /** Что слышно сверх того, что мысль уже говорит: флаги, которые открывает прослушивание. */
    public static String[] thoughtFlags(NpcDef d) {
        switch (d) {
            case NUNA: return new String[]{"heard_nuna", "tower_clue"};
            case ORSEN: return new String[]{"heard_orsen", "knows_hoard"};
            case GARTH: return new String[]{"heard_garth", "knows_debt"};
            case MIRA: return new String[]{"heard_mira", "knows_mira_honest"};
            case BROK: return new String[]{"heard_brok", "knows_brok_paid"};
            case TAVI: return new String[]{"heard_tavi"};
            default: return new String[0];
        }
    }

    public static boolean isStoryNpc(NpcDef d) {
        return d == NpcDef.TAVI || d == NpcDef.NUNA || d == NpcDef.GARTH || d == NpcDef.ORSEN || d == NpcDef.MIRA || d == NpcDef.BROK;
    }

    /** Фоновые реплики (над панелью), чтобы город казался живым. */
    public static String ambient(NpcDef d) {
        switch (d) {
            case TAVI: return "Тави протирает кружку и поглядывает на дверь.";
            case NUNA: return "Нуна бормочет: «Колокол к беде… колокол к беде…»";
            case GARTH: return "Гарт проверяет ремень: «Расходитесь, не толпитесь».";
            case ORSEN: return "Орсен перебирает ключи на шее.";
            case MIRA: return "Мира оглядывается по сторонам и прячет руки в рукава.";
            case BROK: return "Брок цедит: «Проход платный».";
            case HVAT: return "Хват хрустит костяшками.";
            case RZHAVY: return "Ржавый ковыряет ножом стену.";
            case DORN: return "Дорн стучит молотом: «Тинь-тинь».";
            case LEYA: return "Лея смахивает муку с фартука: «Свежие лепёшки!»";
            case OSIP: return "Осип смотрит на поплавок и вздыхает.";
            case FOMA: return "Фома скрипит пером: «Тише, я считаю».";
            case TIM: return "Тим подпрыгивает: «Я бегу с поручением!»";
            case GATE_N: case GATE_S: case GATE_E: case GATE_W: case PATROL_A: case PATROL_B:
                return "Стражник бдительно оглядывает улицу.";
            default:
                String[] a = {
                    "Прохожий вздыхает: «Опять колокол звонил в полночь».",
                    "Женщина шепчет соседке: «Цены на хлеб — как на золото».",
                    "Ребёнок спрашивает: «А правда, что у чужака поющая раковина?»",
                    "Торговец ворчит: «Гильдия снова закрыла амбары».",
                    "Кто-то бормочет: «Не доведёт нас Орсен до добра...»",
                    "Старик кивает: «В Вельмаре даже стены слушают».",
                    "Мальчишка с хохотом убегает от собаки.",
                    "Прачка напевает: «Ой, колокол, колокол...»",
                    "Двое спорят у колодца о цене на хлеб.",
                    "Женщина несёт корзину: «Дорогу, дорогу!»",
                    "Кто-то шепчет: «Говорят, ночью у башни видели Миру».",
                    "Подмастерье зевает: «Опять не выспался. Колокол».",
                    "Старуха крестится и косится на башню."};
                return a[RNG.nextInt(a.length)];
        }
    }
}
