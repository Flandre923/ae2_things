package github.flandre.modid.part;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IManagedGridNode;
import appeng.helpers.InterfaceLogicHost;
import appeng.menu.locator.MenuHostLocator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public interface LimitMeInterfaceHost extends InterfaceLogicHost {
    int SLOT_COUNT = 9;

    Component getDisplayName();

    boolean isMenuValid(Player player);

    InternalInventory getMarkerInternalInventory();

    IItemHandler getExternalItemHandler();

    ItemStack getMarkedItem(int slot);

    void setMarkedItem(int slot, ItemStack stack);

    void clearMarkedItem(int slot);

    boolean isSlotMarked(int slot);

    int getMarkedSlotMask();

    long getLimit(int slot);

    void changeLimit(int slot, long delta);

    void setUnlimited(int slot);

    void setLimit(int slot, long value);

    long getMarkedItemAmountInNetwork(int slot);

    long insertMarkedItem(int slot, ItemStack stack, long requestedAmount);

    long insertMarkedItemSimulate(int slot, ItemStack stack, long requestedAmount);

    ItemStack extractMarkedItem(int slot, long requestedAmount);

    ItemStack extractMarkedItemSimulate(int slot, long requestedAmount);

    void openMenu(Player player, MenuHostLocator locator);
}
