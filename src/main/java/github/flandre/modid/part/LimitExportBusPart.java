package github.flandre.modid.part;

import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.items.parts.PartModels;
import appeng.parts.automation.ExportBusPart;
import appeng.parts.PartModel;
import appeng.util.ConfigInventory;
import appeng.core.definitions.AEItems;
import github.flandre.modid.menu.LimitIOBusMenu;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class LimitExportBusPart extends ExportBusPart {
    private static final ResourceLocation MODEL_BASE = ResourceLocation.parse("ae2_gadgetry:part/limit_export_bus_base");
    private static final String LIMIT_CONFIG_TAG = "limitConfig";

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE,
            ResourceLocation.parse("ae2_gadgetry:part/limit_export_bus_off"));

    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE,
            ResourceLocation.parse("ae2_gadgetry:part/limit_export_bus_on"));

    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
            ResourceLocation.parse("ae2_gadgetry:part/limit_export_bus_has_channel"));

    private final ConfigInventory limitConfig = ConfigInventory.configStacks(63)
            .supportedType(AEKeyType.items())
            .allowOverstacking(true)
            .changeListener(this::onConfigChanged)
            .build();

    public LimitExportBusPart(IPartItem<?> partItem) {
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
        var storageService = grid.getStorageService();
        var fuzzyMode = this.getConfigManager().getSetting(Settings.FUZZY_MODE);
        var schedulingMode = this.getConfigManager().getSetting(Settings.SCHEDULING_MODE);
        int operationsRemaining = super.getOperationsPerTick();
        boolean hasDoneWork = false;

        int x = 0;
        for (; x < this.availableSlots() && operationsRemaining > 0; x++) {
            int slotToExport = this.getStartingSlot(schedulingMode, x);
            var rule = getRule(slotToExport);
            if (rule == null) {
                continue;
            }

            if (isUpgradedWith(AEItems.FUZZY_CARD)) {
                for (var fuzzyMatch : ImmutableList.copyOf(storageService.getCachedInventory()
                        .findFuzzy(rule.itemKey(), fuzzyMode))) {
                    if (!(fuzzyMatch.getKey() instanceof AEItemKey fuzzyKey)) {
                        continue;
                    }

                    long transferred = transferExcess(grid, fuzzyKey, getConfiguredLimit(fuzzyKey), operationsRemaining);
                    if (transferred > 0) {
                        operationsRemaining -= getUsedOperations(fuzzyKey, transferred);
                        hasDoneWork = true;
                    }

                    if (operationsRemaining <= 0) {
                        break;
                    }
                }
            } else {
                long transferred = transferExcess(grid, rule.itemKey(), rule.limit(), operationsRemaining);
                if (transferred > 0) {
                    operationsRemaining -= getUsedOperations(rule.itemKey(), transferred);
                    hasDoneWork = true;
                }
            }
        }

        if (hasDoneWork) {
            this.updateSchedulingMode(schedulingMode, x);
        }

        return hasDoneWork;
    }

    private long transferExcess(IGrid grid, AEItemKey itemKey, long configuredLimit, int operationsRemaining) {
        if (configuredLimit <= 0 || operationsRemaining <= 0) {
            return 0;
        }

        long current = getNetworkAmount(grid, itemKey);
        long excess = current - configuredLimit;
        if (excess <= 0) {
            return 0;
        }

        long transferFactor = itemKey.getAmountPerOperation();
        long requestedAmount = Math.min(excess, (long) operationsRemaining * transferFactor);
        if (requestedAmount <= 0) {
            return 0;
        }

        long accepted = getExportStrategy().push(itemKey, requestedAmount, Actionable.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }

        long extracted = grid.getStorageService().getInventory().extract(itemKey, accepted, Actionable.MODULATE,
                this.source);
        if (extracted <= 0) {
            return 0;
        }

        long pushed = getExportStrategy().push(itemKey, extracted, Actionable.MODULATE);
        if (pushed < extracted) {
            grid.getStorageService().getInventory().insert(itemKey, extracted - pushed, Actionable.MODULATE, this.source);
        }

        return pushed;
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
        return LimitIOBusMenu.EXPORT_TYPE;
    }

    private long getNetworkAmount(IGrid grid, AEItemKey itemKey) {
        return grid.getStorageService().getCachedInventory().get(itemKey);
    }

    private @Nullable LimitRule getRule(int slot) {
        GenericStack stack = this.limitConfig.getStack(slot);
        if (stack == null || !(stack.what() instanceof AEItemKey itemKey)) {
            return null;
        }

        long limit = Math.max(0, stack.amount());
        if (limit <= 0) {
            return null;
        }

        return new LimitRule(itemKey, limit);
    }

    private long getConfiguredLimit(AEItemKey itemKey) {
        long totalLimit = 0;
        for (int slot = 0; slot < this.availableSlots(); slot++) {
            var rule = getRule(slot);
            if (rule != null && rule.itemKey().equals(itemKey)) {
                totalLimit += rule.limit();
            }
        }
        return totalLimit;
    }

    private void onConfigChanged() {
        getHost().markForSave();
        getMainNode().ifPresent((grid, node) -> grid.getTickManager().alertDevice(node));
    }

    private record LimitRule(AEItemKey itemKey, long limit) {
    }
}
