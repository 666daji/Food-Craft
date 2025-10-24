package org.foodcraft.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import org.foodcraft.client.render.model.ModModelLayers;
import org.foodcraft.client.render.gui.tooltip.FlourSackTooltipComponent;
import org.foodcraft.client.render.model.FoodCraftModelLoader;
import org.foodcraft.item.FlourSackItem;

public class FoodCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModModelLayers.register();

        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof FlourSackItem.FlourSackTooltipData flourSackData) {
                return new FlourSackTooltipComponent(flourSackData);
            }
            return null;
        });
        ModelLoadingPlugin.register(new FoodCraftModelLoader());
    }
}
