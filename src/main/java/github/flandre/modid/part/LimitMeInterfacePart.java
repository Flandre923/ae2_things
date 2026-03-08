package github.flandre.modid.part;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigManager;
import appeng.helpers.InterfaceLogic;
import appeng.items.parts.PartModels;
import appeng.menu.locator.MenuHostLocator;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;
import github.flandre.modid.menu.LimitMeInterfaceMenu;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

public class LimitMeInterfacePart extends AEBasePart implements LimitMeInterfaceHost {
    private static final ResourceLocation MODEL_BASE = ResourceLocation.parse("ae2_gadgetry:part/limit_me_interface_base");

    private static final IGridNodeListener<LimitMeInterfacePart> NODE_LISTENER = new NodeListener<>() {
        @Override
        public void onGridChanged(LimitMeInterfacePart nodeOwner, IGridNode node) {
            super.onGridChanged(nodeOwner, node);
            nodeOwner.getInterfaceLogic().gridChanged();
        }

        @Override
        public void onSaveChanges(LimitMeInterfacePart nodeOwner, IGridNode node) {
            nodeOwner.getHost().markForSave();
        }
    };

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE,
            ResourceLocation.parse("ae2_gadgetry:part/limit_me_interface_off"));

    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE,
            ResourceLocation.parse("ae2_gadgetry:part/limit_me_interface_on"));

    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
            ResourceLocation.parse("ae2_gadgetry:part/limit_me_interface_has_channel"));

    private final InterfaceLogic interfaceLogic = createInterfaceLogic();
    private final LimitMeInterfaceStorage storage = new LimitMeInterfaceStorage(new LimitMeInterfaceStorage.HostAccess() {
        @Override
        public void onContentsChanged() {
            saveChanges();
        }

        @Override
        public IGrid getGrid() {
            return resolveGrid();
        }
    });

    public LimitMeInterfacePart(IPartItem<?> partItem) {
        super(partItem);
    }

    protected InterfaceLogic createInterfaceLogic() {
        return new InterfaceLogic(getMainNode(), this, getPartItem().asItem());
    }

    @Override
    protected IManagedGridNode createMainNode() {
        return GridHelper.createManagedNode(this, NODE_LISTENER);
    }

    @Override
    protected void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        if (getMainNode().hasGridBooted()) {
            this.interfaceLogic.notifyNeighbors();
        }
    }

    @Override
    public void saveChanges() {
        getHost().markForSave();
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
    public boolean isMenuValid(Player player) {
        if (getLevel() == null || getHost() == null || getHost().getPart(getSide()) != this) {
            return false;
        }
        var pos = getBlockEntity().getBlockPos();
        return player.level() == getLevel() && player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator) {
        appeng.menu.MenuOpener.open(LimitMeInterfaceMenu.TYPE, player, locator);
    }

    @Override
    public InternalInventory getMarkerInternalInventory() {
        return this.storage.getMarkerInternalInventory();
    }

    @Override
    public IItemHandler getExternalItemHandler() {
        return this.storage.getExternalItemHandler();
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(5, 5, 12, 11, 11, 14);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 4;
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!player.level().isClientSide()) {
            openMenu(player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.readFromNBT(tag, registries);
        this.interfaceLogic.readFromNBT(tag, registries);
        this.storage.readFromNBT(tag, registries);
    }

    @Override
    public void writeToNBT(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeToNBT(tag, registries);
        this.interfaceLogic.writeToNBT(tag, registries);
        this.storage.writeToNBT(tag, registries);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        this.interfaceLogic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.interfaceLogic.clearContent();
        this.storage.clearContent();
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return MODELS_ON;
        } else {
            return MODELS_OFF;
        }
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(UPGRADES)) {
            return this.interfaceLogic.getUpgrades();
        }
        return super.getSubInventory(id);
    }

    public ItemStack getMainMenuIcon() {
        return new ItemStack(getPartItem());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("item.ae2_gadgetry.limit_me_interface_part");
    }

    @Override
    public ItemStack getMarkedItem(int slot) {
        return this.storage.getMarkedItem(slot);
    }

    @Override
    public void setMarkedItem(int slot, ItemStack stack) {
        this.storage.setMarkedItem(slot, stack);
    }

    @Override
    public void clearMarkedItem(int slot) {
        this.storage.clearMarkedItem(slot);
    }

    @Override
    public boolean isSlotMarked(int slot) {
        return this.storage.isSlotMarked(slot);
    }

    @Override
    public int getMarkedSlotMask() {
        return this.storage.getMarkedSlotMask();
    }

    @Override
    public long getLimit(int slot) {
        return this.storage.getLimit(slot);
    }

    @Override
    public void changeLimit(int slot, long delta) {
        this.storage.changeLimit(slot, delta);
    }

    @Override
    public void setUnlimited(int slot) {
        this.storage.setUnlimited(slot);
    }

    @Override
    public void setLimit(int slot, long value) {
        this.storage.setLimit(slot, value);
    }

    @Override
    public long getMarkedItemAmountInNetwork(int slot) {
        return this.storage.getMarkedItemAmountInNetwork(slot);
    }

    @Override
    public long insertMarkedItem(int slot, ItemStack stack, long requestedAmount) {
        return this.storage.insertMarkedItem(slot, stack, requestedAmount);
    }

    @Override
    public long insertMarkedItemSimulate(int slot, ItemStack stack, long requestedAmount) {
        return this.storage.insertMarkedItemSimulate(slot, stack, requestedAmount);
    }

    @Override
    public ItemStack extractMarkedItem(int slot, long requestedAmount) {
        return this.storage.extractMarkedItem(slot, requestedAmount);
    }

    @Override
    public ItemStack extractMarkedItemSimulate(int slot, long requestedAmount) {
        return this.storage.extractMarkedItemSimulate(slot, requestedAmount);
    }

    private @Nullable IGrid resolveGrid() {
        if (this.getLevel() == null || this.getLevel().isClientSide || !this.getMainNode().isReady()) {
            return null;
        }
        return this.getMainNode().getGrid();
    }
}
