package github.flandre.modid;

import org.slf4j.Logger;

import github.flandre.modid.core.definitions.ModBlockEntities;
import github.flandre.modid.core.definitions.ModBlocks;
import github.flandre.modid.core.definitions.ModCreativeTabs;
import github.flandre.modid.core.definitions.ModItems;
import github.flandre.modid.core.definitions.ModMenuTypes;
import com.mojang.logging.LogUtils;

import appeng.api.AECapabilities;
import appeng.core.definitions.AEBlockEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Ae2GadgetryMod.MODID)
public class Ae2GadgetryMod {
    public static final String MODID = "ae2_gadgetry";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public Ae2GadgetryMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        ModBlocks.DR.register(modEventBus);
        ModCreativeTabs.DR.register(modEventBus);
        ModBlockEntities.DR.register(modEventBus);
        ModMenuTypes.DR.register(modEventBus);
        ModItems.init();
        ITEMS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                AEBlockEntities.CABLE_BUS.get(),
                (object, context) -> object.getPart(context) instanceof github.flandre.modid.part.LimitMeInterfacePart part
                        ? part.getExternalItemHandler()
                        : null);
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

