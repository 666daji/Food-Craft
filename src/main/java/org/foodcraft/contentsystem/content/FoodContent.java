package org.foodcraft.contentsystem.content;

import net.minecraft.item.FoodComponent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * 表示可以食用的内容物
 */
public abstract class FoodContent extends AbstractContent{
    private final FoodComponent foodComponent;

    /**
     * 创建一个内容物类型实例。
     *
     * @param id 内容物类型的唯一标识符
     * @param foodComponent 内容物的食物属性
     * @throws NullPointerException 如果id为null
     */
    protected FoodContent(Identifier id, FoodComponent foodComponent) {
        super(id);
        this.foodComponent = foodComponent;
    }

    /**
     * 获取内容物对应的食物组件。
     */
    public FoodComponent getFoodComponent() {
        return foodComponent;
    }

    /**
     * 用于创建一个简单的可食用内容
     * @param id 内容标识符
     * @param category 内容分组
     * @param foodComponent 内容食物属性
     * @return 创建的内容
     */
    public static FoodContent createFoodContent(Identifier id, String category, FoodComponent foodComponent) {
        return new FoodContent(id, foodComponent) {
            @Override
            public @NotNull String getCategory() {
                return category;
            }
        };
    }
}
