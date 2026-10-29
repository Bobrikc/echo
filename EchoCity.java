package com.echocity.velmar;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Вельмар: Эхо колокола. Живой город, детектив с тремя+ концовками и способность слышать мысли. */
@Mod(EchoCity.MODID)
public final class EchoCity {
    public static final String MODID = "echocity";

    public EchoCity(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        modBus.addListener(EchoCity::onAttributes);
        modBus.addListener(EchoCity::onCreativeTab);

        MinecraftForge.EVENT_BUS.addListener(EchoCity::onCommands);
        MinecraftForge.EVENT_BUS.addListener(EchoCity::onClone);
        MinecraftForge.EVENT_BUS.addListener(EchoCity::onRespawn);
    }

    private static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.NPC.get(), NpcEntity.createAttributes().build());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(ModItems.ECHO_SHELL);
    }

    private static void onCommands(RegisterCommandsEvent e) {
        EchoCommands.register(e.getDispatcher());
    }

    /** Сюжетные данные игрока переживают смерть. */
    private static void onClone(PlayerEvent.Clone e) {
        PlayerState.copy(e.getOriginal(), e.getEntity());
    }

    /** Смерть = перерождение: игрок просыпается в таверне на рассвете. */
    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!e.isEndConquered() && e.getEntity() instanceof ServerPlayer sp) StoryEngine.rebirth(sp);
    }
}
