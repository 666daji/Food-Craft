package org.foodcraft.client.render.model;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.client.render.blockentity.GrindingStoneBlockEntityRenderer;
import org.foodcraft.client.render.blockentity.PotteryTableBlockEntityRenderer;

public class ModModelLayers {
    public static final EntityModelLayer GRINDING_STONE = registerMain("grinding_stone", GrindingStoneBlockEntityRenderer::getTexturedModelData);
    public static final EntityModelLayer POTTERY_TABLE = registerMain("pottery_table", PotteryTableBlockEntityRenderer::getTexturedModelData);

    private static EntityModelLayer registerMain(String id, EntityModelLayerRegistry.TexturedModelDataProvider provider) {
        return register(id, "main", provider);
    }

    private static EntityModelLayer register(String id, String layer, EntityModelLayerRegistry.TexturedModelDataProvider provider) {
        EntityModelLayer entityModelLayer = create(id, layer);
        EntityModelLayerRegistry.registerModelLayer(entityModelLayer, provider);
        return entityModelLayer;
    }

    private static EntityModelLayer create(String id, String layer) {
        return new EntityModelLayer(new Identifier(FoodCraft.MOD_ID, id), layer);
    }

    public static void register() {
    }
}
