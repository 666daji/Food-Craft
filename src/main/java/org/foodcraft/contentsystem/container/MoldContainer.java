package org.foodcraft.contentsystem.container;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.ShapedDoughContent;
import org.foodcraft.contentsystem.registry.ContentRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MoldContainer extends ContainerType{
    public static final String DOUGH_KEY = "dough_type";

    public MoldContainer(Identifier id, ContainerSettings settings) {
        super(id, settings);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.isOf(getEmptyItem());
    }

    @Override
    public boolean canContain(AbstractContent content) {
        return content instanceof ShapedDoughContent shapedDough
                && shapedDough.getBaseMold().asItem().equals(getEmptyItem());
    }

    @Override
    public @Nullable AbstractContent extractContent(ItemStack stack) {
        Identifier id = Identifier.tryParse(stack.getOrCreateNbt().getString(DOUGH_KEY));
        if (id != null) {
            return ContentRegistry.get(id);
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
                stack.getOrCreateNbt().remove(DOUGH_KEY);
            }

            return stack;
        }

        // 检查是否是有效的内容物
        if (!canContain(content)) {
            invalidContent(content);
        }

        // 替换内容物
        stack.getOrCreateNbt().putString(DOUGH_KEY, content.getId().toString());
        return stack;
    }
}
