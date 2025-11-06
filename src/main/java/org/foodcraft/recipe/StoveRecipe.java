package org.foodcraft.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.foodcraft.recipe.serializer.StoveRecipeSerializer;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;
import org.jetbrains.annotations.Nullable;

public class StoveRecipe extends SimpleCraftRecipe {
    protected final int bakingTime;
    protected final int inputCount;
    /** 基础模具为null时表示该配方无需模具 */
    @Nullable
    protected final ItemStack mold;

    public StoveRecipe(Identifier id, Ingredient input, ItemStack output, StoveRecipeSerializer.StoveExtraData data) {
        super(id, input, output);
        this.inputCount = data.inputCount();
        this.bakingTime = data.stoveTime();
        this.mold = !data.mold().isEmpty() ? data.mold() : null;
    }

    public int getInputCount() {
        return inputCount;
    }

    public int getBakingTime() {
        return bakingTime;
    }

    @Nullable
    public ItemStack getMold() {
        return mold;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.STOVE;
    }

    /**
     * 判断该配方是否需要模具
     * @return 需要模具返回true，否则返回false
     */
    public boolean isNeedMold() {
        return mold != null;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.STOVE;
    }
}