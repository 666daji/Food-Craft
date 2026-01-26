package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.block.entity.CuttingBoardBlockEntity;
import org.foodcraft.block.entity.UpPlaceBlockEntity;
import org.jetbrains.annotations.Nullable;

public class CuttingBoardBlock extends UpPlaceBlock {
    protected static final VoxelShape SHAPE_X = Block.createCuboidShape(0.0, 0.0, 0.5, 16.0, 1.5, 15.5);
    protected static final VoxelShape SHAPE_Z = Block.createCuboidShape(0.5, 0.0, 0.0, 15.5, 1.5, 16.0);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public CuttingBoardBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(FACING).getAxis() == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    public boolean canFetched(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        // 如果切菜流程在进行中，不允许取出物品
        if (blockEntity instanceof CuttingBoardBlockEntity cuttingBoard) {
            if (cuttingBoard.getCuttingProcess().isActive()) {
                return false;
            }
        }

        Item contentItem = blockEntity.getContentItem();

        // 如果玩家手持物品为空或者与容器中物品不同，允许取出
        return handStack.isEmpty() ||
                (contentItem != null && handStack.getItem() != contentItem) || blockEntity.isFull();
    }

    @Override
    public boolean canPlace(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        // 如果切菜流程在进行中，不允许放置新物品
        if (blockEntity instanceof CuttingBoardBlockEntity cuttingBoard) {
            if (cuttingBoard.getCuttingProcess().isActive()) {
                return false;
            }
        }

        return blockEntity.isValidItem(handStack);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CuttingBoardBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof CuttingBoardBlockEntity cuttingBoard) {
            // 尝试切割操作
            if (cuttingBoard.tryCutItem(player, handStack, hand, hit).isAccepted()) {
                return ActionResult.SUCCESS;
            }

            // 如果切割失败或条件不满足，执行父类逻辑（取出和放置）
            return super.onUse(state, world, pos, player, hand, hit);
        }

        return ActionResult.FAIL;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}