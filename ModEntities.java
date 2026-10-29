package com.echocity.velmar;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EchoCity.MODID);

    public static final RegistryObject<EntityType<NpcEntity>> NPC = ENTITIES.register("npc",
        () -> EntityType.Builder.<NpcEntity>of(NpcEntity::new, MobCategory.MISC)
            .sized(0.6f, 1.95f).clientTrackingRange(10).build("npc"));

    private ModEntities() {}
}
