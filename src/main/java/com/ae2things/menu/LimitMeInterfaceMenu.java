package com.ae2things.menu;

import appeng.api.inventories.InternalInventory;
import appeng.api.inventories.PlatformInventoryWrapper;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;

import com.ae2things.blockentity.LimitMeInterfaceBlockEntity;
import com.ae2things.core.definitions.ModBlocks;
import com.ae2things.core.definitions.ModMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public class LimitMeInterfaceMenu extends AEBaseMenu {
    public static final int BUTTON_LIMIT_MINUS_64 = 1;
    public static final int BUTTON_LIMIT_MINUS_10 = 2;
    public static final int BUTTON_LIMIT_MINUS_1 = 3;
    public static final int BUTTON_LIMIT_PLUS_1 = 4;
    public static final int BUTTON_LIMIT_PLUS_10 = 5;
    public static final int BUTTON_LIMIT_PLUS_64 = 6;
    public static final int BUTTON_LIMIT_UNLIMITED = 7;
    public static final int BUTTON_SELECT_SLOT_BASE = 100;
    public static final String ACTION_SELECT_SLOT = "select_slot";
    public static final String ACTION_SET_LIMIT = "set_limit";

    private static final int CONFIG_SLOT_START = 0;
    private static final int DISPLAY_SLOT_START = LimitMeInterfaceBlockEntity.SLOT_COUNT;
    private static final int PLAYER_SLOT_START = DISPLAY_SLOT_START + LimitMeInterfaceBlockEntity.SLOT_COUNT;

    private final LimitMeInterfaceBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final SimpleContainer displaySlotInventory = new SimpleContainer(LimitMeInterfaceBlockEntity.SLOT_COUNT);
    private final InternalInventory displaySlotInternalInventory = new PlatformInventoryWrapper(new InvWrapper(
            this.displaySlotInventory));

    private int selectedSlot;
    private final int[] networkAmountLow = new int[LimitMeInterfaceBlockEntity.SLOT_COUNT];
    private final int[] networkAmountHigh = new int[LimitMeInterfaceBlockEntity.SLOT_COUNT];
    private final int[] limitLow = new int[LimitMeInterfaceBlockEntity.SLOT_COUNT];
    private final int[] limitHigh = new int[LimitMeInterfaceBlockEntity.SLOT_COUNT];
    private int markedSlotMask;

    public LimitMeInterfaceMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, data.readBlockPos()));
    }

    public LimitMeInterfaceMenu(int containerId, Inventory playerInventory, LimitMeInterfaceBlockEntity blockEntity) {
        super(ModMenuTypes.LIMIT_ME_INTERFACE.get(), containerId, playerInventory, blockEntity);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        registerClientAction(ACTION_SELECT_SLOT, Integer.class, this::selectConfigSlot);
        registerClientAction(ACTION_SET_LIMIT, Long.class, this::setAbsoluteLimit);

        addConfigSlots();
        addDisplaySlots();
        createPlayerInventorySlots(playerInventory);

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return selectedSlot;
            }

            @Override
            public void set(int value) {
                selectedSlot = clampSlot(value);
            }
        });
        for (int slot = 0; slot < LimitMeInterfaceBlockEntity.SLOT_COUNT; slot++) {
            final int s = slot;
            this.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return lowPart(blockEntity.getMarkedItemAmountInNetwork(s));
                }

                @Override
                public void set(int value) {
                    networkAmountLow[s] = value;
                }
            });
            this.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return highPart(blockEntity.getMarkedItemAmountInNetwork(s));
                }

                @Override
                public void set(int value) {
                    networkAmountHigh[s] = value;
                }
            });
            this.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return lowPart(blockEntity.getLimit(s));
                }

                @Override
                public void set(int value) {
                    limitLow[s] = value;
                }
            });
            this.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return highPart(blockEntity.getLimit(s));
                }

                @Override
                public void set(int value) {
                    limitHigh[s] = value;
                }
            });
        }
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.getMarkedSlotMask();
            }

            @Override
            public void set(int value) {
                markedSlotMask = value;
            }
        });
    }

    @Override
    public void broadcastChanges() {
        updateDisplaySlots();
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, ModBlocks.LIMIT_ME_INTERFACE.block().get());
    }

    @Override
    public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
        if (isConfigSlot(slot)) {
            this.selectedSlot = clampSlot(slot - CONFIG_SLOT_START);
        }
        super.doAction(player, action, slot, id);
    }

    @Override
    public void onSlotChange(Slot slot) {
        super.onSlotChange(slot);
        if (slot != null && getSlotSemantic(slot) == SlotSemantics.CONFIG) {
            this.selectedSlot = clampSlot(this.slots.indexOf(slot));
        }
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (isDisplaySlot(slotId) && clickType == ClickType.PICKUP) {
            int targetSlot = slotId - DISPLAY_SLOT_START;
            this.selectedSlot = targetSlot;

            var marked = this.blockEntity.getMarkedItem(targetSlot);
            if (marked.isEmpty()) {
                return;
            }

            var carried = getCarried();
            if (!carried.isEmpty()) {
                long requested = dragType == 1 ? 1 : carried.getCount();
                long inserted = this.blockEntity.insertMarkedItem(targetSlot, carried, requested);
                if (inserted > 0) {
                    carried.shrink((int) inserted);
                    setCarried(carried);
                }
            } else {
                long toExtract = dragType == 1 ? 1 : marked.getMaxStackSize();
                var extracted = this.blockEntity.extractMarkedItem(targetSlot, toExtract);
                if (!extracted.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(extracted);
                }
            }
            return;
        }

        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        var slot = this.slots.get(slotIndex);
        if (!slot.hasItem() || !isPlayerSideSlot(slot)) {
            return ItemStack.EMPTY;
        }

        var sourceStack = slot.getItem();
        var copied = sourceStack.copy();

        var marked = this.blockEntity.getMarkedItem(this.selectedSlot);
        if (marked.isEmpty()) {
            this.blockEntity.setMarkedItem(this.selectedSlot, sourceStack);
            return copied;
        }

        long inserted = this.blockEntity.insertMarkedItem(this.selectedSlot, sourceStack, sourceStack.getCount());
        if (inserted <= 0) {
            return ItemStack.EMPTY;
        }

        sourceStack.shrink((int) inserted);
        if (sourceStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copied;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_SELECT_SLOT_BASE && id < BUTTON_SELECT_SLOT_BASE + LimitMeInterfaceBlockEntity.SLOT_COUNT) {
            this.selectedSlot = id - BUTTON_SELECT_SLOT_BASE;
            return true;
        }

        return switch (id) {
            case BUTTON_LIMIT_MINUS_64 -> applyLimitDelta(-64);
            case BUTTON_LIMIT_MINUS_10 -> applyLimitDelta(-10);
            case BUTTON_LIMIT_MINUS_1 -> applyLimitDelta(-1);
            case BUTTON_LIMIT_PLUS_1 -> applyLimitDelta(1);
            case BUTTON_LIMIT_PLUS_10 -> applyLimitDelta(10);
            case BUTTON_LIMIT_PLUS_64 -> applyLimitDelta(64);
            case BUTTON_LIMIT_UNLIMITED -> {
                this.blockEntity.setUnlimited(this.selectedSlot);
                yield true;
            }
            default -> false;
        };
    }

    public void selectConfigSlot(int slot) {
        this.selectedSlot = clampSlot(slot);
        if (isClientSide()) {
            sendClientAction(ACTION_SELECT_SLOT, this.selectedSlot);
        }
    }

    public void setAbsoluteLimit(long value) {
        long normalized = Math.max(0, value);
        this.blockEntity.setLimit(this.selectedSlot, normalized);
        if (isClientSide()) {
            sendClientAction(ACTION_SET_LIMIT, normalized);
        }
    }

    public ItemStack getMarkedItem() {
        return this.blockEntity.getMarkedItem(this.selectedSlot);
    }

    public long getNetworkAmount() {
        return getNetworkAmount(this.selectedSlot);
    }

    public long getLimit() {
        return getLimit(this.selectedSlot);
    }

    public long getNetworkAmount(int slot) {
        if (slot < 0 || slot >= LimitMeInterfaceBlockEntity.SLOT_COUNT) {
            return 0;
        }
        return composeLong(this.networkAmountLow[slot], this.networkAmountHigh[slot]);
    }

    public long getLimit(int slot) {
        if (slot < 0 || slot >= LimitMeInterfaceBlockEntity.SLOT_COUNT) {
            return 0;
        }
        return composeLong(this.limitLow[slot], this.limitHigh[slot]);
    }

    public int getSelectedSlot() {
        return this.selectedSlot;
    }

    public boolean hasMarkedSlotFlag(int slot) {
        return slot >= 0 && slot < LimitMeInterfaceBlockEntity.SLOT_COUNT
                && (this.markedSlotMask & (1 << slot)) != 0;
    }

    public void setSelectedSlotClient(int slot) {
        this.selectedSlot = clampSlot(slot);
    }

    private boolean applyLimitDelta(long delta) {
        this.blockEntity.changeLimit(this.selectedSlot, delta);
        return true;
    }

    private void updateDisplaySlots() {
        for (int slot = 0; slot < LimitMeInterfaceBlockEntity.SLOT_COUNT; slot++) {
            var marked = this.blockEntity.getMarkedItem(slot);
            if (marked.isEmpty()) {
                this.displaySlotInventory.setItem(slot, ItemStack.EMPTY);
                continue;
            }

            var amount = this.blockEntity.getMarkedItemAmountInNetwork(slot);
            if (amount <= 0) {
                this.displaySlotInventory.setItem(slot, ItemStack.EMPTY);
                continue;
            }

            int displayCount = (int) Math.min(marked.getMaxStackSize(), amount);
            this.displaySlotInventory.setItem(slot, marked.copyWithCount(displayCount));
        }
    }

    private void addConfigSlots() {
        for (int col = 0; col < LimitMeInterfaceBlockEntity.SLOT_COUNT; col++) {
            this.addSlot(new FakeSlot(this.blockEntity.getMarkerInternalInventory(), col), SlotSemantics.CONFIG);
        }
    }

    private void addDisplaySlots() {
        for (int col = 0; col < LimitMeInterfaceBlockEntity.SLOT_COUNT; col++) {
            var slot = new AppEngSlot(this.displaySlotInternalInventory, col) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            };
            slot.setHideAmount(true);
            this.addSlot(slot, SlotSemantics.STORAGE);
        }
    }

    private static boolean isConfigSlot(int slotId) {
        return slotId >= CONFIG_SLOT_START && slotId < DISPLAY_SLOT_START;
    }

    private static boolean isDisplaySlot(int slotId) {
        return slotId >= DISPLAY_SLOT_START && slotId < PLAYER_SLOT_START;
    }

    private static int clampSlot(int slot) {
        return Math.max(0, Math.min(LimitMeInterfaceBlockEntity.SLOT_COUNT - 1, slot));
    }

    private static int lowPart(long value) {
        return (int) value;
    }

    private static int highPart(long value) {
        return (int) (value >>> 32);
    }

    private static long composeLong(int low, int high) {
        return (high & 0xFFFFFFFFL) << 32 | (low & 0xFFFFFFFFL);
    }

    private static LimitMeInterfaceBlockEntity getBlockEntity(Inventory playerInventory, BlockPos pos) {
        var blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof LimitMeInterfaceBlockEntity limitMeInterface) {
            return limitMeInterface;
        }
        throw new IllegalStateException("Expected Limit Me Interface block entity at " + pos);
    }
}
