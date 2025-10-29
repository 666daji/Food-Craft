package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.block.entity.CuttingBoardBlockEntity;
import org.foodcraft.block.entity.UpPlaceBlockEntity;
import org.jetbrains.annotations.Nullable;

public class CuttingBoardBlock extends UpPlaceBlock {
    protected static final VoxelShape SHAPE = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 0.125, 1.0);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public CuttingBoardBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public boolean canFetched(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        return handStack.isEmpty() && !blockEntity.isEmpty();
    }

    @Override
    public boolean canPlace(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        return blockEntity.isEmpty() && !handStack.isEmpty();
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CuttingBoardBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (blockEntity instanceof CuttingBoardBlockEntity cuttingBoard) {
            // 1. 先尝试切割操作（手持剑且菜板上有物品）
            if (handStack.getItem() instanceof SwordItem && !cuttingBoard.isEmpty()) {
                ActionResult cutResult = cuttingBoard.tryCutItem(player, handStack);
                if (cutResult.isAccepted()) {
                    return cutResult;
                }
            }

            // 2. 如果切割失败或条件不满足，执行父类逻辑（取出和放置）
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