package org.foodcraft.component;

import net.minecraft.item.FoodComponent;

public class ModFoodComponents {
    public static final FoodComponent HARD_BREAD = createSimpleFoodComponent(2, 0.6f);
    public static final FoodComponent SMALL_BREAD = createSimpleFoodComponent(1, 0.8f);
    public static final FoodComponent BAGUETTE = createSimpleFoodComponent(2, 0.9f);
    public static final FoodComponent BEETROOT_SOUP_HARD_BREAD_BOAT = createSimpleFoodComponent(8, 0.9f);
    public static final FoodComponent MUSHROOM_STEW_HARD_BREAD_BOAT = createSimpleFoodComponent(8, 0.9f);
    public static final FoodComponent MILK = createSimpleFoodComponent(1, 0.4f);
    public static final FoodComponent TOAST = createSimpleFoodComponent(5, 0.6f);

    private static FoodComponent createSimpleFoodComponent(int hunger, float saturationModifier){
        return new FoodComponent.Builder()
                .hunger(hunger)
                .saturationModifier(saturationModifier)
                .build();
    }
}
