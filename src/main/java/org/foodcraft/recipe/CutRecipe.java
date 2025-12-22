package org.foodcraft.recipe;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;

import java.util.Map;

/**
 * 支持多步骤切割的切菜配方
 */
public class CutRecipe implements net.minecraft.recipe.Recipe<Inventory> {
    private final Identifier id;
    private final Ingredient input;
    private final ItemStack output;
    private final int totalCuts; // 总共需要切的次数
    private final Map<Integer, DefaultedList<ItemStack>> cutStateMap; // 第几刀对应的库存状态
    private final DefaultedList<ItemStack> defaultState; // 默认库存状态（5个槽位）

    public CutRecipe(Identifier id, Ingredient input, ItemStack output, int totalCuts,
                     Map<Integer, DefaultedList<ItemStack>> cutStateMap,
                     DefaultedList<ItemStack> defaultState) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.totalCuts = totalCuts;
        this.cutStateMap = cutStateMap;
        this.defaultState = defaultState;
    }

    @Override
    public boolean matches(Inventory inventory, World world) {
        // 只检查主槽位（索引0）
        return input.test(inventory.getStack(0));
    }

    @Override
    public ItemStack craft(Inventory inventory, DynamicRegistryManager registryManager) {
        return output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return output.copy();
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CUT;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.CUT;
    }

    public Ingredient getInput() {
        return input;
    }

    public int getTotalCuts() {
        return totalCuts;
    }

    public DefaultedList<ItemStack> getCutState(int cutIndex) {
        return cutStateMap.getOrDefault(cutIndex, defaultState);
    }

    public DefaultedList<ItemStack> getDefaultState() {
        return defaultState;
    }

    public Map<Integer, DefaultedList<ItemStack>> getCutStateMap() {
        return cutStateMap;
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> ingredients = DefaultedList.of();
        ingredients.add(input);
        return ingredients;
    }

    /**
     * 获取完成切割后的输出数量
     */
    public int getOutputCount() {
        return output.getCount();
    }
}