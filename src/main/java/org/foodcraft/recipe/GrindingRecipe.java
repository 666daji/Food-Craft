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
import org.jetbrains.annotations.NotNull;

public class GrindingRecipe implements Recipe<Inventory> {
    protected final Identifier id;
    protected final Ingredient input;
    protected final int inputCount;
    protected final ItemStack output;
    protected final float experience;
    protected final int grindingTime;

    public GrindingRecipe(Identifier id, Ingredient input, int inputCount, ItemStack output, float experience, int grindingTime) {
        this.id = id;
        this.input = input;
        this.inputCount = inputCount;
        this.output = output;
        this.experience = experience;
        this.grindingTime = grindingTime;
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> defaultedList = DefaultedList.of();
        defaultedList.add(this.input);
        return defaultedList;
    }

    @Override
    public boolean matches(@NotNull Inventory inventory, World world) {
        ItemStack stack = inventory.getStack(0);
        // 修改匹配逻辑，检查物品数量和类型
        return this.input.test(stack) && stack.getCount() >= this.inputCount;
    }

    @Override
    public ItemStack craft(Inventory inventory, DynamicRegistryManager registryManager) {
        return this.output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return this.output;
    }

    public float getExperience() {
        return this.experience;
    }

    public int getGrindingTime() {
        return this.grindingTime;
    }

    public int getInputCount() {
        return this.inputCount;
    }

    @Override
    public Identifier getId() {
        return this.id;
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