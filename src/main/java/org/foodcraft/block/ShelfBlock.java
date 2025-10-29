package org.foodcraft.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.dfood.tag.ModTags;
import org.foodcraft.block.entity.ShelfBlockEntity;
import org.foodcraft.block.entity.UpPlaceBlockEntity;
import org.jetbrains.annotations.Nullable;

public class ShelfBlock extends UpPlaceBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(0,0,0,8,5,16);
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0,0,0,16,5,8);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(8,0,0,16,5,16);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0,0,8,16,5,16);

    public ShelfBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState()
                .with(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = state.get(FACING);
        return switch (direction) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> throw new IllegalStateException("Unexpected direction: " + direction);
        };
    }

    @Override
    public boolean canFetched(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        if (blockEntity.isEmpty()) {
            return false;
        }

        ShelfBlockEntity shelf = (ShelfBlockEntity) blockEntity;

        if (blockEntity.isFull() && (!shelf.hasEmptyFlowerPot() || !shelf.canInsertFlower(handStack))){
            return true;
        }

        if (shelf.canInsertFlower(handStack) && !shelf.hasEmptyFlowerPot()){
            return true;
        }

        // 如果手持可放入物品，不允许取出（应该优先放置）
        if (shelf.isValidItem(handStack) || (shelf.canInsertFlower(handStack) && shelf.hasEmptyFlowerPot())) {
            return false;
        }

        // 空手或手持不可放入物品时，允许取出
        return handStack.isEmpty() || !shelf.isValidItem(handStack);
    }

    @Override
    public boolean canPlace(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        if (handStack.isEmpty()) {
            return false;
        }

        ShelfBlockEntity shelf = (ShelfBlockEntity) blockEntity;

        // 如果是花，检查是否有空花盆
        if (shelf.canInsertFlower(handStack)) {
            return shelf.hasEmptyFlowerPot();
        }

        // 其他可放入物品，检查架子是否未满
        return shelf.isValidItem(handStack) && !blockEntity.isFull();
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos checkPos = pos.offset(state.get(FACING));
        return !world.getBlockState(checkPos).isIn(ModTags.FOOD_PLACE);
    }

    @Override
    public BlockState getStateForNeighborUpdate(
            BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos
    ) {
        return !state.canPlaceAt(world, pos)
                ? Blocks.AIR.getDefaultState()
                : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    /**
     * 当方块被替换或破坏时的处理
     * 使用自定义的掉落物逻辑来处理花盆中的花
     */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ShelfBlockEntity shelfBlockEntity) {
                // 使用自定义的掉落物逻辑
                if (!world.isClient()) {
                    ItemScatterer.spawn(world, pos, shelfBlockEntity.getDroppedStacks());
                    shelfBlockEntity.clear();
                    world.updateComparators(pos, this);
                }
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ShelfBlockEntity(pos, state);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
}
