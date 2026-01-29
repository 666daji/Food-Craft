package org.foodcraft.recipe;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.api.OccupyUtil;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;

import java.util.HashSet;
import java.util.Set;

public class StoveRecipe implements Recipe<Inventory> {
    public static final Set<StoveRecipe> NEED_OTHER_MODEL_RECIPES = new HashSet<>();

    protected final Identifier id;
    protected final ItemStack input;
    protected final ItemStack output;
    protected final int bakingTime;
    protected final int MaxInputCount;

    public StoveRecipe(Identifier id, ItemStack input, ItemStack output, int MaxInputCount, int bakingTime) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.MaxInputCount = MaxInputCount;
        this.bakingTime = bakingTime;

        if (MaxInputCount > 1) {
            NEED_OTHER_MODEL_RECIPES.add(this);
        }
    }

    public int getMaxInputCount() {
        return MaxInputCount;
    }

    @Override
    public boolean matches(Inventory inventory, World world) {
        ItemStack stack = inventory.getStack(0);

        if (OccupyUtil.isOccupy(this.input)) {
            AbstractContent input = OccupyUtil.getContentFromOccupy(this.input);
            AbstractContent content = ContainerUtil.extractContent(stack);

            return input != null && input.equals(content);
        }

        return ItemStack.areItemsEqual(stack, input);
    }

    @Override
    public ItemStack craft(Inventory inventory, DynamicRegistryManager registryManager) {
        ItemStack stack = inventory.getStack(0);
        int count = Math.min(stack.getCount(), MaxInputCount);

        return OccupyUtil.isOccupy(this.output)?
            ContainerUtil.replaceContent(stack, OccupyUtil.getContentFromOccupy(this.output)):
            output.copyWithCount(count);
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return this.output.copy();
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.STOVE;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.STOVE;
    }

    /**
     * 获取烘培该配方需要的总时间，这与输入的数量有关。
     * <p>输入的数量如果超过了此配方的{@linkplain StoveRecipe#MaxInputCount}</p>，则会按照配方的最大数量处理
     * @param count 烘烤的数量
     * @return 烘烤需要的总时间
     */
    public int getBakingTimeForInput(int count) {
        if (count <= 0) {
            return bakingTime;
        }
        return bakingTime * Math.min(MaxInputCount, count);
    }

    public int getBakingTime() {
        return bakingTime;
    }

    /**
     * 获取输入物品堆栈
     */
    public ItemStack getInput() {
        return input;
    }

    /**
     * 获取输出物品堆栈
     */
    public ItemStack getOutput() {
        return output;
    }
}