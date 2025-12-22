package org.foodcraft.component;

import net.minecraft.item.FoodComponent;

public class ModFoodComponents {
    // 面食
    public static final FoodComponent HARD_BREAD = createSimpleFoodComponent(2, 0.6f);
    public static final FoodComponent SMALL_BREAD = createSimpleFoodComponent(1, 0.8f);
    public static final FoodComponent BAGUETTE = createSimpleFoodComponent(2, 0.9f);
    public static final FoodComponent BEETROOT_SOUP_HARD_BREAD_BOAT = createSimpleFoodComponent(8, 0.9f);
    public static final FoodComponent MUSHROOM_STEW_HARD_BREAD_BOAT = createSimpleFoodComponent(8, 0.9f);
    public static final FoodComponent TOAST = createSimpleFoodComponent(5, 0.6f);

    // 切片食物
    public static final FoodComponent CARROT_SLICES = createSimpleFoodComponent(2, 0.3f);
    public static final FoodComponent CARROT_HEAD = createSimpleFoodComponent(3, 0.3f);

    // 饮品
    public static final FoodComponent MILK = createSimpleFoodComponent(1, 0.4f);

    private static FoodComponent createSimpleFoodComponent(int hunger, float saturationModifier){
        return new FoodComponent.Builder()
                .hunger(hunger)
                .saturationModifier(saturationModifier)
                .build();
    }
}
