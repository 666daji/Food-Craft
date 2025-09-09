package org.foodcraft.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.block.entity.GrindingStoneBlockEntity;
import org.foodcraft.block.entity.ModBlockEntityTypes;
import org.jetbrains.annotations.Nullable;

public class GrindingStoneBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    protected static final VoxelShape TOP = Block.createCuboidShape(0.0,0.0,0.0,16.0,5.0,16.0);
    protected static final VoxelShape BOTTOM = Block.createCuboidShape(1.0,5.0,1.0,15.0,14.0,15.0);

    public GrindingStoneBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.union(BOTTOM,TOP);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GrindingStoneBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, (BlockEntityType<? extends GrindingStoneBlockEntity>) ModBlockEntityTypes.GrindingStone, GrindingStoneBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof GrindingStoneBlockEntity grindingEntity) {
            // 空手时尝试为石磨增加能量
            if (handStack.isEmpty()) {
                if (grindingEntity.tryAddEnergy(40)) {
                    return ActionResult.SUCCESS;
                } else {
                    player.sendMessage(Text.translatable("grinding_stone.energy.full"), true);
                    return ActionResult.FAIL;
                }
            }
            // 手持物品时尝试添加物品
            else {
                ItemStack addStack = handStack.copy();
                addStack.setCount(1);
                GrindingStoneBlockEntity.addInputResult result = grindingEntity.addInput(addStack);
                return switch (result) {
                    case INVALID -> {
                        player.sendMessage(Text.translatable("grinding_stone.add.refused"), true);
                        yield ActionResult.FAIL;
                    }
                    case FULL -> {
                        player.sendMessage(Text.translatable("grinding_stone.add.full"), true);
                        yield ActionResult.FAIL;
                    }
                    case SUCCESS -> {
                        // 如果添加成功，消耗玩家手中的物品
                        if (!player.isCreative()) {
                            handStack.decrement(1);
                        }
                        yield ActionResult.SUCCESS;
                    }
                };
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
