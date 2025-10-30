package org.foodcraft.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MoldRecipe extends SimpleCraftRecipe {
    public static final Logger LOGGER = FoodCraft.LOGGER;
    /** 可以放置在模具中的物品 */
    public static final Map<Item, Set<Item>> CAN_PLACE_MOLD_ITEMS = new HashMap<>();

    protected final Item baseMoldItem;

    public MoldRecipe(Identifier id, Ingredient input, ItemStack output, Item baseMoldItem) {
        super(id, input, output);
        this.baseMoldItem = baseMoldItem;
        if (CAN_PLACE_MOLD_ITEMS.containsKey(baseMoldItem)){
            CAN_PLACE_MOLD_ITEMS.get(baseMoldItem).add(output.getItem());
        } else {
            Set<Item> items = new HashSet<>();
            items.add(output.getItem());
            CAN_PLACE_MOLD_ITEMS.put(baseMoldItem, items);
        }
    }

    /**
     * 检查物品是否是可以放回的产物
     * @param moldItem 对应的基础模具物品
     * @param stack 要检查的物品堆栈
     * @return 是否可以放回
     */
    public static boolean isCanPlace(Item moldItem, ItemStack stack) {
        if (CAN_PLACE_MOLD_ITEMS.containsKey(moldItem)){
            return CAN_PLACE_MOLD_ITEMS.get(moldItem).contains(stack.getItem());
        }
        return false;
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
