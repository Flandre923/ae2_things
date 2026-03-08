package github.flandre.modid.blockentity;

import org.jetbrains.annotations.Nullable;

import github.flandre.modid.core.definitions.ModBlockEntities;
import github.flandre.modid.core.definitions.ModBlocks;
import github.flandre.modid.core.definitions.ModItems;
import github.flandre.modid.menu.LimitMeInterfaceMenu;
import github.flandre.modid.part.LimitMeInterfaceHost;
import github.flandre.modid.part.LimitMeInterfaceStorage;

import appeng.helpers.InterfaceLogic;
import appeng.menu.ISubMenu;
import appeng.menu.locator.MenuHostLocator;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.IConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class LimitMeInterfaceBlockEntity extends BlockEntity implements MenuProvider, IInWorldGridNodeHost, LimitMeInterfaceHost {
    private static final IGridNodeListener<LimitMeInterfaceBlockEntity> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(LimitMeInterfaceBlockEntity nodeOwner, IGridNode node) {
            nodeOwner.setChanged();
        }
    };

    private final IManagedGridNode mainNode;
    private final InterfaceLogic interfaceLogic;
    private final LimitMeInterfaceStorage storage;
    private boolean nodeDestroyed = false;

    public LimitMeInterfaceBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.LIMIT_ME_INTERFACE.get(), pos, blockState);
    }

    protected LimitMeInterfaceBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
        this.mainNode = GridHelper.createManagedNode(this, NODE_LISTENER)
                .setVisualRepresentation(ModBlocks.LIMIT_ME_INTERFACE.item().get())
                .setInWorldNode(true)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setTagName("gridNode");
        this.interfaceLogic = new InterfaceLogic(this.mainNode, this, ModBlocks.LIMIT_ME_INTERFACE.item().get());
        this.storage = new LimitMeInterfaceStorage(new LimitMeInterfaceStorage.HostAccess() {
            @Override
            public void onContentsChanged() {
                LimitMeInterfaceBlockEntity.this.setChanged();
            }

            @Override
            public IGrid getGrid() {
                return resolveGrid();
            }
        });
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
        this.interfaceLogic.writeToNBT(tag, registries);
        this.storage.writeToNBT(tag, registries);
        this.mainNode.saveToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.interfaceLogic.readFromNBT(tag, registries);
        this.storage.readFromNBT(tag, registries);
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
    public boolean isMenuValid(Player player) {
        if (this.isRemoved() || this.level == null) {
            return false;
        }
        return player.level() == this.level && player.distanceToSqr(
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public @Nullable IGridNode getGridNode(Direction dir) {
        return this.mainNode.getNode();
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    @Override
    public InterfaceLogic getInterfaceLogic() {
        return this.interfaceLogic;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.interfaceLogic.getConfigManager();
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    public InternalInventory getMarkerInternalInventory() {
        return this.storage.getMarkerInternalInventory();
    }

    @Override
    public IItemHandler getExternalItemHandler() {
        return this.storage.getExternalItemHandler();
    }

    public IItemHandler getExternalItemHandler(@Nullable Direction side) {
        return this.storage.getExternalItemHandler();
    }

    public ItemStack getMarkedItem(int slot) {
        return this.storage.getMarkedItem(slot);
    }

    public void setMarkedItem(int slot, ItemStack stack) {
        this.storage.setMarkedItem(slot, stack);
    }

    public void clearMarkedItem(int slot) {
        this.storage.clearMarkedItem(slot);
    }

    public boolean isSlotMarked(int slot) {
        return this.storage.isSlotMarked(slot);
    }

    public int getMarkedSlotMask() {
        return this.storage.getMarkedSlotMask();
    }

    public long getLimit(int slot) {
        return this.storage.getLimit(slot);
    }

    public void changeLimit(int slot, long delta) {
        this.storage.changeLimit(slot, delta);
    }

    public void setUnlimited(int slot) {
        this.storage.setUnlimited(slot);
    }

    public void setLimit(int slot, long value) {
        this.storage.setLimit(slot, value);
    }

    public long getMarkedItemAmountInNetwork(int slot) {
        return this.storage.getMarkedItemAmountInNetwork(slot);
    }

    public long insertMarkedItem(int slot, ItemStack stack, long requestedAmount) {
        return this.storage.insertMarkedItem(slot, stack, requestedAmount);
    }

    @Override
    public long insertMarkedItemSimulate(int slot, ItemStack stack, long requestedAmount) {
        return this.storage.insertMarkedItemSimulate(slot, stack, requestedAmount);
    }

    public ItemStack extractMarkedItem(int slot, long requestedAmount) {
        return this.storage.extractMarkedItem(slot, requestedAmount);
    }

    @Override
    public ItemStack extractMarkedItemSimulate(int slot, long requestedAmount) {
        return this.storage.extractMarkedItemSimulate(slot, requestedAmount);
    }

    public void setOwningPlayer(Player player) {
        this.mainNode.setOwningPlayer(player);
        this.setChanged();
    }

    private @Nullable IGrid resolveGrid() {
        if (this.level == null || this.level.isClientSide || !this.mainNode.isReady()) {
            return null;
        }
        return this.mainNode.getGrid();
    }

    protected IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return ModBlocks.LIMIT_ME_INTERFACE.item().get().getDefaultInstance();
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        openMenu(player, subMenu.getLocator());
    }

    private void destroyNode() {
        if (!this.nodeDestroyed) {
            this.mainNode.destroy();
            this.nodeDestroyed = true;
        }
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator) {
        appeng.menu.MenuOpener.open(LimitMeInterfaceMenu.TYPE, player, locator);
    }
}


