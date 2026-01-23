package org.foodcraft.recipe;

import com.google.gson.*;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.foodcraft.block.process.KneadingProcess;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.item.FlourItem;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;

import java.util.*;

/**
 * 面团配方
 */
public class DoughRecipe implements Recipe<KneadingProcess<?>> {
    private final Identifier id;
    private final ItemStack output;

    // 使用Map来统计每种面粉的数量
    private final Map<FlourItem.FlourType, Integer> flourRequirements;

    // 液体要求：液体类型 -> 数量
    private final Map<AbstractContent, Integer> liquidRequirements;

    // 额外物品要求：物品 -> 数量
    private final Map<Ingredient, Integer> extraRequirements;

    public DoughRecipe(Identifier id, ItemStack output,
                       Map<FlourItem.FlourType, Integer> flourRequirements,
                       Map<AbstractContent, Integer> liquidRequirements,
                       Map<Ingredient, Integer> extraRequirements) {
        this.id = id;
        this.output = output;
        this.flourRequirements = flourRequirements;
        this.liquidRequirements = liquidRequirements;
        this.extraRequirements = extraRequirements;
    }

    @Override
    public boolean matches(KneadingProcess<?> process, World world) {
        // 检查面粉
        if (!matchesFlours(process.getFlourCounts())) {
            return false;
        }

        // 检查液体
        if (!matchesLiquids(process.getLiquidCounts())) {
            return false;
        }

        // 检查额外物品
        if (!matchesExtraItems(process.getExtraItemStacks())) {
            return false;
        }

        // 检查揉面次数
        return process.getKneadingCount() >= 2;
    }

    private boolean matchesFlours(Map<FlourItem.FlourType, Integer> processFlours) {
        for (Map.Entry<FlourItem.FlourType, Integer> requirement : flourRequirements.entrySet()) {
            int processCount = processFlours.getOrDefault(requirement.getKey(), 0);
            if (processCount < requirement.getValue()) {
                return false;
            }
        }

        // 检查总面粉数量（配方要求的总数）
        int requiredTotal = flourRequirements.values().stream().mapToInt(Integer::intValue).sum();
        int processTotal = processFlours.values().stream().mapToInt(Integer::intValue).sum();

        return processTotal == requiredTotal;
    }

    private boolean matchesLiquids(Map<AbstractContent, Integer> processLiquids) {
        for (Map.Entry<AbstractContent, Integer> requirement : liquidRequirements.entrySet()) {
            int processCount = processLiquids.getOrDefault(requirement.getKey(), 0);
            if (processCount < requirement.getValue()) {
                return false;
            }
        }

        // 检查总液体数量
        int requiredTotal = liquidRequirements.values().stream().mapToInt(Integer::intValue).sum();
        int processTotal = processLiquids.values().stream().mapToInt(Integer::intValue).sum();

        return processTotal == requiredTotal;
    }

    private boolean matchesExtraItems(List<ItemStack> processExtras) {
        // 如果没有额外物品要求，那么流程中的额外物品应该为空
        if (extraRequirements.isEmpty()) {
            return processExtras.isEmpty();
        }

        // 创建可修改的额外物品列表副本
        List<ItemStack> remainingExtras = new ArrayList<>(processExtras);

        // 检查每个要求
        for (Map.Entry<Ingredient, Integer> requirement : extraRequirements.entrySet()) {
            Ingredient ingredient = requirement.getKey();
            int requiredCount = requirement.getValue();
            int foundCount = 0;

            // 统计匹配的额外物品
            Iterator<ItemStack> iterator = remainingExtras.iterator();
            while (iterator.hasNext()) {
                ItemStack stack = iterator.next();
                if (ingredient.test(stack)) {
                    foundCount++;
                    iterator.remove(); // 移除已匹配的
                    if (foundCount >= requiredCount) {
                        break;
                    }
                }
            }

            if (foundCount < requiredCount) {
                return false;
            }
        }

        // 检查是否有未匹配的额外物品（不应该有）
        return remainingExtras.isEmpty();
    }

    @Override
    public ItemStack craft(KneadingProcess<?> inventory, DynamicRegistryManager registryManager) {
        return output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return output;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.DOUGH_MAKING;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.DOUGH_MAKING;
    }

    // Getters
    public Map<FlourItem.FlourType, Integer> getFlourRequirements() {
        return Collections.unmodifiableMap(flourRequirements);
    }

    public Map<AbstractContent, Integer> getLiquidRequirements() {
        return Collections.unmodifiableMap(liquidRequirements);
    }

    public Map<Ingredient, Integer> getExtraRequirements() {
        return Collections.unmodifiableMap(extraRequirements);
    }
}