package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.dfood.block.FoodBlock;
import org.dfood.shape.FoodShapeHandle;
import org.dfood.util.IntPropertyManager;
import org.jetbrains.annotations.Nullable;

/**
 * 表示堆叠数为1的{@link FoodBlock}，
 * 不需要在{@link IntPropertyManager}中缓存属性即可创建实例
 */
public class SimpleFoodBlock extends FoodBlock {
    @Nullable
    protected VoxelShape shape;

    public SimpleFoodBlock(Settings settings) {
        super(settings, 1);
    }

    public SimpleFoodBlock(Settings settings, boolean isFood){
        super(settings, 1, isFood);
    }

    /**
     * 允许自定义简单的普通形状，
     * 省去{@link FoodShapeHandle}的复杂使用。
     * @param shape 自定义的形状
     */
    public void setShape(@Nullable VoxelShape shape) {
        this.shape = shape;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (shape != null) {
            return shape;
        }

        return super.getOutlineShape(state, world, pos, context);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(IntPropertyManager.create("number_of_food", 1));
    }
}
