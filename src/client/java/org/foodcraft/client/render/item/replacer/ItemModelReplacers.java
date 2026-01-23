package org.foodcraft.client.render.item.replacer;

import org.foodcraft.registry.ModItems;

public class ItemModelReplacers {
    public static void registry() {
        ReplaceItemModel.registry(ModItems.FLOUR_SACK, FlourSackModelReplacer::ReplaceModel);
        ReplaceItemModel.registry(ModItems.HARD_BREAD_BOAT, BreadBoatModelReplacer::ReplaceModel);
        ReplaceItemModel.registry(ModItems.IRON_PLATE, PlateModelReplacer::ReplaceModel);
    }
}
