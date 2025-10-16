package org.foodcraft.recipe;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;

public class StoveRecipe implements Recipe<Inventory> {
    protected final Identifier id;
    protected final Ingredient input;
    protected final ItemStack output;
    protected final float experience;
    protected final int bakingTime;

    public StoveRecipe(Identifier id, Ingredient input, ItemStack output, float experience, int bakingTime) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.experience = experience;
        this.bakingTime = bakingTime;
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> defaultedList = DefaultedList.of();
        defaultedList.add(this.input);
        return defaultedList;
    }

    @Override
    public boolean matches(Inventory inventory, World world) {
        return this.input.test(inventory.getStack(0));
    }

    @Override
    public ItemStack craft(Inventory inventory, DynamicRegistryManager registryManager) {
        return this.output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    public Ingredient getInput() {
        return input;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return this.output;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    public float getExperience() {
        return this.experience;
    }

    public int getBakingTime() {
        return bakingTime;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.STOVE;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.STOVE;
    }
}
