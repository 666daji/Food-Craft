package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.dfood.block.SimpleFoodBlock;
import org.foodcraft.block.entity.PlateBlockEntity;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 表示一个可以摆盘的盘子方块
 */
public class PlateBlock extends SimpleFoodBlock implements BlockEntityProvider {
    /**
     * 表示当前的方块是否拥有一个已经制作完毕的菜肴。
     * <p>请不要直接更改该属性。</p>
     */
    public static final BooleanProperty IS_COMPLETION = BooleanProperty.of("is_completion");

    public PlateBlock(Settings settings) {
        super(settings, false, null, false);
        this.setDefaultState(getDefaultState().with(IS_COMPLETION, false));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof PlateBlockEntity plateBlockEntity) {
            return plateBlockEntity.tryPlating(player, hand, hit);
        }

        return ActionResult.PASS;
    }

    @Override
    public ItemStack createStack(int count, BlockState state, @Nullable BlockEntity blockEntity) {
        ItemStack result = super.createStack(count, state, blockEntity);

        if (state.get(IS_COMPLETION) && blockEntity instanceof PlateBlockEntity plateBlockEntity) {
            return ContainerUtil.replaceContent(result, plateBlockEntity.getOutcome());
        }

        return result;
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        BlockEntity entity = builder.get(LootContextParameters.BLOCK_ENTITY);

        if (state.get(IS_COMPLETION) && entity instanceof PlateBlockEntity plateBlockEntity) {
            List<ItemStack> droppedStacks = super.getDroppedStacks(state, builder);
            droppedStacks.forEach(stack -> ContainerUtil.replaceContent(stack, plateBlockEntity.getOutcome()));
        }

        return super.getDroppedStacks(state, builder);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PlateBlockEntity(pos, state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(IS_COMPLETION);
    }
}
