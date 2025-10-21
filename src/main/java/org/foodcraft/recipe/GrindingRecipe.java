package org.foodcraft.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.inventory.Inventory;
import net.minecraft.world.World;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;

public class GrindingRecipe extends SimpleCraftRecipe {
    protected final int inputCount;
    protected final int grindingTime;

    public GrindingRecipe(Identifier id, Ingredient input, int inputCount, ItemStack output, int grindingTime) {
        super(id, input, output);
        this.inputCount = inputCount;
        this.grindingTime = grindingTime;
    }

    @Override
    public boolean matches(Inventory inventory, World world) {
        ItemStack stack = inventory.getStack(0);
        return this.input.test(stack) && stack.getCount() >= this.inputCount;
    }

    public int getGrindingTime() {
        return this.grindingTime;
    }

    public int getInputCount() {
        return this.inputCount;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.GRINDING;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.GRINDING;
    }
}