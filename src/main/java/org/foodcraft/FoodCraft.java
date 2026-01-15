package org.foodcraft;

import net.fabricmc.api.ModInitializer;
import org.foodcraft.registry.*;
import org.foodcraft.integration.dfood.DFoodInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FoodCraft implements ModInitializer {
    public static final String MOD_ID = "foodcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        DFoodInit.init();
        RegistryInit.init();
        LOGGER.info("Baking Process is initializing!");
    }
}
