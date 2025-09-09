package org.foodcraft.recipe;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;

public class ModRecipeSerializers {
    public static final RecipeSerializer<GrindingRecipe> GRINDING = register("grinding", new GrindingRecipeSerializer<>(GrindingRecipe::new));

    static <S extends RecipeSerializer<T>, T extends Recipe<?>> S register(String id, S serializer) {
        return Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(FoodCraft.MOD_ID, id), serializer);
    }

    public static void initialize() {}
}
