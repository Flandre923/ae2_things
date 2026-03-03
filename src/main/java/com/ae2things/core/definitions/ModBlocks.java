package com.ae2things.core.definitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.ae2things.Ae2ThingsMod;
import com.ae2things.block.LimitMeInterfaceBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks DR = DeferredRegister.createBlocks(Ae2ThingsMod.MODID);

    private static final List<RegisteredBlock<?>> BLOCKS = new ArrayList<>();

    public static final RegisteredBlock<Block> LIMIT_ME_INTERFACE = block(
            "Limit Me Interface",
            "limit_me_interface",
            () -> new LimitMeInterfaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()));

    private ModBlocks() {
    }

    public static List<RegisteredBlock<?>> getBlocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    private static <T extends Block> RegisteredBlock<T> block(String englishName, String id, Supplier<T> blockSupplier) {
        var deferredBlock = DR.register(id, blockSupplier);
        var deferredItem = Ae2ThingsMod.ITEMS.registerSimpleBlockItem(id, deferredBlock);
        var definition = new RegisteredBlock<>(englishName, deferredBlock, deferredItem);
        BLOCKS.add(definition);
        return definition;
    }

    public record RegisteredBlock<T extends Block>(
            String englishName,
            DeferredBlock<T> block,
            DeferredItem<BlockItem> item) {
    }
}
