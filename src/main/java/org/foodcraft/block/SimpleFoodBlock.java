package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import org.dfood.block.FoodBlock;
import org.dfood.util.IntPropertyManager;

/**
 * 表示堆叠数为1的{@link FoodBlock}，
 * 不需要在{@link IntPropertyManager}中缓存属性即可创建实例
 */
public class SimpleFoodBlock extends FoodBlock {
    public SimpleFoodBlock(Settings settings) {
        super(settings, 1);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(IntPropertyManager.create("number_of_food", 1));
    }
}
