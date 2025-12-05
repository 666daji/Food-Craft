package org.foodcraft.recipe;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;

public class PotteryRecipe extends SimpleCraftRecipe{
    protected final int inputCount;
    protected final int craftTime;

    public PotteryRecipe(Identifier id, Ingredient input, ItemStack output, int inputCount, int craftTime) {
        super(id, input, output);
        this.inputCount = inputCount;
        this.craftTime = craftTime;
    }

    @Override
    public boolean matches(Inventory inventory, World world) {
        ItemStack stack = inventory.getStack(0);
        return this.input.test(stack) && stack.getCount() >= this.inputCount;
    }

    public int getCraftTime() {
        return craftTime;
    }

    public int getInputCount() {
        return this.inputCount;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.POTTERY;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.POTTERY;
    }
}
