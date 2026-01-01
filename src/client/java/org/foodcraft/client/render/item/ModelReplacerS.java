package org.foodcraft.client.render.item;

import org.foodcraft.registry.ModItems;

public class ModelReplacerS {
    public static void registry() {
        ReplaceItemModel.registry(ModItems.FLOUR_SACK, FlourSackModelReplacer::ReplaceModel);
        ReplaceItemModel.registry(ModItems.HARD_BREAD_BOAT, EdibleContainerModelReplacer::ReplaceModel);
    }
}
