package github.flandre.modid.menu;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.implementations.AESubScreen;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.locator.MenuHostLocator;
import appeng.parts.automation.IOBusPart;
import appeng.util.inv.AppEngInternalInventory;
import appeng.menu.slot.InaccessibleSlot;
import github.flandre.modid.Ae2GadgetryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class LimitIOBusSetAmountMenu extends AEBaseMenu implements ISubMenu {
    public static final String ACTION_SET_AMOUNT = "setAmount";

    public static final MenuType<LimitIOBusSetAmountMenu> TYPE = MenuTypeBuilder
            .create(LimitIOBusSetAmountMenu::new, IOBusPart.class)
            .buildUnregistered(ResourceLocation.parse(Ae2GadgetryMod.MODID + ":limit_io_bus_set_amount"));

    private final IOBusPart host;
    private final InaccessibleSlot configuredItem;

    private int configSlot = -1;
    @Nullable
    private AEKey configuredKey;
    @GuiSync(1)
    private int initialAmount = 0;
    @GuiSync(2)
    private int maxAmount = Integer.MAX_VALUE;

    public LimitIOBusSetAmountMenu(int id, Inventory playerInventory, IOBusPart host) {
        super(TYPE, id, playerInventory, host);
        this.host = host;
        this.configuredItem = new InaccessibleSlot(new AppEngInternalInventory(1), 0);
        this.addSlot(this.configuredItem, SlotSemantics.MACHINE_OUTPUT);
        registerClientAction(ACTION_SET_AMOUNT, Integer.class, this::confirmAmount);
    }

    public static void open(ServerPlayer player, MenuHostLocator locator, int configSlot, AEKey whatToConfigure,
            int initialAmount) {
        appeng.menu.MenuOpener.open(TYPE, player, locator);

        if (player.containerMenu instanceof LimitIOBusSetAmountMenu menu) {
            menu.setConfiguredStack(configSlot, whatToConfigure, initialAmount);
            menu.broadcastChanges();
        }
    }

    private void setConfiguredStack(int configSlot, AEKey whatToConfigure, int initialAmount) {
        this.configSlot = configSlot;
        this.configuredKey = Objects.requireNonNull(whatToConfigure, "whatToConfigure");
        this.initialAmount = initialAmount;
        this.maxAmount = Math.max(0, (int) Math.min(Integer.MAX_VALUE, this.host.getConfig().getMaxAmount(whatToConfigure)));
        this.configuredItem.set(whatToConfigure.wrapForDisplayOrFilter());
    }

    public void confirmAmount(int amount) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_AMOUNT, amount);
            return;
        }

        if (this.configSlot < 0 || this.configuredKey == null) {
            this.host.returnToMainMenu(getPlayer(), this);
            return;
        }

        var config = this.host.getConfig();
        if (!Objects.equals(config.getKey(this.configSlot), this.configuredKey)) {
            this.host.returnToMainMenu(getPlayer(), this);
            return;
        }

        amount = Math.max(0, Math.min(amount, this.maxAmount));
        if (amount == 0) {
            config.setStack(this.configSlot, null);
        } else {
            config.setStack(this.configSlot, new GenericStack(this.configuredKey, amount));
        }

        this.host.returnToMainMenu(getPlayer(), this);
    }

    @Override
    public IOBusPart getHost() {
        return this.host;
    }

    public int getInitialAmount() {
        return this.initialAmount;
    }

    public int getMaxAmount() {
        return this.maxAmount;
    }

    @Nullable
    public AEKey getConfiguredKey() {
        var stack = GenericStack.fromItemStack(this.configuredItem.getItem());
        return stack != null ? stack.what() : null;
    }

    public void goBack() {
        if (isClientSide()) {
            AESubScreen.goBack();
        }
    }
}
