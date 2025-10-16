package org.foodcraft.component;

import net.minecraft.item.FoodComponent;

public class ModFoodComponents {
    public static final FoodComponent HARD_BREAD = new FoodComponent.Builder()
            .hunger(1)
            .saturationModifier(0.6f)
            .build();
}
