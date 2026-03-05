package github.flandre.modid.blockentity;

import org.jetbrains.annotations.Nullable;

import github.flandre.modid.core.definitions.ModBlockEntities;
import github.flandre.modid.core.definitions.ModBlocks;
import github.flandre.modid.menu.LimitMeInterfaceMenu;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.inventories.PlatformInventoryWrapper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleContainer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class LimitMeInterfaceBlockEntity extends BlockEntity implements MenuProvider, IInWorldGridNodeHost {
    public static final int SLOT_COUNT = 9;
    private static final String TAG_MARKED_ITEM_PREFIX = "marked_item_";
    private static final String TAG_LIMIT_PREFIX = "limit_";

    private static final IGridNodeListener<LimitMeInterfaceBlockEntity> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(LimitMeInterfaceBlockEntity nodeOwner, IGridNode node) {
            nodeOwner.setChanged();
        }
    };

    private final IManagedGridNode mainNode;
    private final SimpleContainer markerInventory = new SimpleContainer(SLOT_COUNT) {
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
            LimitMeInterfaceBlockEntity.this.setChanged();
        }
    };
    private final InternalInventory markerInternalInventory = new PlatformInventoryWrapper(new InvWrapper(
            this.markerInventory));
    private final IItemHandler externalItemHandler = new ExternalItemHandler();
    private final long[] limits = new long[SLOT_COUNT];
    private final boolean[] slotMarkedFlags = new boolean[SLOT_COUNT];
    private boolean nodeDestroyed = false;

    public LimitMeInterfaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.LIMIT_ME_INTERFACE.get(), pos, blockState);
        this.mainNode = GridHelper.createManagedNode(this, NODE_LISTENER)
                .setVisualRepresentation(ModBlocks.LIMIT_ME_INTERFACE.item().get())
                .setInWorldNode(true)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setTagName("gridNode");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LimitMeInterfaceBlockEntity blockEntity) {
        // Intentionally empty. We use this ticker only to ensure server-side lifecycle behavior.
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        this.nodeDestroyed = false;
        GridHelper.onFirstTick(this, be -> be.mainNode.create(be.getLevel(), be.getBlockPos()));
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        destroyNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        destroyNode();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            var marked = getMarkedItem(slot);
            if (!marked.isEmpty()) {
                tag.put(TAG_MARKED_ITEM_PREFIX + slot, marked.save(registries));
            }
            tag.putLong(TAG_LIMIT_PREFIX + slot, this.limits[slot]);
        }

        this.mainNode.saveToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            var markedItemKey = TAG_MARKED_ITEM_PREFIX + slot;
            if (tag.contains(markedItemKey, Tag.TAG_COMPOUND)) {
                setMarkedItem(slot, ItemStack.parseOptional(registries, tag.getCompound(markedItemKey)));
            } else {
                clearMarkedItem(slot);
            }
            this.limits[slot] = Math.max(0, tag.getLong(TAG_LIMIT_PREFIX + slot));
        }

        this.mainNode.loadFromNBT(tag);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ae2_gadgetry.limit_me_interface");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LimitMeInterfaceMenu(containerId, playerInventory, this);
    }

    @Override
    public @Nullable IGridNode getGridNode(Direction dir) {
        return this.mainNode.getNode();
    }

    public Container getMarkerInventory() {
        return this.markerInventory;
    }

    public InternalInventory getMarkerInternalInventory() {
        return this.markerInternalInventory;
    }

    public IItemHandler getExternalItemHandler(@Nullable Direction side) {
        return this.externalItemHandler;
    }

    public ItemStack getMarkedItem() {
        return getMarkedItem(0);
    }

    public ItemStack getMarkedItem(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        return this.markerInventory.getItem(slot);
    }

    public void setMarkedItem(ItemStack stack) {
        setMarkedItem(0, stack);
    }

    public void setMarkedItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }

        if (stack.isEmpty()) {
            clearMarkedItem(slot);
            return;
        }
        var copy = stack.copyWithCount(1);
        this.markerInventory.setItem(slot, copy);
        this.setChanged();
    }

    public void clearMarkedItem() {
        clearMarkedItem(0);
    }

    public void clearMarkedItem(int slot) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.markerInventory.setItem(slot, ItemStack.EMPTY);
        this.setChanged();
    }

    public boolean isSlotMarked(int slot) {
        return isValidSlot(slot) && this.slotMarkedFlags[slot];
    }

    public int getMarkedSlotMask() {
        int mask = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (this.slotMarkedFlags[slot]) {
                mask |= (1 << slot);
            }
        }
        return mask;
    }

    public long getLimit() {
        return getLimit(0);
    }

    public long getLimit(int slot) {
        if (!isValidSlot(slot)) {
            return 0;
        }
        return this.limits[slot];
    }

    public void changeLimit(long delta) {
        changeLimit(0, delta);
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
        this.setChanged();
    }

    public void setUnlimited() {
        setUnlimited(0);
    }

    public void setUnlimited(int slot) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.limits[slot] = 0;
        this.setChanged();
    }

    public void setLimit(int slot, long value) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.limits[slot] = Math.max(0, value);
        this.setChanged();
    }

    public long getMarkedItemAmountInNetwork() {
        return getMarkedItemAmountInNetwork(0);
    }

    public long getMarkedItemAmountInNetwork(int slot) {
        var key = getMarkedKey(slot);
        if (key == null) {
            return 0;
        }
        var grid = getGrid();
        if (grid == null) {
            return 0;
        }
        var allStacks = new KeyCounter();
        grid.getStorageService().getInventory().getAvailableStacks(allStacks);
        return allStacks.get(key);
    }

    public long insertMarkedItem(ItemStack stack, long requestedAmount) {
        return insertMarkedItem(0, stack, requestedAmount);
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
        var grid = getGrid();
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

        return grid.getStorageService().getInventory().insert(incoming, maxInsert, mode,
                IActionSource.empty());
    }

    public ItemStack extractMarkedItem(long requestedAmount) {
        return extractMarkedItem(0, requestedAmount);
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
        var grid = getGrid();
        if (grid == null) {
            return ItemStack.EMPTY;
        }

        var extracted = grid.getStorageService().getInventory().extract(marked, requestedAmount, mode,
                IActionSource.empty());
        if (extracted <= 0) {
            return ItemStack.EMPTY;
        }
        return marked.toStack((int) Math.min(extracted, Integer.MAX_VALUE));
    }

    public void setOwningPlayer(Player player) {
        this.mainNode.setOwningPlayer(player);
        this.setChanged();
    }

    private @Nullable IGrid getGrid() {
        if (this.level == null || this.level.isClientSide || !this.mainNode.isReady()) {
            return null;
        }
        return this.mainNode.getGrid();
    }

    private @Nullable AEItemKey getMarkedKey() {
        return getMarkedKey(0);
    }

    private @Nullable AEItemKey getMarkedKey(int slot) {
        return AEItemKey.of(getMarkedItem(slot));
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private void destroyNode() {
        if (!this.nodeDestroyed) {
            this.mainNode.destroy();
            this.nodeDestroyed = true;
        }
    }

    private void refreshMarkedFlags() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.slotMarkedFlags[slot] = !this.markerInventory.getItem(slot).isEmpty();
        }
    }

    private class ExternalItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return SLOT_COUNT;
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


