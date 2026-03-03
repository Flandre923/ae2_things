package com.ae2things.core.definitions;

import com.ae2things.Ae2ThingsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> DR = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            Ae2ThingsMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = DR.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ae2_things.main"))
                    .icon(() -> new ItemStack(ModBlocks.LIMIT_ME_INTERFACE.item().get()))
                    .displayItems((params, output) -> output.accept(ModBlocks.LIMIT_ME_INTERFACE.item().get()))
                    .build());

    private ModCreativeTabs() {
    }
}
