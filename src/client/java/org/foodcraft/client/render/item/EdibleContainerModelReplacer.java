package org.foodcraft.client.render.item;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.item.ItemStack;
import org.foodcraft.block.EmptyEdibleContainerBlock;
import org.foodcraft.item.EdibleContainerItem;
import org.foodcraft.util.enums.SoupType;

public class EdibleContainerModelReplacer{

    public static BakedModel ReplaceModel(ReplaceItemModel.ReplaceContext context) {
        ItemStack stack = context.stack();

        if (stack.getItem() instanceof EdibleContainerItem containerItem) {
            BlockState blockState = containerItem.getBlock().getDefaultState();
            SoupType soupType = EdibleContainerItem.getSoupFromStack(stack);
            if (soupType != null) {
                blockState = EmptyEdibleContainerBlock.asTargetState(blockState, soupType);
            }
            BlockModels blockModels = context.modelManager().getBlockModels();

            return blockModels.getModel(blockState);
        }

        return context.originalModel();
    }
}
