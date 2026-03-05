package github.flandre.modid.core.definitions;

import github.flandre.modid.Ae2GadgetryMod;
import github.flandre.modid.blockentity.LimitMeInterfaceBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> DR = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,
            Ae2GadgetryMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LimitMeInterfaceBlockEntity>> LIMIT_ME_INTERFACE = DR
            .register("limit_me_interface",
                    () -> BlockEntityType.Builder.of(LimitMeInterfaceBlockEntity::new,
                            ModBlocks.LIMIT_ME_INTERFACE.block().get()).build(null));

    private ModBlockEntities() {
    }
}

