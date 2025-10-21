package org.foodcraft.registry;

import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.recipe.serializer.*;

public class ModRecipeSerializers {
    public static final RecipeSerializer<?> GRINDING = register("grinding", new GrindingRecipeSerializer());
    public static final RecipeSerializer<?> STOVE = register("stove", new StoveRecipeSerializer());
    public static final RecipeSerializer<?> MOLD = register("mold", new MoldRecipeSerializer());

    private static <S extends RecipeSerializer<?>> S register(String id, S serializer) {
        return Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(FoodCraft.MOD_ID, id), serializer);
    }

    public static void initialize() {

    }
}