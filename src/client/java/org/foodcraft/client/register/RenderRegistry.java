package org.foodcraft.client.register;

import net.minecraft.client.gui.screen.ingame.HandledScreens;
import org.foodcraft.client.render.block.ModBlockColors;
import org.foodcraft.client.render.item.renderer.ItemRenderers;
import org.foodcraft.client.render.item.replacer.ModelReplacerS;
import org.foodcraft.client.render.model.ModModelLayers;
import org.foodcraft.client.render.model.ModRenderLayers;
import org.foodcraft.client.render.screen.PotteryTableScreen;
import org.foodcraft.registry.ModScreenHandlerTypes;

public class RenderRegistry {
    public static void registryRender() {
        ModModelLayers.register();
        ModelReplacerS.registry();
        ModBlockColors.registryColors();
        ModRenderLayers.registryRenderLayer();
        ItemRenderers.registry();
        HandledScreens.register(ModScreenHandlerTypes.POTTERY_TABLE, PotteryTableScreen::new);
    }
}
