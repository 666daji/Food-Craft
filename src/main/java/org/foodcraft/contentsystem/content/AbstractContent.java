package org.foodcraft.contentsystem.content;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.contentsystem.registry.ContentRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 内容物类型抽象基类。
 * <p>
 * 表示一种可以被容器承载的内容物类型，如水、汤、牛奶等。
 * 每个内容物类型有唯一的标识符和显示名称。
 * 这是一个纯类型定义，不包含具体状态。
 * </p>
 */
public abstract class AbstractContent {
    private final Identifier id;

    /**
     * 创建一个内容物类型实例。
     *
     * @param id 内容物类型的唯一标识符
     * @throws NullPointerException 如果id为null
     */
    protected AbstractContent(Identifier id) {
        this.id = Objects.requireNonNull(id, "Content type ID cannot be null");

        try {
            ContentRegistry.register(this);
        } catch (Exception e) {
            FoodCraft.LOGGER.error("Content type registration failed: {}", id);
        }
    }

    /**
     * 获取内容物类型的唯一标识符。
     */
    @NotNull
    public final Identifier getId() {
        return id;
    }

    /**
     * 获取内容物类型的显示名称。
     * <p>
     * 默认实现使用翻译键格式："content.{namespace}.{path}"
     * 子类可以重写此方法以提供不同的显示名称。
     * </p>
     */
    @NotNull
    public String getDisplayTranslationKey() {
        return "content." + id.getNamespace() + "." + id.getPath();
    }

    /**
     * 获取内容物的显示文本。
     * @return 内容物的显示文本
     */
    public Text getDisplayName() {
        return Text.translatable(getDisplayTranslationKey());
    }

    /**
     * 获取内容物类型的分类。
     * <p>
     * 用于区分不同类型的内容物（如"liquid"、"food"等）。
     * 子类必须实现此方法。
     * </p>
     */
    @NotNull
    public abstract String getCategory();

    /**
     * 检查该内容物是否属于某个分组。
     * @param category 目标分组
     * @return 属于返回true，否则返回false
     */
    public boolean isIn(String category) {
        return category.equals(getCategory());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractContent that = (AbstractContent) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getCategory() + ": {id=" + id + "}";
    }

    /**
     * 用于创建一个简单的内容。
     * @param id 内容标识符
     * @param category 内容分组
     * @return 创建的内容
     */
    public static AbstractContent createSimpleContent(Identifier id, String category) {
        return new AbstractContent(id) {
            @Override
            public @NotNull String getCategory() {
                return category;
            }
        };
    }
}