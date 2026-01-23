package org.foodcraft.client.register;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import org.foodcraft.client.render.gui.tooltip.FlourSackTooltipComponent;
import org.foodcraft.client.render.model.ModModelLoader;
import org.foodcraft.item.FlourSackItem;

public class ModFabricEvent {
    public static void registerFabricEvents() {
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof FlourSackItem.FlourSackTooltipData flourSackData) {
                return new FlourSackTooltipComponent(flourSackData);
            }
            return null;
        });
        ModelLoadingPlugin.register(new ModModelLoader());
    }
}
