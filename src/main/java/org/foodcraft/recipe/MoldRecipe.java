package org.foodcraft.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;

public class MoldRecipe extends SimpleCraftRecipe {
    protected final Item baseMoldItem;

    public MoldRecipe(Identifier id, Ingredient input, ItemStack output, Item baseMoldItem) {
        super(id, input, output);
        this.baseMoldItem = baseMoldItem;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.MOLD;
    }

    public Item getBaseMoldItem() {
        return baseMoldItem;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MOLD;
    }
}
