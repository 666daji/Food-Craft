package org.foodcraft.contentsystem.container;

import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.SoupContent;
import org.foodcraft.contentsystem.foodcraft.ModContents;
import org.foodcraft.item.BreadBoatItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 硬面包船容器类型。
 * <p>
 * 用于承载汤类内容物的硬面包船。
 * </p>
 */
public class BreadBoatContainer extends ContainerType {

    public BreadBoatContainer(Identifier id, ContainerSettings settings) {
        super(id, settings);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.getItem() instanceof BreadBoatItem;
    }

    @Override
    public boolean canContain(AbstractContent content) {
        return BreadBoatSoupType.fromContent(content) != null;
    }

    @Override
    public @Nullable AbstractContent extractContent(ItemStack stack) {
        if (stack.getNbt() != null && stack.hasNbt()
                && stack.getNbt().contains(BreadBoatItem.SOUP_KEY, NbtElement.STRING_TYPE)) {
            String soupKey = stack.getNbt().getString(BreadBoatItem.SOUP_KEY);
            return BreadBoatSoupType.StringToSoup(soupKey);
        }

        return null;
    }

    @Override
    public @NotNull ItemStack createItemStack(AbstractContent content, int amount) {
        ItemStack result = new ItemStack(getEmptyItem(), amount);

        return BreadBoatItem.serveSoup(result, BreadBoatSoupType.fromContent(content));
    }

    /**
     * 允许装入硬面包船的汤类型枚举。
     */
    public enum BreadBoatSoupType implements StringIdentifiable {
        BEETROOT_SOUP(ModContents.BEETROOT_SOUP),
        MUSHROOM_STEW(ModContents.MUSHROOM_STEW);

        private final SoupContent content;

        BreadBoatSoupType(SoupContent content) {
            this.content = content;
        }

        @Override
        public String asString() {
            return this.content.getId().getPath();
        }

        public FoodComponent getFoodComponent() {
            return this.content.getFoodComponent();
        }

        /**
         * 从内容物获取对应的汤类型。
         *
         * @param content 内容物
         * @return 对应的汤类型，如果没有则返回null
         */
        @Nullable
        public static BreadBoatContainer.BreadBoatSoupType fromContent(AbstractContent content) {
            for (BreadBoatSoupType type : values()) {
                if (type.content.equals(content)) {
                    return type;
                }
            }
            return null;
        }

        /**
         * 从字符串获取对应的汤类型。
         *
         * @return 对应的汤
         */
        @Nullable
        public static SoupContent StringToSoup(String name) {
            BreadBoatSoupType type = fromSting(name);
            return type == null ? null : type.content;
        }

        /**
         * 从字符串获取对应的可盛入面包船的汤类型。
         *
         * @return 对应的可盛入面包船的汤类型
         */
        public static BreadBoatSoupType fromSting(String name) {
            for (BreadBoatSoupType type : values()) {
                if (type.asString().equals(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}