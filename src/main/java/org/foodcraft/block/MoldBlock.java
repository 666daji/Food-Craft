package org.foodcraft.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.block.entity.MoldBlockEntity;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.ShapedDoughContent;
import org.foodcraft.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MoldBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final VoxelShape CAKE_EMBRYO_MOLD_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 9, 16);
    public static final VoxelShape TOAST_EMBRYO_MOLD_SHAPE_X = Block.createCuboidShape(3, 0, 0, 13, 8, 16);
    public static final VoxelShape TOAST_EMBRYO_MOLD_SHAPE_Z = Block.createCuboidShape(0, 0, 3, 16, 8, 13);

    public MoldBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof MoldBlockEntity moldBlockEntity)) {
            return ActionResult.PASS;
        }

        ItemStack contentStack = moldBlockEntity.getAndClearResultStack();
        ItemStack heldStack = player.getStackInHand(hand);

        if (!contentStack.isEmpty()) {
            player.giveItemStack(contentStack);
            return ActionResult.SUCCESS;
        }

        if (moldBlockEntity.addDough(heldStack)) {
            if (!player.isCreative()) {
                heldStack.decrement(1);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.getBlock() == ModBlocks.TOAST_EMBRYO_MOLD) {
            return state.get(FACING).getAxis() ==
                    Direction.Axis.Z ? TOAST_EMBRYO_MOLD_SHAPE_Z : TOAST_EMBRYO_MOLD_SHAPE_X;
        }
        return CAKE_EMBRYO_MOLD_SHAPE;
    }


    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        AbstractContent content = ContainerUtil.extractContent(itemStack);
        BlockEntity entity = world.getBlockEntity(pos);

        if (content instanceof ShapedDoughContent shapedDough && entity instanceof MoldBlockEntity moldBlockEntity) {
            moldBlockEntity.setShapedDough(shapedDough);
        }
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        List<ItemStack> result = super.getDroppedStacks(state, builder);
        BlockEntity entity = builder.get(LootContextParameters.BLOCK_ENTITY);

        if (entity instanceof MoldBlockEntity moldBlockEntity) {
            ShapedDoughContent content = moldBlockEntity.getShapedDough();
            if (content != null) {
                result.forEach(stack -> ContainerUtil.replaceContent(stack, content));
            }
        }

        return result;
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

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MoldBlockEntity(pos, state);
    }
}
