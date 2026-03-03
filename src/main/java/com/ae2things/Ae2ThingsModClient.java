package com.ae2things;

import com.ae2things.client.gui.LimitMeInterfaceScreen;
import com.ae2things.core.definitions.ModMenuTypes;

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
@Mod(value = Ae2ThingsMod.MODID, dist = Dist.CLIENT)
public class Ae2ThingsModClient {
    public Ae2ThingsModClient(IEventBus modEventBus, ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(Ae2ThingsModClient::onClientSetup);
        modEventBus.addListener(Ae2ThingsModClient::registerScreens);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        Ae2ThingsMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        Ae2ThingsMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    static void registerScreens(RegisterMenuScreensEvent event) {
        InitScreens.register(event, ModMenuTypes.LIMIT_ME_INTERFACE.get(), LimitMeInterfaceScreen::new,
                "/screens/ae2_things/limit_interface.json");
    }
}
