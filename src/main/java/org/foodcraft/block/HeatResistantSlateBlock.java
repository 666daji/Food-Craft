package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.block.entity.HeatResistantSlateBlockEntity;
import org.foodcraft.block.multi.MultiBlockHelper;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.jetbrains.annotations.Nullable;

public class HeatResistantSlateBlock extends BlockWithEntity {

    public HeatResistantSlateBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        if (!world.isClient) {
            // 处理核心方块放置
            MultiBlockHelper.onCoreBlockPlaced(world, pos, this);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            // 方块被替换或破坏
            if (!world.isClient) {
                // 处理核心方块破坏
                MultiBlockHelper.onCoreBlockBroken(world, pos, this);
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos fromPos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, fromPos, notify);

        if (!world.isClient) {
            // 处理相邻方块更新，检查多方块结构完整性
            MultiBlockHelper.onNeighborUpdate(world, pos, this);
        }
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HeatResistantSlateBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, (BlockEntityType<? extends HeatResistantSlateBlockEntity>) ModBlockEntityTypes.HEAT_RESISTANT_SLATE, HeatResistantSlateBlockEntity::tick);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}