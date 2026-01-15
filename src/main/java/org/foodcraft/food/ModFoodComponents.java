package org.foodcraft.food;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;

public class ModFoodComponents {
    // 面食
    public static final FoodComponent HARD_BREAD = createSimpleFoodComponent(10, 0.6f);
    public static final FoodComponent SMALL_BREAD = createSimpleFoodComponent(3, 0.5f);
    public static final FoodComponent BAGUETTE = createSimpleFoodComponent(10, 0.6f);
    public static final FoodComponent TOAST = createSimpleFoodComponent(5, 0.6f);

    // 可食用容器
    public static final FoodComponent HARD_BREAD_BOAT = createSimpleFoodComponent(2, 0.4f);

    // 切片食物
    public static final FoodComponent CARROT_SLICES = createSimpleFoodComponent(2, 0.3f);
    public static final FoodComponent CARROT_HEAD = createSimpleFoodComponent(3, 0.3f);
    public static final FoodComponent COD_CUBES = createSimpleFoodComponent(3, 0.3f);
    public static final FoodComponent COD_HEAD = createSimpleFoodComponent(3, 0.3f);
    public static final FoodComponent COOKED_COD_CUBES = createSimpleFoodComponent(3, 0.3f);
    public static final FoodComponent COOKED_COD_HEAD = createSimpleFoodComponent(3, 0.3f);
    public static final FoodComponent SALMON_CUBES = createSimpleFoodComponent(3, 0.3f);
    public static final FoodComponent COOKED_SALMON_CUBES = createSimpleFoodComponent(3, 0.3f);

    // 饮品
    public static final FoodComponent MILK = createSimpleFoodComponent(1, 0.4f);

    // 菜肴
    public static final FoodComponent BEEF_BERRIES = new FoodComponent.Builder()
            .hunger(2).saturationModifier(0.4f).meat()
            .statusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 600, 0), 0.3F)
            .build();
    public static final FoodComponent COOKED_BEEF_BERRIES = new FoodComponent.Builder()
            .hunger(7).saturationModifier(0.7f).build();

    private static FoodComponent createSimpleFoodComponent(int hunger, float saturationModifier){
        return new FoodComponent.Builder()
                .hunger(hunger)
                .saturationModifier(saturationModifier)
                .build();
    }
}
