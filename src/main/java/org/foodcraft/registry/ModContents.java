package org.foodcraft.registry;

import net.minecraft.item.FoodComponents;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.contentsystem.content.*;
import org.foodcraft.food.ModFoodComponents;

public class ModContents {
    // 汤
    public static final FoodContent MUSHROOM_STEW = FoodContent.createFoodContent(
            createModId("mushroom_stew"), ContentCategories.SOUP, FoodComponents.MUSHROOM_STEW);

    public static final FoodContent BEETROOT_SOUP = FoodContent.createFoodContent(
            createModId("beetroot_soup"), ContentCategories.SOUP, FoodComponents.BEETROOT_SOUP);

    public static final FoodContent RABBIT_STEW = FoodContent.createFoodContent(
            createModId("rabbit_stew"), ContentCategories.SOUP, FoodComponents.RABBIT_STEW);

    // 基础液体
    public static final AbstractContent WATER = new BaseLiquidContent(
            createModId("water"), 4159204);

    public static final AbstractContent MILK = new BaseLiquidContent(
            createModId("milk"), 0xFFF8E1);

    // 菜肴
    public static final DishesContent BEEF_BERRIES = new DishesContent(
            createModId("beef_berries"), ModFoodComponents.BEEF_BERRIES);
    public static final DishesContent COOKED_BEEF_BERRIES = new DishesContent(
            createModId("cooked_beef_berries"), ModFoodComponents.COOKED_BEEF_BERRIES);

    // ================ 辅助方法 ================

    private static Identifier createModId(String path) {
        return new Identifier(FoodCraft.MOD_ID, path);
    }

    public static void registryContents() {}
}