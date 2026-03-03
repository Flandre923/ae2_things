package com.ae2things.core.definitions;

import com.ae2things.Ae2ThingsMod;
import com.ae2things.menu.LimitMeInterfaceMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> DR = DeferredRegister.create(Registries.MENU, Ae2ThingsMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<LimitMeInterfaceMenu>> LIMIT_ME_INTERFACE = DR.register(
            "limit_me_interface",
            () -> IMenuTypeExtension.create(LimitMeInterfaceMenu::new));

    private ModMenuTypes() {
    }
}
