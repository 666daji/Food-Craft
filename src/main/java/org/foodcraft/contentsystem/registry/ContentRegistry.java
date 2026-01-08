package org.foodcraft.contentsystem.registry;

import org.foodcraft.contentsystem.content.AbstractContent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 内容物类型注册表。
 */
public final class ContentRegistry {
    private static final Map<Identifier, AbstractContent> REGISTRY = new HashMap<>();
    private static final Map<String, List<AbstractContent>> CONTENT_BY_CATEGORY = new HashMap<>();

    /**
     * 注册一个内容物类型。
     */
    public static void register(@NotNull AbstractContent content) {
        Objects.requireNonNull(content, "Content cannot be null");

        Identifier id = content.getId();
        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("Content type already registered: " + id);
        }

        REGISTRY.put(id, content);

        // 按分类存储
        String category = content.getCategory();
        CONTENT_BY_CATEGORY.computeIfAbsent(category, k -> new ArrayList<>())
                .add(content);
    }

    /**
     * 获取指定标识符的内容物类型。
     */
    @Nullable
    public static AbstractContent get(Identifier id) {
        return REGISTRY.get(id);
    }

    /**
     * 获取所有已注册的内容物类型。
     */
    @NotNull
    public static Collection<AbstractContent> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * 获取指定分类的所有内容物类型。
     */
    @NotNull
    public static List<AbstractContent> getByCategory(String category) {
        List<AbstractContent> contents = CONTENT_BY_CATEGORY.get(category);
        if (contents == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(contents);
    }

    /**
     * 获取所有内容物分类。
     */
    @NotNull
    public static Set<String> getCategories() {
        return Collections.unmodifiableSet(CONTENT_BY_CATEGORY.keySet());
    }

    /**
     * 获取内容物类型的数量。
     */
    public static int size() {
        return REGISTRY.size();
    }

    /**
     * 清空注册表（仅用于测试）。
     */
    public static void clear() {
        REGISTRY.clear();
        CONTENT_BY_CATEGORY.clear();
    }
}