package org.foodcraft.recipe;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;

public class ModRecipeTypes {
    public static final RecipeType<GrindingRecipe> GRINDING = register("grinding");

    static <T extends Recipe<?>> RecipeType<T> register(String id) {
        return Registry.register(Registries.RECIPE_TYPE, new Identifier(FoodCraft.MOD_ID, id), new RecipeType<T>() {
            public String toString() {
                return id;
            }
        });
    }

    public static void initialize() {
       ModRecipeSerializers.initialize();
    }
}
