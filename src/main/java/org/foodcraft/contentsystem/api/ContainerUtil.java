package org.foodcraft.contentsystem.api;

import org.foodcraft.FoodCraft;
import org.foodcraft.contentsystem.container.ContainerType;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.registry.ContainerRegistry;
import org.foodcraft.contentsystem.registry.ContentRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 容器系统工具类。
 * <p>
 * 提供对容器系统的常用操作和便捷方法。
 * </p>
 */
public final class ContainerUtil {

    /**
     * 分析物品堆栈，返回容器-内容物绑定。
     *
     * @param stack 要分析的物品堆栈
     * @return 包含绑定对象的Optional，如果不匹配任何容器则返回空的Optional
     */
    @NotNull
    public static Optional<ContainerContentBinding> analyze(ItemStack stack) {
        return Optional.ofNullable(ContainerRegistry.analyze(stack));
    }

    /**
     * 检查物品堆栈是否提供指定的内容物。
     *
     * @param stack 要检查的物品堆栈
     * @param content 要检查的内容物类型
     * @return 如果物品堆栈装有该内容物则返回true
     */
    public static boolean providesContent(@NotNull ItemStack stack, @NotNull AbstractContent content) {
        Objects.requireNonNull(stack, "Item stack cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");

        ContainerContentBinding binding = ContainerRegistry.analyze(stack);
        return binding != null && binding.contains(content);
    }

    /**
     * 检查物品堆栈是否提供指定标识符的内容物。
     *
     * @param stack 要检查的物品堆栈
     * @param contentId 内容物类型的标识符
     * @return 如果物品堆栈装有该内容物则返回true
     */
    public static boolean providesContent(@NotNull ItemStack stack, @NotNull Identifier contentId) {
        Objects.requireNonNull(stack, "Item stack cannot be null");
        Objects.requireNonNull(contentId, "Content ID cannot be null");

        AbstractContent content = ContentRegistry.get(contentId);
        if (content == null) {
            return false;
        }

        return providesContent(stack, content);
    }

    /**
     * 检查物品堆栈是否为空容器。
     *
     * @param stack 要检查的物品堆栈
     * @return 如果物品堆栈是空容器则返回true
     */
    public static boolean isEmptyContainer(@NotNull ItemStack stack) {
        Objects.requireNonNull(stack, "Item stack cannot be null");

        ContainerContentBinding binding = ContainerRegistry.analyze(stack);
        return binding != null && binding.isEmpty();
    }

    /**
     * 检查物品堆栈是否为指定类型的容器。
     *
     * @param stack 要检查的物品堆栈
     * @param container 要检查的容器类型
     * @return 如果物品堆栈是该类型的容器则返回true
     */
    public static boolean isContainerOfType(@NotNull ItemStack stack, @NotNull ContainerType container) {
        Objects.requireNonNull(stack, "Item stack cannot be null");
        Objects.requireNonNull(container, "Container cannot be null");

        return container.matches(stack);
    }

    /**
     * 检查物品堆栈是否为指定标识符的容器类型。
     *
     * @param stack 要检查的物品堆栈
     * @param containerId 容器类型的标识符
     * @return 如果物品堆栈是该类型的容器则返回true
     */
    public static boolean isContainerOfType(@NotNull ItemStack stack, @NotNull Identifier containerId) {
        Objects.requireNonNull(stack, "Item stack cannot be null");
        Objects.requireNonNull(containerId, "Container ID cannot be null");

        ContainerType container = ContainerRegistry.get(containerId);
        if (container == null) {
            return false;
        }

        return container.matches(stack);
    }

    /**
     * 检查物品堆栈是否可以装入指定的内容物。
     *
     * @param stack 要检查的物品堆栈
     * @param content 要检查的内容物类型
     * @return 如果物品堆栈是空容器且可以装入该内容物则返回true
     */
    public static boolean canContain(@NotNull ItemStack stack, @NotNull AbstractContent content) {
        Objects.requireNonNull(stack, "Item stack cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");

        ContainerContentBinding binding = ContainerRegistry.analyze(stack);
        if (binding == null || !binding.isEmpty()) {
            return false;
        }

        return binding.container().canContain(content);
    }

    /**
     * 替换容器中的内容物。
     *
     * @param stack 要替换内容物的容器物品堆栈
     * @param newContent 新的内容物
     * @return 替换后的物品堆栈，如果容器不存在或新内容物与容器不兼容则返回原堆栈的副本
     */
    @NotNull
    public static ItemStack replaceContent(@NotNull ItemStack stack, @Nullable AbstractContent newContent) {
        Objects.requireNonNull(stack, "Item stack cannot be null");

        ContainerType container = ContainerRegistry.findContainer(stack);
        if (container == null) {
            // 不是容器，返回原堆栈副本
            return stack.copy();
        }

        try {
            return container.replaceContent(stack, newContent);
        } catch (IllegalArgumentException e) {
            // 无法替换（内容物不兼容等），返回原堆栈副本
            FoodCraft.LOGGER.error("{}", e.getMessage(), e);
            return stack.copy();
        }
    }

