package com.ae2things.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;

import com.ae2things.menu.LimitMeInterfaceMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LimitMeInterfaceConfigScreen extends AEBaseScreen<LimitMeInterfaceMenu> {
    private final String mainStylePath;
    private final Component parentTitle;
    private final Inventory playerInventory;
    private final NumberEntryWidget limitEntry;
    private boolean limitInitialized;

    public LimitMeInterfaceConfigScreen(LimitMeInterfaceMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style, String mainStylePath) {
        super(menu, playerInventory, Component.translatable("gui.ae2_things.limit_page"), style);
        this.mainStylePath = mainStylePath;
        this.parentTitle = title;
        this.playerInventory = playerInventory;

        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        widgets.add("back", new TabButton(Icon.BACK, Component.translatable("gui.ae2_things.back"), btn -> goBack()));

        this.limitEntry = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.UNITLESS);
        this.limitEntry.setTextFieldStyle(style.getWidget("amountToStockInput"));
        this.limitEntry.setMinValue(0);
        this.limitEntry.setHideValidationIcon(true);
        this.limitEntry.setOnConfirm(this::confirm);
    }

    @Override
    protected boolean shouldAddToolbar() {
        return false;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!this.limitInitialized) {
            this.limitEntry.setLongValue(this.menu.getLimit());
            this.limitInitialized = true;
        }
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        var marked = this.menu.getMarkedItem();
        if (!marked.isEmpty()) {
            this.drawItem(guiGraphics, 23, 53, marked);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            goBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirm() {
        this.limitEntry.getLongValue().ifPresent(value -> {
            this.menu.setAbsoluteLimit(value);
            goBack();
        });
    }

    private void goBack() {
        this.switchToScreen(new LimitMeInterfaceScreen(this.menu, this.playerInventory, this.parentTitle,
                StyleManager.loadStyleDoc(this.mainStylePath)));
    }
}

