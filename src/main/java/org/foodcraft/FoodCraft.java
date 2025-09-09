package org.foodcraft;

import net.fabricmc.api.ModInitializer;
import org.foodcraft.block.ModBlocks;
import org.foodcraft.block.entity.ModBlockEntityTypes;
import org.foodcraft.integration.dfood.dfoodInit;
import org.foodcraft.item.ModItems;
import org.foodcraft.recipe.ModRecipeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FoodCraft implements ModInitializer {
    public static final String MOD_ID = "foodcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        dfoodInit.init();
        ModBlocks.registerModBlocks();
        ModItems.registerModItems();
        ModBlockEntityTypes.registerAllBlockEntityTypes();
        ModRecipeTypes.initialize();
        LOGGER.info("FoodCraft mod is initializing");
    }
}
