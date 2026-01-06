package org.foodcraft.fluidsystem.api;

import org.foodcraft.fluidsystem.container.ContainerType;
import org.foodcraft.fluidsystem.content.AbstractContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 容器-内容物绑定对象。
 * <p>
 * 表示一个物品堆栈被识别为特定的容器类型和内容物类型的组合。
 * 这是一个临时的、不可变的对象，仅用于查询结果。
 * </p>
 */
public record ContainerContentBinding(@NotNull ContainerType container, @Nullable AbstractContent content) {
    /**
     * 创建一个绑定对象。
     *
     * @param container 容器类型，不能为null
     * @param content   内容物类型，可以为null（表示空容器）
     */
    public ContainerContentBinding(@NotNull ContainerType container, @Nullable AbstractContent content) {
        this.container = Objects.requireNonNull(container, "Container cannot be null");
        this.content = content;
    }

    /**
     * 检查容器是否为空。
     */
    public boolean isEmpty() {
        return content == null;
    }

    /**
     * 检查容器是否装有指定类型的内容物。
     *
     * @param content 要检查的内容物类型
     * @return 如果容器装有该内容物则返回true
     */
    public boolean contains(@NotNull AbstractContent content) {
        return !isEmpty() && this.content.equals(content);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ContainerContentBinding that = (ContainerContentBinding) obj;
        return container.equals(that.container) && Objects.equals(content, that.content);
    }

    @Override
    public @NotNull String toString() {
        return "ContainerContentBinding{" +
                "container=" + container.getId() +
                ", content=" + (content != null ? content.getId() : "null") +
                '}';
    }
}