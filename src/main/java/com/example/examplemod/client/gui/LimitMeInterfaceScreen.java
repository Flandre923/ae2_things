package com.example.examplemod.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;

import com.example.examplemod.menu.LimitMeInterfaceMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LimitMeInterfaceScreen extends AEBaseScreen<LimitMeInterfaceMenu> {
    private static final ResourceLocation ICONS_TEXTURE = ResourceLocation.parse("ae2_things:textures/gui/icons.png");
    private static final String MAIN_STYLE = "/screens/ae2_things/limit_interface.json";
    private static final String CONFIG_STYLE = "/screens/ae2_things/limit_interface_config.json";

    private static final int ICON_TEXTURE_SIZE = 256;
    private static final int GEAR_ICON_U = 32;
    private static final int GEAR_ICON_V = 64;
    private static final int GEAR_ICON_W = 18;
    private static final int GEAR_ICON_H = 16;
    private final Inventory playerInventory;

    public LimitMeInterfaceScreen(LimitMeInterfaceMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.playerInventory = playerInventory;
    }

    @Override
    protected boolean shouldAddToolbar() {
        return false;
    }

    @Override
    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(guiGraphics, offsetX, offsetY, mouseX, mouseY, partialTicks);

        for (int slot = 0; slot < 9; slot++) {
            if (!this.menu.hasMarkedSlotFlag(slot)) {
                continue;
            }
            int iconX = this.leftPos + 8 + slot * 18;
            int iconY = this.topPos + 34;
            guiGraphics.blit(ICONS_TEXTURE, iconX, iconY, GEAR_ICON_U, GEAR_ICON_V, GEAR_ICON_W, GEAR_ICON_H,
                    ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
        }
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        renderSlotAmounts(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int slot = 0; slot < 9; slot++) {
                if (!this.menu.hasMarkedSlotFlag(slot)) {
                    continue;
                }
                int iconX = this.leftPos + 8 + slot * 18;
                int iconY = this.topPos + 34;
                if (mouseX >= iconX && mouseX < iconX + GEAR_ICON_W
                        && mouseY >= iconY && mouseY < iconY + GEAR_ICON_H) {
                    this.menu.selectConfigSlot(slot);
                    this.switchToScreen(new LimitMeInterfaceConfigScreen(this.menu, this.playerInventory, this.title,
                            StyleManager.loadStyleDoc(CONFIG_STYLE), MAIN_STYLE));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderSlotAmounts(GuiGraphics guiGraphics) {
        for (int slot = 0; slot < 9; slot++) {
            if (!this.menu.hasMarkedSlotFlag(slot)) {
                continue;
            }

            String limitText = this.menu.getLimit(slot) <= 0 ? "INF" : compactAmount(this.menu.getLimit(slot));
            drawTinyText(guiGraphics, limitText, 9 + slot * 18, 65, 0xE8D060);

            String networkText = compactAmount(this.menu.getNetworkAmount(slot));
            drawTinyText(guiGraphics, networkText, 9 + slot * 18, 83, 0x66CCFF);
        }
    }

    private void drawTinyText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
        guiGraphics.drawString(this.font, text, x * 2, y * 2, color, false);
        guiGraphics.pose().popPose();
    }

    private static String compactAmount(long value) {
        if (value >= 1_000_000_000L) {
            return (value / 1_000_000_000L) + "G";
        }
        if (value >= 1_000_000L) {
            return (value / 1_000_000L) + "M";
        }
        if (value >= 10_000L) {
            return (value / 1_000L) + "K";
        }
        return Long.toString(value);
    }
}

