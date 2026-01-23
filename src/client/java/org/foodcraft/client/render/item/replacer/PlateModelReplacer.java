package org.foodcraft.client.render.item.replacer;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import org.dfood.util.DFoodUtils;
import org.foodcraft.block.PlateBlock;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.content.AbstractContent;

public class PlateModelReplacer {

    public static BakedModel ReplaceModel(ReplaceItemModel.ReplaceContext context) {
        AbstractContent content = ContainerUtil.extractContent(context.stack());

        if (content != null) {
            BlockState renderState = DFoodUtils.getBlockStateFromItem(context.stack().getItem())
                    .with(PlateBlock.IS_COVERED, true);

            return context.modelManager().getBlockModels().getModel(renderState);
        }

        return context.originalModel();
    }
}
