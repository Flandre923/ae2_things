package github.flandre.modid.part;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.inventories.PlatformInventoryWrapper;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public final class LimitMeInterfaceStorage {
    private static final String TAG_MARKED_ITEM_PREFIX = "marked_item_";
    private static final String TAG_LIMIT_PREFIX = "limit_";

    private final HostAccess host;
    private final SimpleContainer markerInventory = new SimpleContainer(LimitMeInterfaceHost.SLOT_COUNT) {
        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!stack.isEmpty()) {
                stack = stack.copyWithCount(1);
            }
            super.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            refreshMarkedFlags();
            host.onContentsChanged();
        }
    };
    private final InternalInventory markerInternalInventory = new PlatformInventoryWrapper(new InvWrapper(this.markerInventory));
    private final IItemHandler externalItemHandler = new ExternalItemHandler();
    private final long[] limits = new long[LimitMeInterfaceHost.SLOT_COUNT];
    private final boolean[] slotMarkedFlags = new boolean[LimitMeInterfaceHost.SLOT_COUNT];

    public LimitMeInterfaceStorage(HostAccess host) {
        this.host = host;
    }

    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        for (int slot = 0; slot < LimitMeInterfaceHost.SLOT_COUNT; slot++) {
            var markedItemKey = TAG_MARKED_ITEM_PREFIX + slot;
            if (tag.contains(markedItemKey, Tag.TAG_COMPOUND)) {
                setMarkedItem(slot, ItemStack.parseOptional(registries, tag.getCompound(markedItemKey)));
            } else {
                clearMarkedItem(slot);
            }
            this.limits[slot] = Math.max(0, tag.getLong(TAG_LIMIT_PREFIX + slot));
        }
    }

    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        for (int slot = 0; slot < LimitMeInterfaceHost.SLOT_COUNT; slot++) {
            var marked = getMarkedItem(slot);
            if (!marked.isEmpty()) {
                tag.put(TAG_MARKED_ITEM_PREFIX + slot, marked.save(registries));
            }
            tag.putLong(TAG_LIMIT_PREFIX + slot, this.limits[slot]);
        }
    }

    public InternalInventory getMarkerInternalInventory() {
        return this.markerInternalInventory;
    }

    public IItemHandler getExternalItemHandler() {
        return this.externalItemHandler;
    }

    public void clearContent() {
        this.markerInventory.clearContent();
        refreshMarkedFlags();
    }

    public ItemStack getMarkedItem(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        return this.markerInventory.getItem(slot);
    }

    public void setMarkedItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        if (stack.isEmpty()) {
            clearMarkedItem(slot);
            return;
        }
        this.markerInventory.setItem(slot, stack.copyWithCount(1));
        host.onContentsChanged();
    }

    public void clearMarkedItem(int slot) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.markerInventory.setItem(slot, ItemStack.EMPTY);
        host.onContentsChanged();
    }

    public boolean isSlotMarked(int slot) {
        return isValidSlot(slot) && this.slotMarkedFlags[slot];
    }

    public int getMarkedSlotMask() {
        int mask = 0;
        for (int slot = 0; slot < LimitMeInterfaceHost.SLOT_COUNT; slot++) {
            if (this.slotMarkedFlags[slot]) {
                mask |= (1 << slot);
            }
        }
        return mask;
    }

    public long getLimit(int slot) {
        if (!isValidSlot(slot)) {
            return 0;
        }
        return this.limits[slot];
    }

    public void changeLimit(int slot, long delta) {
        if (!isValidSlot(slot) || delta == 0) {
            return;
        }

        long limit = this.limits[slot];
        if (delta > 0 && limit > Long.MAX_VALUE - delta) {
            limit = Long.MAX_VALUE;
        } else if (delta < 0 && limit < -delta) {
            limit = 0;
        } else {
            limit += delta;
        }
        this.limits[slot] = limit;
        host.onContentsChanged();
    }

    public void setUnlimited(int slot) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.limits[slot] = 0;
        host.onContentsChanged();
    }

    public void setLimit(int slot, long value) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.limits[slot] = Math.max(0, value);
        host.onContentsChanged();
    }

    public long getMarkedItemAmountInNetwork(int slot) {
        var key = getMarkedKey(slot);
        if (key == null) {
            return 0;
        }
        var grid = host.getGrid();
        if (grid == null) {
            return 0;
        }
        var allStacks = new KeyCounter();
        grid.getStorageService().getInventory().getAvailableStacks(allStacks);
        return allStacks.get(key);
    }

    public long insertMarkedItem(int slot, ItemStack stack, long requestedAmount) {
        return insertMarkedItem(slot, stack, requestedAmount, Actionable.MODULATE);
    }

    public long insertMarkedItemSimulate(int slot, ItemStack stack, long requestedAmount) {
        return insertMarkedItem(slot, stack, requestedAmount, Actionable.SIMULATE);
    }

    private long insertMarkedItem(int slot, ItemStack stack, long requestedAmount, Actionable mode) {
        if (!isValidSlot(slot) || requestedAmount <= 0 || stack.isEmpty()) {
            return 0;
        }
        var marked = getMarkedKey(slot);
        var incoming = AEItemKey.of(stack);
        if (marked == null || incoming == null || !marked.equals(incoming)) {
            return 0;
        }
        var grid = host.getGrid();
        if (grid == null) {
            return 0;
        }

        var currentAmount = getMarkedItemAmountInNetwork(slot);
        var maxInsert = requestedAmount;
        long limit = this.limits[slot];
        if (limit > 0) {
            if (currentAmount >= limit) {
                return 0;
            }
            maxInsert = Math.min(maxInsert, limit - currentAmount);
        }

        return grid.getStorageService().getInventory().insert(incoming, maxInsert, mode, IActionSource.empty());
    }

    public ItemStack extractMarkedItem(int slot, long requestedAmount) {
        return extractMarkedItem(slot, requestedAmount, Actionable.MODULATE);
    }

    public ItemStack extractMarkedItemSimulate(int slot, long requestedAmount) {
        return extractMarkedItem(slot, requestedAmount, Actionable.SIMULATE);
    }

    private ItemStack extractMarkedItem(int slot, long requestedAmount, Actionable mode) {
        if (!isValidSlot(slot) || requestedAmount <= 0) {
            return ItemStack.EMPTY;
        }
        var marked = getMarkedKey(slot);
        if (marked == null) {
            return ItemStack.EMPTY;
        }
        var grid = host.getGrid();
        if (grid == null) {
            return ItemStack.EMPTY;
        }

        long maxExtract = requestedAmount;
        long limit = this.limits[slot];
        if (limit > 0) {
            long currentAmount = getMarkedItemAmountInNetwork(slot);
            if (currentAmount <= limit) {
                return ItemStack.EMPTY;
            }
            maxExtract = Math.min(maxExtract, currentAmount - limit);
        }

        var extracted = grid.getStorageService().getInventory().extract(marked, maxExtract, mode, IActionSource.empty());
        if (extracted <= 0) {
            return ItemStack.EMPTY;
        }
        return marked.toStack((int) Math.min(extracted, Integer.MAX_VALUE));
    }

    private @Nullable AEItemKey getMarkedKey(int slot) {
        return AEItemKey.of(getMarkedItem(slot));
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < LimitMeInterfaceHost.SLOT_COUNT;
    }

    private void refreshMarkedFlags() {
        for (int slot = 0; slot < LimitMeInterfaceHost.SLOT_COUNT; slot++) {
            this.slotMarkedFlags[slot] = !this.markerInventory.getItem(slot).isEmpty();
        }
    }

    public interface HostAccess {
        void onContentsChanged();

        @Nullable
        IGrid getGrid();
    }

    private class ExternalItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return LimitMeInterfaceHost.SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (!isValidSlot(slot)) {
                return ItemStack.EMPTY;
            }
            var marked = getMarkedItem(slot);
            if (marked.isEmpty()) {
                return ItemStack.EMPTY;
            }
            long amount = getMarkedItemAmountInNetwork(slot);
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }
            return marked.copyWithCount((int) Math.min(marked.getMaxStackSize(), amount));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isValidSlot(slot) || stack.isEmpty()) {
                return stack;
            }
            long inserted = simulate
                    ? insertMarkedItemSimulate(slot, stack, stack.getCount())
                    : insertMarkedItem(slot, stack, stack.getCount());
            if (inserted <= 0) {
                return stack;
            }
            var remaining = stack.copy();
            remaining.shrink((int) inserted);
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isValidSlot(slot) || amount <= 0) {
                return ItemStack.EMPTY;
            }
            return simulate ? extractMarkedItemSimulate(slot, amount) : extractMarkedItem(slot, amount);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (!isValidSlot(slot) || stack.isEmpty()) {
                return false;
            }
            return insertMarkedItemSimulate(slot, stack, 1) > 0;
        }
    }
}
