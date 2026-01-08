package org.foodcraft.contentsystem.content;

import net.minecraft.item.FoodComponent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class SoupContent extends AbstractContent {
    public static final String CATEGORY = "soup";

    private final FoodComponent foodComponent;

    public SoupContent(Identifier id, FoodComponent foodComponent) {
        super(id);
        this.foodComponent = foodComponent;
    }

    @Override
    public @NotNull String getCategory() {
        return CATEGORY;
    }

    /**
     * 获取内容物对应的食物组件。
     */
    public FoodComponent getFoodComponent() {
        return foodComponent;
    }
}