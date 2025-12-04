package org.foodcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import org.foodcraft.client.render.model.ModModelLayers;
import org.foodcraft.client.render.gui.tooltip.FlourSackTooltipComponent;
import org.foodcraft.client.render.model.FoodCraftModelLoader;
import org.foodcraft.client.render.screen.PotteryTableScreen;
import org.foodcraft.item.FlourSackItem;
import org.foodcraft.registry.ModScreenHandlerTypes;

public class FoodCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModModelLayers.register();
        HandledScreens.register(ModScreenHandlerTypes.POTTERY_TABLE, PotteryTableScreen::new);

        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof FlourSackItem.FlourSackTooltipData flourSackData) {
                return new FlourSackTooltipComponent(flourSackData);
            }
            return null;
        });
        ModelLoadingPlugin.register(new FoodCraftModelLoader());
    }
}