    /**
     * 替换容器中的内容物。
     *
     * @param stack 要替换内容物的容器物品堆栈
     * @param newContentId 新内容物的标识符
     * @return 替换后的物品堆栈，如果容器不存在、内容物不存在或新内容物与容器不兼容则返回原堆栈的副本
     */
    @NotNull
    public static ItemStack replaceContent(@NotNull ItemStack stack, @Nullable Identifier newContentId) {
        Objects.requireNonNull(stack, "Item stack cannot be null");

        AbstractContent newContent = ContentRegistry.get(newContentId);
        if (newContent == null) {
            return stack.copy();
        }

        return replaceContent(stack, newContent);
    }

    /**
     * 创建装有指定内容物的物品堆栈。
     *
     * @param container 容器类型
     * @param content 内容物类型
     * @param amount 物品数量
     * @return 装有内容物的物品堆栈
     * @throws IllegalArgumentException 如果容器不能装入该内容物
     */
    @NotNull
    public static ItemStack createFilledContainer(@NotNull ContainerType container,
                                                  @NotNull AbstractContent content,
                                                  int amount) {
        Objects.requireNonNull(container, "Container cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");

        if (!container.canContain(content)) {
            throw new IllegalArgumentException(
                    "Container " + container.getId() + " cannot contain content " + content.getId()
            );
        }

        return container.createItemStack(content, amount);
    }

    /**
     * 创建装有指定内容物的物品堆栈。
     *
     * @param containerId 容器类型的标识符
     * @param contentId 内容物类型的标识符
     * @param amount 物品数量
     * @return 装有内容物的物品堆栈，如果容器或内容物不存在则返回空物品堆栈
     */
    @NotNull
    public static ItemStack createFilledContainer(@NotNull Identifier containerId,
                                                  @NotNull Identifier contentId,
                                                  int amount) {
        Objects.requireNonNull(containerId, "Container ID cannot be null");
        Objects.requireNonNull(contentId, "Content ID cannot be null");

        ContainerType container = ContainerRegistry.get(containerId);
        AbstractContent content = ContentRegistry.get(contentId);

        if (container == null || content == null) {
            return ItemStack.EMPTY;
        }

        return createFilledContainer(container, content, amount);
    }

    /**
     * 创建空容器物品堆栈。
     *
     * @param container 容器类型
     * @param amount 物品数量
     * @return 空的容器物品堆栈
     */
    @NotNull
    public static ItemStack createEmptyContainer(@NotNull ContainerType container, int amount) {
        Objects.requireNonNull(container, "Container cannot be null");
        return container.createEmptyItemStack(amount);
    }

    /**
     * 创建空容器物品堆栈。
     *
     * @param containerId 容器类型的标识符
     * @param amount 物品数量
     * @return 空的容器物品堆栈，如果容器不存在则返回空物品堆栈
     */
    @NotNull
    public static ItemStack createEmptyContainer(@NotNull Identifier containerId, int amount) {
        Objects.requireNonNull(containerId, "Container ID cannot be null");

        ContainerType container = ContainerRegistry.get(containerId);
        if (container == null) {
            return ItemStack.EMPTY;
        }

        return container.createEmptyItemStack(amount);
    }

    /**
     * 尝试从物品堆栈中提取内容物。
     *
     * @param stack 要提取内容物的物品堆栈
     * @return 提取到的内容物类型，如果不匹配任何容器或容器为空则返回null
     */
    @Nullable
    public static AbstractContent extractContent(@NotNull ItemStack stack) {
        Objects.requireNonNull(stack, "Item stack cannot be null");

        ContainerType container = ContainerRegistry.findContainer(stack);
        if (container == null) {
            return null;
        }

        return container.extractContent(stack);
    }

    /**
     * 获取物品堆栈的容器类型。
     *
     * @param stack 要检查的物品堆栈
     * @return 容器类型，如果不匹配任何容器则返回null
     */
    @Nullable
    public static ContainerType getContainerType(@NotNull ItemStack stack) {
        Objects.requireNonNull(stack, "Item stack cannot be null");
        return ContainerRegistry.findContainer(stack);
    }

    /**
     * 获取指定分类的所有内容物类型。
     *
     * @param category 分类名称
     * @return 该分类的所有内容物类型
     */
    @NotNull
    public static List<AbstractContent> getContentsByCategory(@NotNull String category) {
        Objects.requireNonNull(category, "Category cannot be null");
        return ContentRegistry.getByCategory(category);
    }
}