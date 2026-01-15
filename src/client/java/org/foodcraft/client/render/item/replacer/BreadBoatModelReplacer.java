package org.foodcraft.client.render.item.replacer;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.item.ItemStack;
import org.foodcraft.block.EmptyBreadBoatBlock;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.container.BreadBoatContainer;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.item.BreadBoatItem;

public class BreadBoatModelReplacer {

    public static BakedModel ReplaceModel(ReplaceItemModel.ReplaceContext context) {
        ItemStack stack = context.stack();

        if (stack.getItem() instanceof BreadBoatItem containerItem) {
            BlockState blockState = containerItem.getBlock().getDefaultState();
            AbstractContent content = ContainerUtil.extractContent(stack);
            BreadBoatContainer.BreadBoatSoupType soupType = BreadBoatContainer.BreadBoatSoupType.fromContent(content);
            if (soupType != null) {
                blockState = EmptyBreadBoatBlock.asTargetState(blockState, soupType);
            }
            BlockModels blockModels = context.modelManager().getBlockModels();

            return blockModels.getModel(blockState);
        }

        return context.originalModel();
    }
}
