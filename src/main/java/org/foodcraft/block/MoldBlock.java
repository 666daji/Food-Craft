package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.entity.HeatResistantSlateBlockEntity;
import org.foodcraft.block.entity.MoldBlockEntity;
import org.foodcraft.block.entity.UpPlaceBlockEntity;
import org.foodcraft.item.MoldContentItem;
import org.foodcraft.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class MoldBlock extends UpPlaceBlock {
    public static final Logger LOGGER = FoodCraft.LOGGER;
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final VoxelShape CAKE_EMBRYO_MOLD_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 9, 16);
    public static final VoxelShape TOAST_EMBRYO_MOLD_SHAPE_X = Block.createCuboidShape(3, 0, 0, 13, 8, 16);
    public static final VoxelShape TOAST_EMBRYO_MOLD_SHAPE_Z = Block.createCuboidShape(0, 0, 3, 16, 8, 13);

    /** 是否可以放置在耐热石板上 */
    public final boolean canPlaceSlate;

    public MoldBlock(Settings settings, boolean canPlaceSlate) {
        super(settings, new UpSounds(SoundEvents.ENTITY_SALMON_DEATH, SoundEvents.ENTITY_SALMON_DEATH));
        this.canPlaceSlate = canPlaceSlate;
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.getBlock() == ModBlocks.TOAST_EMBRYO_MOLD) {
            return state.get(FACING).getAxis() ==
                    Direction.Axis.Z ? TOAST_EMBRYO_MOLD_SHAPE_Z : TOAST_EMBRYO_MOLD_SHAPE_X;
        }
        return CAKE_EMBRYO_MOLD_SHAPE;
    }

    /**
     * 防止因为放置在耐热石板上而产生的无限递归
     */
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof HeatResistantSlateBlockEntity) {
            return getBaseShape(state, world, pos, context);
        }
        return super.getOutlineShape(state, world, pos, context);
    }

    @Override
    public boolean canFetched(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        return !blockEntity.isEmpty();
    }

    @Override
    public boolean canPlace(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        return blockEntity.isValidItem(handStack);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient && itemStack.getItem() instanceof MoldContentItem moldContentItem){
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof MoldBlockEntity moldBlockEntity){
                moldContentItem.toMoldBlock(moldBlockEntity, itemStack);
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MoldBlockEntity moldBlockEntity && moldBlockEntity.isEmpty()){
            super.onStateReplaced(state, world, pos, newState, moved);
        }

        if (state.hasBlockEntity() && !state.isOf(newState.getBlock())) {
            world.removeBlockEntity(pos);
        }
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        BlockEntity blockEntity = builder.get(LootContextParameters.BLOCK_ENTITY);

        if (blockEntity instanceof MoldBlockEntity moldBlockEntity) {
            // 如果模具中有内容，返回对应的模具物品
            if (!moldBlockEntity.isEmpty()) {
                ItemStack result =  MoldContentItem.getTargetStack(state.getBlock(), moldBlockEntity.getInputStack());
                return List.of(result);
            }
        }

        // 空模具掉落自身
        return super.getDroppedStacks(state, builder);
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
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MoldBlockEntity(pos, state);
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
