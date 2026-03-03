package com.example.examplemod.core.definitions;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.menu.LimitMeInterfaceMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> DR = DeferredRegister.create(Registries.MENU, ExampleMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<LimitMeInterfaceMenu>> LIMIT_ME_INTERFACE = DR.register(
            "limit_me_interface",
            () -> IMenuTypeExtension.create(LimitMeInterfaceMenu::new));

    private ModMenuTypes() {
    }
}
