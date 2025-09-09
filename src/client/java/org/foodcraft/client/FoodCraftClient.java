package org.foodcraft.client;

import net.fabricmc.api.ClientModInitializer;
import org.foodcraft.client.entity.ModModelLayers;

public class FoodCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModModelLayers.register();
    }
}
