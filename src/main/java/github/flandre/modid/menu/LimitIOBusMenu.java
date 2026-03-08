package github.flandre.modid.menu;

import appeng.api.util.KeyTypeSelection;
import appeng.api.util.KeyTypeSelectionHost;
import appeng.menu.implementations.IOBusMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.parts.automation.IOBusPart;
import github.flandre.modid.Ae2GadgetryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class LimitIOBusMenu extends IOBusMenu {
    public static final String ACTION_OPEN_SET_AMOUNT = "setAmount";

    public static final MenuType<LimitIOBusMenu> EXPORT_TYPE = MenuTypeBuilder
            .create(LimitIOBusMenu::new, github.flandre.modid.part.LimitExportBusPart.class)
            .buildUnregistered(ResourceLocation.parse(Ae2GadgetryMod.MODID + ":limit_export_bus"));

    public static final MenuType<LimitIOBusMenu> IMPORT_TYPE = MenuTypeBuilder
            .create(LimitIOBusMenu::new, github.flandre.modid.part.LimitImportBusPart.class)
            .buildUnregistered(ResourceLocation.parse(Ae2GadgetryMod.MODID + ":limit_import_bus"));

    public LimitIOBusMenu(MenuType<?> menuType, int id, Inventory ip, IOBusPart host) {
        super(menuType, id, ip, host);
        registerClientAction(ACTION_OPEN_SET_AMOUNT, Integer.class, this::openSetAmountMenuServer);
    }

    public void requestOpenSetAmountMenu(int configSlot) {
        if (isClientSide()) {
            sendClientAction(ACTION_OPEN_SET_AMOUNT, configSlot);
        } else {
            openSetAmountMenuServer(configSlot);
        }
    }

    private void openSetAmountMenuServer(int configSlot) {
        var stack = getHost().getConfig().getStack(configSlot);
        if (stack != null) {
            LimitIOBusSetAmountMenu.open((ServerPlayer) getPlayer(), getLocator(), configSlot,
                    stack.what(), (int) stack.amount());
        }
    }

    @Override
    public KeyTypeSelection getServerKeyTypeSelection() {
        return ((KeyTypeSelectionHost) getHost()).getKeyTypeSelection();
    }
}
