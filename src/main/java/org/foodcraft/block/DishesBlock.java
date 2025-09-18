package org.foodcraft.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.dfood.tag.ModTags;
import org.foodcraft.block.entity.DishesBlockEntity;
import org.foodcraft.block.entity.UpPlaceBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DishesBlock extends UpPlaceBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<DishesType> TYPE = EnumProperty.of("type", DishesType.class);
    private static final VoxelShape BASE_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 1.5, 16.0);

    private static final DoubleBlockProperties.PropertyRetriever<DishesBlockEntity, Optional<Inventory>> INVENTORY_RETRIEVER =
            new DoubleBlockProperties.PropertyRetriever<>() {
                @Override
                public Optional<Inventory> getFromBoth(DishesBlockEntity first, DishesBlockEntity second) {
                    return Optional.of(new DoubleInventory(first, second));
                }

                @Override
                public Optional<Inventory> getFrom(DishesBlockEntity single) {
                    return Optional.of(single);
                }

                @Override
                public Optional<Inventory> getFallback() {
                    return Optional.empty();
                }
            };

    public DishesBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(TYPE, DishesType.SINGLE));
    }

    @Override
    public VoxelShape getBaseShape() {
        return BASE_SHAPE;
    }

    @Override
    public boolean canFetched(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        Item contentItem = blockEntity.getContentItem();

        // 如果玩家手持物品为空或者与容器中物品不同，允许取出
        return handStack.isEmpty() || (contentItem != null && handStack.getItem() != contentItem);
    }

    @Override
    public boolean canPlace(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        // 检查物品是否可以放入盘子
        if (blockEntity instanceof DishesBlockEntity dishesBlockEntity) {
            return dishesBlockEntity.isValidItem(handStack);
        }
        return false;
    }

    @Override
    public BlockState getStateForNeighborUpdate(
            BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos
    ) {
        // 首先检查方块是否可以放置在当前位置，如果不能则返回空气
        if (!state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }

        if (neighborState.isOf(this) && direction.getAxis().isHorizontal()) {
            DishesType neighborType = neighborState.get(TYPE);
            if (state.get(TYPE) == DishesType.SINGLE
                    && neighborType != DishesType.SINGLE
                    && state.get(FACING) == neighborState.get(FACING)
                    && getFacing(neighborState) == direction.getOpposite()) {
                return state.with(TYPE, neighborType.getOpposite());
            }
        } else if (getFacing(state) == direction) {
            return state.with(TYPE, DishesType.SINGLE);
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    /**
     * 获取盘子方块的面朝方向
     */
    public static Direction getFacing(BlockState state) {
        Direction direction = state.get(FACING);
        return state.get(TYPE) == DishesType.LEFT ?
                direction.rotateYClockwise() : direction.rotateYCounterclockwise();
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DishesBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    /**
     * 获取相邻盘子方块的方向
     */
    @Nullable
    private Direction getNeighborDishesDirection(ItemPlacementContext ctx, Direction dir) {
        BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(dir));
        return blockState.isOf(this) && blockState.get(TYPE) == DishesType.SINGLE ?
                blockState.get(FACING) : null;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        DishesType dishesType = DishesType.SINGLE;
        Direction direction = ctx.getHorizontalPlayerFacing();
        boolean isSneaking = ctx.shouldCancelInteraction();
        Direction side = ctx.getSide();

        if (side.getAxis().isHorizontal() && isSneaking) {
            Direction neighborDirection = this.getNeighborDishesDirection(ctx, side.getOpposite());
            if (neighborDirection != null && neighborDirection.getAxis() != side.getAxis()) {
                direction = neighborDirection;
                dishesType = neighborDirection.rotateYCounterclockwise() == side.getOpposite() ?
                        DishesType.RIGHT : DishesType.LEFT;
            }
        }

        if (dishesType == DishesType.SINGLE && !isSneaking) {
            if (direction == this.getNeighborDishesDirection(ctx, direction.rotateYClockwise())) {
                dishesType = DishesType.LEFT;
            } else if (direction == this.getNeighborDishesDirection(ctx, direction.rotateYCounterclockwise())) {
                dishesType = DishesType.RIGHT;
            }
        }

        return this.getDefaultState().with(FACING, direction).with(TYPE, dishesType);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos downPos = pos.down();
        return !world.getBlockState(downPos).isIn(ModTags.FOOD_PLACE);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE);
    }

    /**
     * 盘子类型枚举
     */
    public enum DishesType implements StringIdentifiable {
        SINGLE("single"),
        LEFT("left"),
        RIGHT("right");

        private final String name;

        DishesType(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }

        public DishesType getOpposite() {
            return switch (this) {
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
                default -> SINGLE;
            };
        }
    }
}