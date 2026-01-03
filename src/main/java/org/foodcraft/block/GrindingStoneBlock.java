package org.foodcraft.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.foodcraft.block.entity.GrindingStoneBlockEntity;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

public class GrindingStoneBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
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
        return world.isClient ? null : checkType(type, (BlockEntityType<? extends GrindingStoneBlockEntity>) ModBlockEntityTypes.GRINDING_STONE, GrindingStoneBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof GrindingStoneBlockEntity grindingEntity) {
            // 空手时检查
            if (handStack.isEmpty()) {
                // 检查是否可以研磨当前物品
                if (!grindingEntity.canGrindCurrentInput()) {
                    // 如果不能研磨，返还物品给玩家
                    grindingEntity.returnInputToPlayer(player);
                    return ActionResult.SUCCESS;
                }

                // 如果可以研磨，尝试添加能量
                if (grindingEntity.tryAddEnergy(40)) {
                    return ActionResult.SUCCESS;
                } else {
                    player.sendMessage(Text.translatable("grinding_stone.energy.full"), true);
                    return ActionResult.FAIL;
                }
            }
            // 手持物品时尝试添加物品
            else {
                GrindingStoneBlockEntity.AddInputResult result = grindingEntity.addInput(handStack, player);
                return switch (result) {
                    case INVALID -> {
                        player.sendMessage(Text.translatable("grinding_stone.add.refused"), true);
                        yield ActionResult.FAIL;
                    }
                    case FULL -> {
                        player.sendMessage(Text.translatable("grinding_stone.add.full"), true);
                        yield ActionResult.FAIL;
                    }
                    case NOT_ENOUGH -> {
                        player.sendMessage(Text.translatable("grinding_stone.add.not_enough"), true);
                        yield ActionResult.FAIL;
                    }
                    case SUCCESS -> ActionResult.SUCCESS;
                };
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof GrindingStoneBlockEntity grindingEntity) {
                // 掉落所有物品
                dropItems(grindingEntity, world, pos);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    /**
     * 掉落方块实体中的所有物品
     */
    private void dropItems(GrindingStoneBlockEntity blockEntity, World world, BlockPos pos) {
        var items = blockEntity.getItemsToDrop();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(world,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                itemEntity.setToDefaultPickupDelay();
                world.spawnEntity(itemEntity);
            }
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (!world.getBlockState(pos.offset(direction)).isAir()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public BlockState getStateForNeighborUpdate(
            BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos
    ) {
        return !state.canPlaceAt(world, pos)
                ? Blocks.AIR.getDefaultState()
                : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof GrindingStoneBlockEntity grindingStoneBlockEntity && grindingStoneBlockEntity.canPlaySound()){
            world.playSound(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    ModSounds.GRINDING_STONE_GRINDING,
                    SoundCategory.BLOCKS,
                    1.0F,
                    1.0F,
                    true
            );
        }
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing());
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
        builder.add(FACING);
    }
}