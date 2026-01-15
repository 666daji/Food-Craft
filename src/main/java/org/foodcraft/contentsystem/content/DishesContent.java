package org.foodcraft.contentsystem.content;

import net.minecraft.item.FoodComponent;
import net.minecraft.util.Identifier;
import org.foodcraft.contentsystem.foodcraft.ContentCategories;
import org.jetbrains.annotations.NotNull;

public class DishesContent extends FoodContent {
    public DishesContent(Identifier id, FoodComponent foodComponent) {
        super(id, foodComponent);
    }

    @Override
    public @NotNull String getCategory() {
        return ContentCategories.DISHES;
    }
}
