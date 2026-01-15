package org.foodcraft.contentsystem.container;

import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.FoodContent;
import org.foodcraft.contentsystem.foodcraft.ContentCategories;
import org.foodcraft.contentsystem.foodcraft.ModContents;
import org.foodcraft.contentsystem.registry.ContentRegistry;
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
    public static final String SOUP_KEY = "soup_type";

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
                && stack.getNbt().contains(SOUP_KEY, NbtElement.STRING_TYPE)) {
            String soupKey = stack.getNbt().getString(SOUP_KEY);
            return ContentRegistry.get(Identifier.tryParse(soupKey));
        }

        return null;
    }

    @Override
    public @NotNull ItemStack replaceContent(@NotNull ItemStack stack, @Nullable AbstractContent content) {
        // 检查堆栈是否是有效的容器
        if (!matches(stack)) {
            invalidContainer(stack);
        }

        // 清空容器
        if (content == null) {
            if (stack.hasNbt()) {
                stack.getOrCreateNbt().remove(SOUP_KEY);
            }

            return stack;
        }

        // 检查是否是有效的内容物
        if (!canContain(content)) {
            invalidContent(content);
        }

        // 替换内容物
        if (canContain(content)) {
            stack.getOrCreateNbt().putString(SOUP_KEY, content.getId().toString());
        }

        return stack;
    }

    /**
     * 允许装入硬面包船的汤类型枚举。
     */
    public enum BreadBoatSoupType implements StringIdentifiable {
        BEETROOT_SOUP(ModContents.BEETROOT_SOUP),
        MUSHROOM_STEW(ModContents.MUSHROOM_STEW);

        private final FoodContent content;

        BreadBoatSoupType(FoodContent content) {
            if (content.isIn(ContentCategories.SOUP)) {
                this.content = content;
            } else {
                throw new IllegalArgumentException();
            }
        }

        /**
         * 获取对应的内容物。
         * @return 对应的内容物
         */
        public FoodContent getContent() {
            return content;
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
        public static BreadBoatContainer.BreadBoatSoupType fromContent(@Nullable AbstractContent content) {
            if (content == null) {
                return null;
            }

            for (BreadBoatSoupType type : values()) {
                if (type.content.equals(content)) {
                    return type;
                }
            }
            return null;
        }
    }
}