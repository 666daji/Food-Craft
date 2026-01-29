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
            createModId("milk"), 0xFFFAF2ED);

    // 菜肴
    public static final DishesContent BEEF_BERRIES = new DishesContent(
            createModId("beef_berries"), ModFoodComponents.BEEF_BERRIES);

    public static final DishesContent COOKED_BEEF_BERRIES = new DishesContent(
            createModId("cooked_beef_berries"), ModFoodComponents.COOKED_BEEF_BERRIES);

    public static final DishesContent ROASTED_MUSHROOMS = new DishesContent(
            createModId("roasted_mushrooms"), ModFoodComponents.BEEF_BERRIES);

    public static final DishesContent COOKED_ROASTED_MUSHROOMS = new DishesContent(
            createModId("cooked_roasted_mushrooms"), ModFoodComponents.BEEF_BERRIES);

    public static final DishesContent HONEY_ROASTED_BEEF = new DishesContent(
            createModId("honey_roasted_beef"), ModFoodComponents.BEEF_BERRIES);

    public static final DishesContent COOKED_HONEY_ROASTED_BEEF = new DishesContent(
            createModId("cooked_honey_roasted_beef"), ModFoodComponents.BEEF_BERRIES);

    public static final DishesContent FRY_SALMON_CUBES = new DishesContent(
            createModId("fry_salmon_cubes"), ModFoodComponents.COOKED_BEEF_BERRIES);

    public static final DishesContent COOKED_FRY_SALMON_CUBES = new DishesContent(
            createModId("cooked_fry_salmon_cubes"), ModFoodComponents.COOKED_BEEF_BERRIES);

    public static final DishesContent GRILLED_FISH_POTATOES = new DishesContent(
            createModId("grilled_fish_potatoes"), ModFoodComponents.BEEF_BERRIES);

    public static final DishesContent COOKED_GRILLED_FISH_POTATOES = new DishesContent(
            createModId("cooked_grilled_fish_potatoes"), ModFoodComponents.BEEF_BERRIES);

    // 定型面团
    public static final ShapedDoughContent TOAST_EMBRYO = new ShapedDoughContent(
            createModId("toast_embryo"), ModItems.TOAST_DOUGH, ModBlocks.TOAST_EMBRYO_MOLD);

    public static final ShapedDoughContent TOAST = new ShapedDoughContent(
            createModId("toast"), ModItems.TOAST, ModBlocks.TOAST_EMBRYO_MOLD);

    public static final ShapedDoughContent CAKE_EMBRYO = new ShapedDoughContent(
            createModId("cake_embryo"), ModItems.CAKE_DOUGH, ModBlocks.CAKE_EMBRYO_MOLD);

    public static final ShapedDoughContent BAKED_CAKE_EMBRYO = new ShapedDoughContent(
            createModId("baked_cake_embryo"), ModItems.BAKED_CAKE_EMBRYO, ModBlocks.CAKE_EMBRYO_MOLD);

    // 糖浆
    public static final AbstractContent HONEY = AbstractContent.createSimpleContent(
            createModId("honey"), ContentCategories.SYRUP);

    // ================ 辅助方法 ================

    private static Identifier createModId(String path) {
        return new Identifier(FoodCraft.MOD_ID, path);
    }

    public static void registryContents() {}
}