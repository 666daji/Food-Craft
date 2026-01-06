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
public final class ContainerContentBinding {
    @NotNull
    private final ContainerType container;
    @Nullable
    private final AbstractContent content;

    /**
     * 创建一个绑定对象。
     *
     * @param container 容器类型，不能为null
     * @param content 内容物类型，可以为null（表示空容器）
     */
    public ContainerContentBinding(@NotNull ContainerType container, @Nullable AbstractContent content) {
        this.container = Objects.requireNonNull(container, "Container cannot be null");
        this.content = content;
    }

    /**
     * 获取容器类型。
     */
    @NotNull
    public ContainerType getContainer() {
        return container;
    }

    /**
     * 获取内容物类型。
     *
     * @return 内容物类型，如果容器为空则返回null
     */
    @Nullable
    public AbstractContent getContent() {
        return content;
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
    public int hashCode() {
        return Objects.hash(container, content);
    }

    @Override
    public String toString() {
        return "ContainerContentBinding{" +
                "container=" + container.getId() +
                ", content=" + (content != null ? content.getId() : "null") +
                '}';
    }
}