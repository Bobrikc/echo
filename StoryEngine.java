package com.echocity.velmar;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Исполняет диалоги: показывает реплики в чате с кликабельными вариантами ответа, слушает «эхо», перерождает. */
public final class StoryEngine {
    public static final int MAX_ECHO = 3;
    private static final String[] ENDINGS = {"ending_law", "ending_law_bad", "ending_street", "ending_silence", "ending_gold"};

    private StoryEngine() {}

    // ---------- разговор ----------
    public static void talk(ServerPlayer p, NpcEntity npc) {
        NpcDef d = npc.def();
        boolean night = NpcDef.isNight(p.level().getDayTime());
        PlayerState.tag(p).putString("talk_skin", npc.skinName());
        String entry = StoryData.entry(d, p, night);
        if (entry == null) {
            String line = StoryData.ambient(d);
            MutableComponent fb = Component.literal("\n[" + npc.npcName() + "] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(line).withStyle(ChatFormatting.GRAY));
            PlayerState.tag(p).putString("choices", "");
            Payload.send(p, Payload.dialogue(npc.npcName(), npc.skinName(), "n", line, "", Payload.list()), fb);
            return;
        }
        show(p, entry);
    }

    /** Игрок нажал на вариант ответа в чате. */
    public static void choose(ServerPlayer p, int index) {
        String stored = PlayerState.tag(p).getString("choices");
        String[] ids = stored.isEmpty() ? new String[0] : stored.split("\\|");
        if (index < 1 || index > ids.length) {
            p.sendSystemMessage(Component.literal("Этот выбор уже недоступен. Заговори с человеком снова.").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        PlayerState.tag(p).putString("choices", "");
        show(p, ids[index - 1]);
    }

    private static boolean allFlags(ServerPlayer p, String list) {
        if (list == null || list.isEmpty()) return true;
        for (String f : list.split(",")) if (!PlayerState.flag(p, f.trim())) return false;
        return true;
    }

    private static boolean anyFlag(ServerPlayer p, String list) {
        if (list == null || list.isEmpty()) return false;
        for (String f : list.split(",")) if (PlayerState.flag(p, f.trim())) return true;
        return false;
    }

    static void show(ServerPlayer p, String id) {
        StoryNode node = StoryData.get(id);
        if (node == null) return;

        for (String f : node.sets) PlayerState.set(p, f);
        for (String f : node.unsets) PlayerState.unset(p, f);
        if (node.givesShell && !PlayerState.flag(p, "shell_given")) {
            p.getInventory().add(new ItemStack(ModItems.ECHO_SHELL.get()));
            PlayerState.set(p, "shell_given");
            PlayerState.putInt(p, "echo", MAX_ECHO);
        }
        if (node.takesShell) {
            PlayerState.set(p, "shell_lost");
            p.getInventory().clearOrCountMatchingItems(s -> s.is(ModItems.ECHO_SHELL.get()), -1, p.inventoryMenu.getCraftSlots());
        }
        if (node.hurt > 0) p.hurt(p.damageSources().generic(), node.hurt);
        if (node.xp > 0) p.giveExperiencePoints(node.xp);
        if (!node.giveItem.isEmpty()) {
            net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(node.giveItem));
            if (it != net.minecraft.world.item.Items.AIR) p.getInventory().add(new ItemStack(it));
        }
        if (node.stamp) PlayerState.tag(p).putLong("stamp", p.level().getGameTime());

        // узлы-развилки мини-игр: показывают не себя, а то, куда ведут
        if (!node.routes.isEmpty()) {
            for (StoryNode.Route r : node.routes) {
                if (allFlags(p, r.require()) && !anyFlag(p, r.forbid())) { show(p, r.next()); return; }
            }
        }
        if (node.rollPct >= 0) {
            int pct = node.rollPct + (!node.rollBonusFlag.isEmpty() && PlayerState.flag(p, node.rollBonusFlag) ? node.rollBonusPct : 0);
            show(p, p.getRandom().nextInt(100) < pct ? node.rollOk : node.rollFail);
            return;
        }
        if (node.timedMin >= 0) {
            long dt = p.level().getGameTime() - PlayerState.tag(p).getLong("stamp");
            show(p, dt < node.timedMin ? node.timedEarly : dt <= node.timedMax ? node.timedOk : node.timedLate);
            return;
        }

        if (node.endTitle != null) {
            PlayerState.tag(p).putString("choices", "");
            Cutscenes.playEnding(p, node);
            return;
        }

        boolean narrator = node.speaker.equals("Рассказчик");
        String skin = narrator ? "" : PlayerState.tag(p).getString("talk_skin");

        // читаемая версия на случай, если у клиента не сработает окно диалога
        MutableComponent body = Component.empty();
        body.append(Component.literal("\n[" + node.speaker + "]").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (String line : node.text.split("\n")) body.append(Component.literal("\n" + line).withStyle(ChatFormatting.WHITE));
        StringBuilder extra = new StringBuilder();
        for (StoryNode.Extra e : node.extras) {
            if (!PlayerState.flag(p, e.flag())) continue;
            body.append(Component.literal("\n" + e.text()).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
            if (extra.length() > 0) extra.append('\n');
            extra.append(e.text());
        }

        List<String> offered = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (StoryNode.Choice c : node.choices) {
            if (!allFlags(p, c.require()) || anyFlag(p, c.forbid())) continue;
            offered.add(c.next());
            labels.add(c.label());
            int n = offered.size();
            boolean echoChoice = c.label().startsWith("[Эхо]");
            body.append(Component.literal("\n  ➤ " + n + ". " + c.label()).withStyle(s -> s
                .withColor(echoChoice ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/echocity choose " + n))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Нажми, чтобы ответить")))));
        }
        PlayerState.tag(p).putString("choices", String.join("|", offered));
        if (!node.gameType.isEmpty()) {
            PlayerState.tag(p).putString("game", node.gameOk + "|" + node.gameFail + "|" + node.gameQuit);
            boolean assist = !node.gameAssistFlag.isEmpty() && PlayerState.flag(p, node.gameAssistFlag);
            Payload.send(p, Payload.game(node.gameType, node.gameTitle, node.gameIntro, assist), body);
            return;
        }
        Payload.send(p, Payload.dialogue(node.speaker, skin, narrator ? "s" : "n", node.text, extra.toString(), labels), body);
    }

    /** Результат мини-игры с экрана клиента: win, lose или quit. */
    public static void game(ServerPlayer p, String res) {
        String g = PlayerState.tag(p).getString("game");
        if (g.isEmpty()) return;
        PlayerState.tag(p).putString("game", "");
        PlayerState.tag(p).putString("choices", "");
        String[] a = (g + "|||").split("\\|", -1);
        String next = res.equals("win") ? a[0] : res.equals("lose") ? a[1] : a[2];
        if (!next.isEmpty()) show(p, next);
    }

    /** Итог концовки: текст сюжета в чате, титры и счётчик найденных концовок (вызывается после катсцены). */
    public static void finishEnding(ServerPlayer p, StoryNode node) {
        int found = 0;
        for (String e : ENDINGS) if (PlayerState.flag(p, e)) found++;
        p.sendSystemMessage(Component.literal("\n[" + node.speaker + "]").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (String line : node.text.split("\n")) p.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.WHITE));
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 30));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(node.endTitle).withStyle(ChatFormatting.GOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(node.endSubtitle).withStyle(ChatFormatting.GRAY)));
        p.sendSystemMessage(Component.literal("★ КОНЦОВКА: " + node.endTitle + " (найдено " + found + " из " + ENDINGS.length + ")")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        p.sendSystemMessage(Component.literal("Чтобы прожить Вельмар заново, напиши /echocity reset").withStyle(ChatFormatting.GRAY));
    }

    // ---------- эхо ----------
    private static void refillIfNewDay(ServerPlayer p) {
        long day = p.level().getDayTime() / 24000L + 1;
        if (PlayerState.getInt(p, "echo_day") != (int) day) {
            PlayerState.putInt(p, "echo", MAX_ECHO);
            PlayerState.putInt(p, "echo_day", (int) day);
        }
    }

    public static void listen(ServerPlayer p, NpcEntity npc) {
        NpcDef d = npc.def();
        refillIfNewDay(p);
        boolean story = StoryData.isStoryNpc(d);
        boolean heard = story && PlayerState.flag(p, "heard_" + d.id);
        if (story && !heard) {
            int echo = PlayerState.getInt(p, "echo");
            if (echo <= 0) {
                p.displayClientMessage(Component.literal("Эхо молчит. Дождись рассвета.").withStyle(ChatFormatting.DARK_PURPLE), true);
                return;
            }
            PlayerState.putInt(p, "echo", echo - 1);
            for (String f : StoryData.thoughtFlags(d)) PlayerState.set(p, f);
        }
        String thought = StoryData.thought(d);
        MutableComponent fb = Component.literal("\n✦ Эхо (" + npc.npcName() + "): ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
            .append(Component.literal(thought).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        String charges = "Заряды эха: " + PlayerState.getInt(p, "echo") + "/" + MAX_ECHO;
        Payload.send(p, Payload.bubble(npc.npcName(), npc.skinName(), thought, true), fb);
        p.displayClientMessage(Component.literal(charges).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    // ---------- перерождение ----------
    public static boolean hasShell(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)
            if (p.getInventory().getItem(i).is(ModItems.ECHO_SHELL.get())) return true;
        return false;
    }

    public static void rebirth(ServerPlayer p) {
        if (!PlayerState.hasCity(p)) return;
        ServerLevel lvl = p.serverLevel();
        if (lvl.dimension() != Level.OVERWORLD) return;
        PlayerState.putInt(p, "deaths", PlayerState.getInt(p, "deaths") + 1);
        long next = (lvl.getDayTime() / 24000L + 1) * 24000L + 200L;
        lvl.setDayTime(next);
        PlayerState.putInt(p, "echo", MAX_ECHO);
        PlayerState.putInt(p, "echo_day", (int) (next / 24000L) + 1);
        PlayerState.set(p, "reborn_new");
        if (PlayerState.getInt(p, "deaths") >= 3) PlayerState.set(p, "reborn_many");
        BlockPos o = PlayerState.origin(p);
        p.teleportTo(lvl, o.getX() - 15 + 0.5, o.getY(), o.getZ() - 9 + 0.5, 0f, 0f);
        if (PlayerState.flag(p, "has_shell") && !PlayerState.flag(p, "shell_lost") && !hasShell(p))
            p.getInventory().add(new ItemStack(ModItems.ECHO_SHELL.get()));
        int n = PlayerState.getInt(p, "deaths");
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Новое утро").withStyle(ChatFormatting.AQUA)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Перерождение №" + n + ". Колокол вернул тебя в таверну.").withStyle(ChatFormatting.GRAY)));
    }
}
