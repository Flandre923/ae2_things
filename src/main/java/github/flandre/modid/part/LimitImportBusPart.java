package github.flandre.modid.part;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.automation.ImportBusPart;
import appeng.util.ConfigInventory;
import github.flandre.modid.menu.LimitIOBusMenu;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public class LimitImportBusPart extends ImportBusPart {
    private static final ResourceLocation MODEL_BASE = ResourceLocation.parse("ae2_gadgetry:part/limit_import_bus_base");
    private static final String LIMIT_CONFIG_TAG = "limitConfig";

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE,
            ResourceLocation.parse("ae2_gadgetry:part/limit_import_bus_off"));

    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE,
            ResourceLocation.parse("ae2_gadgetry:part/limit_import_bus_on"));

    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
            ResourceLocation.parse("ae2_gadgetry:part/limit_import_bus_has_channel"));

    private final ConfigInventory limitConfig = ConfigInventory.configStacks(63)
            .supportedType(AEKeyType.items())
            .allowOverstacking(true)
            .changeListener(this::onConfigChanged)
            .build();

    public LimitImportBusPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public ConfigInventory getConfig() {
        return this.limitConfig != null ? this.limitConfig : super.getConfig();
    }

    @Override
    public void writeToNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.writeToNBT(data, registries);
        this.limitConfig.writeToChildTag(data, LIMIT_CONFIG_TAG, registries);
    }

    @Override
    public void readFromNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.readFromNBT(data, registries);
        if (data.contains(LIMIT_CONFIG_TAG, Tag.TAG_LIST)) {
            this.limitConfig.readFromChildTag(data, LIMIT_CONFIG_TAG, registries);
        }
    }

    @Override
    protected boolean doBusWork(IGrid grid) {
        var level = getLevel();
        if (level == null) {
            return false;
        }

        var self = this.getHost().getBlockEntity();
        var targetPos = self.getBlockPos().relative(this.getSide());
        var targetSide = getSide().getOpposite();
        IItemHandler adjacent = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, targetSide);
        if (adjacent == null) {
            return false;
        }

        int operationsRemaining = super.getOperationsPerTick();
        boolean hasDoneWork = false;

        for (int slot = 0; slot < adjacent.getSlots() && operationsRemaining > 0; slot++) {
            var stackInSlot = adjacent.getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                continue;
            }

            var itemKey = AEItemKey.of(stackInSlot);
            if (itemKey == null) {
                continue;
            }

            var rule = getRule(itemKey);
            if (rule == null) {
                continue;
            }

            long networkAmount = getNetworkAmount(grid, itemKey);
            long remaining = rule.limit() - networkAmount;
            if (remaining <= 0) {
                continue;
            }

            long transferFactor = itemKey.getAmountPerOperation();
            long allowedAmount = Math.min(remaining, (long) operationsRemaining * transferFactor);
            if (allowedAmount <= 0) {
                continue;
            }

            var simulatedExtract = adjacent.extractItem(slot, (int) Math.min(Integer.MAX_VALUE, allowedAmount), true);
            if (simulatedExtract.isEmpty()) {
                continue;
            }

            var simulatedKey = AEItemKey.of(simulatedExtract);
            if (!itemKey.equals(simulatedKey)) {
                continue;
            }

            long acceptable = grid.getStorageService().getInventory().insert(itemKey, simulatedExtract.getCount(),
                    Actionable.SIMULATE, this.source);
            if (acceptable <= 0) {
                continue;
            }

            int extractAmount = (int) Math.min(simulatedExtract.getCount(), acceptable);
            var extracted = adjacent.extractItem(slot, extractAmount, false);
            if (extracted.isEmpty()) {
                continue;
            }

            var extractedKey = AEItemKey.of(extracted);
            if (!itemKey.equals(extractedKey)) {
                adjacent.insertItem(slot, extracted, false);
                continue;
            }

            long inserted = grid.getStorageService().getInventory().insert(itemKey, extracted.getCount(),
                    Actionable.MODULATE, this.source);
            if (inserted <= 0) {
                adjacent.insertItem(slot, extracted, false);
                continue;
            }

            if (inserted < extracted.getCount()) {
                var leftover = itemKey.toStack((int) (extracted.getCount() - inserted));
                adjacent.insertItem(slot, leftover, false);
            }

            operationsRemaining -= getUsedOperations(itemKey, inserted);
            hasDoneWork = true;
        }

        return hasDoneWork;
    }

    private int getUsedOperations(AEItemKey itemKey, long transferredAmount) {
        long transferFactor = itemKey.getAmountPerOperation();
        long usedOperations = Math.max(1L, (transferredAmount + transferFactor - 1) / transferFactor);
        return (int) Math.min(Integer.MAX_VALUE, usedOperations);
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

    @Override
    protected MenuType<?> getMenuType() {
        return LimitIOBusMenu.IMPORT_TYPE;
    }

    private long getNetworkAmount(IGrid grid, AEItemKey itemKey) {
        return grid.getStorageService().getCachedInventory().get(itemKey);
    }

    private @Nullable LimitRule getRule(AEItemKey itemKey) {
        long totalLimit = 0;
        for (int slot = 0; slot < this.availableSlots(); slot++) {
            GenericStack stack = this.limitConfig.getStack(slot);
            if (stack == null || !(stack.what() instanceof AEItemKey configuredKey)) {
                continue;
            }

            if (configuredKey.equals(itemKey)) {
                totalLimit += Math.max(0, stack.amount());
            }
        }

        return totalLimit > 0 ? new LimitRule(itemKey, totalLimit) : null;
    }

    private void onConfigChanged() {
        getHost().markForSave();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    private record LimitRule(AEItemKey itemKey, long limit) {
    }
}
