package org.foodcraft.util.enums;

import net.minecraft.item.*;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.Nullable;

public enum SoupType implements StringIdentifiable {
    BEETROOT_SOUP("beetroot_soup",
            FoodComponents.BEETROOT_SOUP,
            Items.BEETROOT_SOUP),
    MUSHROOM_STEW("mushroom_stew",
            FoodComponents.MUSHROOM_STEW,
            Items.MUSHROOM_STEW);

    private final String name;
    private final FoodComponent foodComponent;
    /** 表示装有该类型的汤的基本物品，一般为碗装。 */
    private final Item sourceItem;

    SoupType(String name, FoodComponent foodComponent, Item sourceItem) {
        this.name = name;
        this.foodComponent = foodComponent;
        this.sourceItem = sourceItem;
    }

    @Override
    public String asString() {
        return this.name;
    }

    public FoodComponent getFoodComponent() {
        return foodComponent;
    }

    public Item getSourceItem() {
        return sourceItem;
    }

    /**
     * 从物品堆栈中获取汤的类型。
     * @param stack 装有汤的容器
     * @return 对应的汤类型，如果没有汤或不是合法容器则返回null
     */
    @Nullable
    public static SoupType fromStack(ItemStack stack) {
        for (SoupType soupType : SoupType.values()) {
            if (stack.isOf(soupType.getSourceItem())) {
                return soupType;
            }
        }

        return null;
    }

    @Nullable
    public static SoupType fromString(String name) {
        for (SoupType soupType : SoupType.values()) {
            if (soupType.name.equals(name)) {
                return soupType;
            }
        }
        return null;
    }

    public boolean hasFoodComponent() {
        return this.foodComponent != null;
    }
}
