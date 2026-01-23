package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.dfood.block.SimpleFoodBlock;
import org.foodcraft.block.entity.PlateBlockEntity;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.DishesContent;
import org.foodcraft.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 表示一个可以摆盘的盘子方块
 */
public class PlateBlock extends SimpleFoodBlock implements BlockEntityProvider {
    /**
     * 表示当前的方块是否已经被盖子覆盖。
     * <p>请不要直接更改该属性的值。</p>
     */
    public static final BooleanProperty IS_COVERED = BooleanProperty.of("is_covered");
    public static final VoxelShape BASE_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 2,16);
    public static final VoxelShape LIB_SHAPE = Block.createCuboidShape(1, 2, 1, 15, 8, 15);

    public PlateBlock(Settings settings) {
        super(settings, false, BASE_SHAPE, false, null);
        this.setDefaultState(getDefaultState().with(IS_COVERED, false));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity entity = world.getBlockEntity(pos);
        ItemStack stack = player.getStackInHand(hand);

        if (entity instanceof PlateBlockEntity plateBlockEntity) {
            // 尝试摆盘
            ActionResult result = plateBlockEntity.tryPlating(player, hand, hit);
            if (result.isAccepted()) {
                return result;
            }

            if (plateBlockEntity.getPlatingProcess().isActive()) {
                return result;
            }

            // 玩家潜行时尝试取下盖子
            if (player.isSneaking() && state.get(IS_COVERED) &&
                    plateBlockEntity.removeCoverAndRestore()) {
                if (!player.isCreative()) {
                    player.setStackInHand(hand, new ItemStack(ModItems.PLATE_LID));
                }
                return ActionResult.SUCCESS;
            }
        }

        // 一个莫名奇妙的修复
        if (world.isClient) {
            playTakeSound(world, pos, player);
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public ItemStack createStack(int count, BlockState state, @Nullable BlockEntity blockEntity) {
        ItemStack result = super.createStack(count, state, blockEntity);

        if (state.get(IS_COVERED) && blockEntity instanceof PlateBlockEntity plateBlockEntity) {
            return ContainerUtil.replaceContent(result, plateBlockEntity.getOutcome());
        }

        return result;
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        BlockEntity entity = builder.get(LootContextParameters.BLOCK_ENTITY);

        if (state.get(IS_COVERED) && entity instanceof PlateBlockEntity plateBlockEntity) {
            List<ItemStack> droppedStacks = super.getDroppedStacks(state, builder);
            droppedStacks.forEach(stack -> ContainerUtil.replaceContent(stack, plateBlockEntity.getOutcome()));

            return droppedStacks;
        }

        return super.getDroppedStacks(state, builder);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        AbstractContent content = ContainerUtil.extractContent(itemStack);
        BlockEntity entity = world.getBlockEntity(pos);

        if (content instanceof DishesContent dishes && entity instanceof PlateBlockEntity plateBlockEntity) {
            plateBlockEntity.setOutcome(dishes);
            plateBlockEntity.coverWithLid();
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory inventory) {
                ItemScatterer.spawn(world, pos, inventory);
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        AbstractContent content = ContainerUtil.extractContent(stack);

        if (content != null) {
            tooltip.add(content.getDisplayName());
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(IS_COVERED)) {
            return VoxelShapes.union(super.getOutlineShape(state, world, pos, context), LIB_SHAPE);
        }

        return super.getOutlineShape(state, world, pos, context);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PlateBlockEntity(pos, state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(IS_COVERED);
    }
}
