package github.flandre.modid.block;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import github.flandre.modid.blockentity.LimitMeInterfaceBlockEntity;
import github.flandre.modid.core.definitions.ModBlockEntities;

import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LimitMeInterfaceBlock extends BaseEntityBlock implements EntityBlock {
    public static final MapCodec<LimitMeInterfaceBlock> CODEC = simpleCodec(LimitMeInterfaceBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE_NORTH = Block.box(2, 2, 0, 14, 14, 4);
    private static final VoxelShape SHAPE_SOUTH = Block.box(2, 2, 12, 14, 14, 16);
    private static final VoxelShape SHAPE_WEST = Block.box(0, 2, 2, 4, 14, 14);
    private static final VoxelShape SHAPE_EAST = Block.box(12, 2, 2, 16, 14, 14);

    public LimitMeInterfaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return super.canBeReplaced(state, useContext);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof LimitMeInterfaceBlockEntity limitMeInterfaceBlockEntity) {
                limitMeInterfaceBlockEntity.openMenu(serverPlayer, MenuLocators.forBlockEntity(limitMeInterfaceBlockEntity));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        copyBlockEntityDataFromItem(level, pos, stack);
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof LimitMeInterfaceBlockEntity limitMeInterfaceBlockEntity) {
                limitMeInterfaceBlockEntity.setOwningPlayer(player);
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return getFacingShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return getFacingShape(state);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return getFacingShape(state);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos) {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.LIMIT_ME_INTERFACE.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null
                : createTickerHelper(blockEntityType, ModBlockEntities.LIMIT_ME_INTERFACE.get(),
                        LimitMeInterfaceBlockEntity::serverTick);
    }

    private static VoxelShape getFacingShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    private static void copyBlockEntityDataFromItem(Level level, BlockPos pos, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }

        var customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData == null) {
            return;
        }

        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof LimitMeInterfaceBlockEntity limitInterface) {
            CompoundTag data = customData.copyTag();
            blockEntity.loadWithComponents(data, level.registryAccess());
            limitInterface.setChanged();
        }
    }
}

