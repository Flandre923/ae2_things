package github.flandre.modid.core.definitions;

import github.flandre.modid.Ae2GadgetryMod;
import github.flandre.modid.part.LimitExportBusPart;
import github.flandre.modid.part.LimitImportBusPart;
import github.flandre.modid.part.LimitMeInterfacePart;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Function;

public final class ModItems {
    public static final DeferredHolder<Item, PartItem<LimitMeInterfacePart>> LIMIT_ME_INTERFACE_PART = registerPart(
            "limit_me_interface_part",
            LimitMeInterfacePart.class,
            LimitMeInterfacePart::new);

    public static final DeferredHolder<Item, PartItem<LimitImportBusPart>> LIMIT_IMPORT_BUS = registerPart(
            "limit_import_bus",
            LimitImportBusPart.class,
            LimitImportBusPart::new);

    public static final DeferredHolder<Item, PartItem<LimitExportBusPart>> LIMIT_EXPORT_BUS = registerPart(
            "limit_export_bus",
            LimitExportBusPart.class,
            LimitExportBusPart::new);

    private ModItems() {
    }

    public static void init() {
        // Trigger class initialization for static item registrations.
    }

    private static <T extends IPart> DeferredHolder<Item, PartItem<T>> registerPart(
            String id,
            Class<T> partClass,
            Function<IPartItem<T>, T> factory) {
        PartModels.registerModels(PartModelsHelper.createModels(partClass));
        return Ae2GadgetryMod.ITEMS.register(id, () -> new PartItem<>(new Item.Properties(), partClass, factory));
    }
}
