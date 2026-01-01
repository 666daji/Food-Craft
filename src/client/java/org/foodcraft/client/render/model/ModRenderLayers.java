package org.foodcraft.client.render.model;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import org.foodcraft.integration.dfood.AssistedBlocks;
import org.foodcraft.registry.ModBlocks;

public class ModRenderLayers {
    private static final BlockRenderLayerMap instance = BlockRenderLayerMap.INSTANCE;

    public static void registryRenderLayer() {
        instance.putBlock(AssistedBlocks.CRIPPLED_SUSPICIOUS_STEW, RenderLayer.getCutout());
        instance.putBlock(ModBlocks.COMBUSTION_FIREWOOD, RenderLayer.getCutout());
        instance.putBlock(ModBlocks.SALT_SHAKER, RenderLayer.getCutout());
        instance.putBlock(ModBlocks.MILK_POTION, RenderLayer.getCutout());
    }
}
