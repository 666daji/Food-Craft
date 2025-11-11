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

import java.util.HashSet;
import java.util.Set;

public class StoveRecipe extends SimpleCraftRecipe {
    public static final Set<StoveRecipe> needOtherModelRecipes = new HashSet<>();

    protected final int bakingTime;
    protected final int MaxInputCount;
    /** 基础模具为null时表示该配方无需模具 */
    @Nullable
    protected final ItemStack mold;

    public StoveRecipe(Identifier id, Ingredient input, ItemStack output, StoveRecipeSerializer.StoveExtraData data) {
        super(id, input, output);
        this.MaxInputCount = data.inputCount();
        this.bakingTime = data.stoveTime();
        this.mold = !data.mold().isEmpty() ? data.mold() : null;

        if (MaxInputCount > 1){
            needOtherModelRecipes.add(this);
        }
    }

    public int getMaxInputCount() {
        return MaxInputCount;
    }

    /**
     * 获取烘培该配方需要的总时间，这与输入的数量有关。
     * <p>输入的数量如果超过了此配方的{@linkplain StoveRecipe#MaxInputCount}</p>，则会按照配方的最大数量处理
     * @param count 烘烤的数量
     * @return 烘烤需要的总时间
     */
    public int getBakingTimeForInput(int count) {
        if (count <= 0){
            return bakingTime;
        }
        return bakingTime * Math.min(MaxInputCount, count);
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