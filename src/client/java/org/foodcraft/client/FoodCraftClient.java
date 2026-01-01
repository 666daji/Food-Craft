package org.foodcraft.client;

import net.fabricmc.api.ClientModInitializer;
import org.foodcraft.client.register.ModFabricEvent;
import org.foodcraft.client.render.RenderRegistry;

public class FoodCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        RenderRegistry.registryRender();
        ModFabricEvent.registerFabricEvents();
    }
}
