package com.example.examplemod;

import org.slf4j.Logger;

import com.example.examplemod.core.definitions.ModBlockEntities;
import com.example.examplemod.core.definitions.ModBlocks;
import com.example.examplemod.core.definitions.ModCreativeTabs;
import com.example.examplemod.core.definitions.ModMenuTypes;
import com.mojang.logging.LogUtils;

import appeng.api.AECapabilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ExampleMod.MODID)
public class ExampleMod {
    public static final String MODID = "ae2_things";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        ModBlocks.DR.register(modEventBus);
        ModCreativeTabs.DR.register(modEventBus);
        ModBlockEntities.DR.register(modEventBus);
        ModMenuTypes.DR.register(modEventBus);
        ITEMS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Common setup hook.
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.LIMIT_ME_INTERFACE.get(),
                (object, context) -> object);
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.LIMIT_ME_INTERFACE.get(),
                (object, context) -> object.getExternalItemHandler(context));
    }
}
