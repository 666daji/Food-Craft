package org.foodcraft.client.render.model;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;

public class FoodCraftModelLoader implements ModelLoadingPlugin {
    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        pluginContext.addModels(
                createModelIdentifier("wheat_flour_sack"),
                createModelIdentifier("lapis_lazuli_flour_sack"),
                createModelIdentifier("cocoa_flour_sack"),
                createModelIdentifier("amethyst_flour_sack")
        );
    }

    public static ModelIdentifier createModelIdentifier(String path){
        return new ModelIdentifier(new Identifier(FoodCraft.MOD_ID, path), "inventory");
    }
}