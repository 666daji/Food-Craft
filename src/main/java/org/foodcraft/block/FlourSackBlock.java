package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.dfood.block.ComplexFoodBlock;
import org.dfood.block.FoodBlockBuilder;
import org.foodcraft.block.entity.FlourSackBlockEntity;
import org.foodcraft.item.FlourSackItem;
import org.jetbrains.annotations.Nullable;

public class FlourSackBlock extends ComplexFoodBlock implements BlockEntityProvider {
    public static final IntProperty SHELF_INDEX = IntProperty.of("shelf_index", 0, 1);

    protected FlourSackBlock(Settings settings, int maxFood) {
        super(settings, maxFood, true, null, false, null);

        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(NUMBER_OF_FOOD, 1)
                .with(SHELF_INDEX, 0));
    }

    public static class Builder extends FoodBlockBuilder<FlourSackBlock, Builder> {
        private Builder() {}

        public static Builder create() {
            return new Builder();
        }

        @Override
        protected FlourSackBlock createBlock() {
            return new FlourSackBlock(this.settings, this.maxFood);
        }
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FlourSackBlockEntity(pos, state);
    }

    @Override
    public boolean isSame(ItemStack stack, BlockState state, @Nullable BlockEntity blockEntity) {
        NbtCompound nbt = stack.getNbt();

        if (nbt == null || !nbt.contains(FlourSackItem.STORED_ITEM_KEY)) {
            return false;
        }

        return super.isSame(stack, state, blockEntity);
    }

    /**
     * 获取指定位置的粉尘袋物品堆栈
     */
    public ItemStack getSackStack(World world, BlockPos pos, int index) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof FlourSackBlockEntity flourSackEntity) {
            return flourSackEntity.getSackStack(index);
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(SHELF_INDEX);
    }
}