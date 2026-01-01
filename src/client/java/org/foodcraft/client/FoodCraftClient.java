package org.foodcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import org.foodcraft.client.register.ModFabricEvent;
import org.foodcraft.client.render.item.ModelReplacerS;
import org.foodcraft.client.render.model.ModModelLayers;
import org.foodcraft.client.render.screen.PotteryTableScreen;
import org.foodcraft.registry.ModScreenHandlerTypes;

public class FoodCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModModelLayers.register();
        HandledScreens.register(ModScreenHandlerTypes.POTTERY_TABLE, PotteryTableScreen::new);
        ModFabricEvent.registerFabricEvents();
        ModelReplacerS.registry();
    }
}
