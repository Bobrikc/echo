package com.echocity.velmar;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Клиент перехватывает служебные сообщения мода и вместо чата показывает окна диалога и катсцен. */
@Mod.EventBusSubscriber(modid = EchoCity.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    private static String findPayload(Component c) {
        String ins = c.getStyle().getInsertion();
        if (ins != null && ins.startsWith(Payload.PREFIX)) return ins;
        for (Component sibling : c.getSiblings()) {
            String r = findPayload(sibling);
            if (r != null) return r;
        }
        return null;
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent event) {
        String payload = findPayload(event.getMessage());
        if (payload == null) return;
        String[] f = Payload.parse(payload);
        if (f == null || f.length == 0) return;
        event.setCanceled(true);
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> handle(mc, f));
    }

    private static void handle(Minecraft mc, String[] f) {
        switch (f[0]) {
            case "D" -> {
                if (mc.screen instanceof DialogueScreen ds) ds.apply(f);
                else mc.setScreen(new DialogueScreen(f));
            }
            case "C" -> mc.setScreen(new CutsceneScreen(f));
            case "B" -> BubbleHud.show(f);
            case "G" -> mc.setScreen(new MiniGameScreen(f));
            case "X" -> {
                if (mc.screen instanceof CutsceneScreen) mc.setScreen(null);
            }
            default -> { }
        }
    }
}
