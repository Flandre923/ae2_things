package github.flandre.modid.core.definitions;

import github.flandre.modid.Ae2GadgetryMod;
import github.flandre.modid.menu.LimitIOBusMenu;
import github.flandre.modid.menu.LimitIOBusSetAmountMenu;
import github.flandre.modid.menu.LimitMeInterfaceMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> DR = DeferredRegister.create(Registries.MENU, Ae2GadgetryMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<LimitMeInterfaceMenu>> LIMIT_ME_INTERFACE = DR.register(
            "limit_me_interface",
            () -> LimitMeInterfaceMenu.TYPE);

    public static final DeferredHolder<MenuType<?>, MenuType<LimitIOBusMenu>> LIMIT_IMPORT_BUS = DR.register(
            "limit_import_bus",
            () -> LimitIOBusMenu.IMPORT_TYPE);

    public static final DeferredHolder<MenuType<?>, MenuType<LimitIOBusMenu>> LIMIT_EXPORT_BUS = DR.register(
            "limit_export_bus",
            () -> LimitIOBusMenu.EXPORT_TYPE);

    public static final DeferredHolder<MenuType<?>, MenuType<LimitIOBusSetAmountMenu>> LIMIT_IO_BUS_SET_AMOUNT = DR.register(
            "limit_io_bus_set_amount",
            () -> LimitIOBusSetAmountMenu.TYPE);

    private ModMenuTypes() {
    }
}

