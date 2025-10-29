package org.foodcraft.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;

public class CutRecipe extends SimpleCraftRecipe{
    protected final int outputCount;

    public CutRecipe(Identifier id, Ingredient input, ItemStack output, int outputCount) {
        super(id, input, output);
        this.outputCount = outputCount;
    }

    public int getOutputCount() {
        return outputCount;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CUT;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.CUT;
    }
}
