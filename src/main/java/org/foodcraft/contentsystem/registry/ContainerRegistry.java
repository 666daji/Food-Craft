package org.foodcraft.contentsystem.registry;

import org.foodcraft.contentsystem.api.ContainerContentBinding;
import org.foodcraft.contentsystem.container.ContainerType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 容器类型注册表。
 * <p>
 * 管理所有已注册的容器类型，提供查找和分析物品堆栈的功能。
 * </p>
 */
public final class ContainerRegistry {
    private static final Map<Identifier, ContainerType> REGISTRY = new HashMap<>();

    /**
     * 注册一个容器类型。
     *
     * @param container 要注册的容器类型
     * @throws NullPointerException 如果container为null
     * @throws IllegalArgumentException 如果已存在相同标识符的容器类型
     */
    public static void register(@NotNull ContainerType container) {
        Objects.requireNonNull(container, "Container cannot be null");

        Identifier id = container.getId();
        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("Container type already registered: " + id);
        }

        REGISTRY.put(id, container);
    }

    /**
     * 获取指定标识符的容器类型。
     */
    @Nullable
    public static ContainerType get(Identifier id) {
        return REGISTRY.get(id);
    }

    /**
     * 获取所有已注册的容器类型。
     */
    @NotNull
    public static Collection<ContainerType> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * 分析物品堆栈，识别其容器类型和内容物。
     * <p>
     * 遍历所有已注册的容器类型，找到匹配的容器，
     * 然后尝试从该容器中提取内容物。
     * </p>
     *
     * @param stack 要分析的物品堆栈
     * @return 容器-内容物绑定对象，如果不匹配任何容器则返回null
     */
    @Nullable
    public static ContainerContentBinding analyze(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        for (ContainerType container : REGISTRY.values()) {
            if (container.matches(stack)) {
                AbstractContent content = container.extractContent(stack);
                return new ContainerContentBinding(container, content);
            }
        }

        return null;
    }

    /**
     * 检查物品堆栈是否属于某个容器类型。
     *
     * @param stack 要检查的物品堆栈
     * @return 如果物品堆栈匹配任何容器类型则返回true
     */
    public static boolean isContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        for (ContainerType container : REGISTRY.values()) {
            if (container.matches(stack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 查找匹配指定物品堆栈的容器类型。
     *
     * @param stack 要检查的物品堆栈
     * @return 匹配的容器类型，如果没有则返回null
     */
    @Nullable
    public static ContainerType findContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        for (ContainerType container : REGISTRY.values()) {
            if (container.matches(stack)) {
                return container;
            }
        }

        return null;
    }

    /**
     * 获取容器类型的数量。
     */
    public static int size() {
        return REGISTRY.size();
    }

    /**
     * 清空注册表（仅用于测试）。
     */
    public static void clear() {
        REGISTRY.clear();
    }
}