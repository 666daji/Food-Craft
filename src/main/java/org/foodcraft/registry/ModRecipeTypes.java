package org.foodcraft.registry;

import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.recipe.*;

public class ModRecipeTypes {
    public static final RecipeType<GrindingRecipe> GRINDING = register("grinding");
    public static final RecipeType<StoveRecipe> STOVE = register("stove");
    public static final RecipeType<MoldRecipe> MOLD = register("mold");
    public static final RecipeType<CutRecipe> CUT = register("cut");
    public static final RecipeType<PotteryRecipe> POTTERY = register("pottery");
    public static final RecipeType<DoughRecipe> DOUGH_MAKING = register("dough_making");
    public static final RecipeType<PlatingRecipe> PLATING = register("plating");

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
