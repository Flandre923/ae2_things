package github.flandre.modid;

import github.flandre.modid.client.gui.LimitMeInterfaceScreen;
import github.flandre.modid.client.gui.LimitIOBusScreen;
import github.flandre.modid.client.gui.LimitIOBusSetAmountScreen;
import github.flandre.modid.core.definitions.ModMenuTypes;

import appeng.init.client.InitScreens;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Ae2GadgetryMod.MODID, dist = Dist.CLIENT)
public class Ae2GadgetryModClient {
    public Ae2GadgetryModClient(IEventBus modEventBus, ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(Ae2GadgetryModClient::onClientSetup);
        modEventBus.addListener(Ae2GadgetryModClient::registerScreens);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Ae2GadgetryMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        Ae2GadgetryMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, ModMenuTypes.LIMIT_ME_INTERFACE.get(), LimitMeInterfaceScreen::new,
                "/screens/ae2_gadgetry/limit_interface.json");
        InitScreens.register(event, ModMenuTypes.LIMIT_IMPORT_BUS.get(), LimitIOBusScreen::new,
                "/screens/import_bus.json");
        InitScreens.register(event, ModMenuTypes.LIMIT_EXPORT_BUS.get(), LimitIOBusScreen::new,
                "/screens/export_bus.json");
        InitScreens.register(event, ModMenuTypes.LIMIT_IO_BUS_SET_AMOUNT.get(), LimitIOBusSetAmountScreen::new,
                "/screens/set_stock_amount.json");
    }
}

