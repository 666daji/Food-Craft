package org.foodcraft.block;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.dfood.block.FoodBlock;
import org.foodcraft.FoodCraft;

public class ModEnforceAsItems {
    public static final FoodBlock.EnforceAsItem HARD_BREAD_BOAT = createAsItem("hard_bread_boat");

    private static FoodBlock.EnforceAsItem createAsItem(String item){
        return () -> Registries.ITEM.get(new Identifier(FoodCraft.MOD_ID, item));
    }
}
