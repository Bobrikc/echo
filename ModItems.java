package com.echocity.velmar;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EchoCity.MODID);

    /** Раковина эха: Shift + ПКМ по человеку, чтобы услышать его скрытую мысль. */
    public static final RegistryObject<Item> ECHO_SHELL = ITEMS.register("echo_shell",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)) {
            @Override
            public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
                tip.add(Component.translatable("tooltip.echocity.echo_shell").withStyle(ChatFormatting.GRAY));
            }

            @Override
            public boolean isFoil(ItemStack stack) { return true; }
        });

    private ModItems() {}
}
