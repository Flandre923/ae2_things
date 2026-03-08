package github.flandre.modid.client.gui;

import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.core.localization.GuiText;
import github.flandre.modid.menu.LimitIOBusSetAmountMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LimitIOBusSetAmountScreen extends appeng.client.gui.AEBaseScreen<LimitIOBusSetAmountMenu> {
    private final NumberEntryWidget amount;
    private boolean initialized;

    public LimitIOBusSetAmountScreen(LimitIOBusSetAmountMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        AESubScreen.addBackButton(menu, "back", widgets);

        this.amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.UNITLESS);
        this.amount.setLongValue(1);
        this.amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        this.amount.setMinValue(0);
        this.amount.setHideValidationIcon(true);
        this.amount.setOnConfirm(this::confirm);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!this.initialized) {
            var configuredKey = menu.getConfiguredKey();
            if (configuredKey != null) {
                this.amount.setType(NumberEntryType.of(configuredKey));
                this.amount.setLongValue(menu.getInitialAmount());
                this.amount.setMaxValue(menu.getMaxAmount());
                this.initialized = true;
            }
        }
    }

    private void confirm() {
        this.amount.getIntValue().ifPresent(menu::confirmAmount);
    }
}
