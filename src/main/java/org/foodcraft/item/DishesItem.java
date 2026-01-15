package org.foodcraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import org.jetbrains.annotations.Nullable;

/**
 * 表示可以装菜肴的物品
 */
public class DishesItem extends BlockItem {

    public DishesItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    protected @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        return super.getPlacementState(context);
    }
}
