package org.foodcraft.contentsystem.container;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * 抽象映射容器类型。
 * <p>
 * 用于管理物品和内容物之间的双向映射。
 * 子类只需要实现 matches 和 canContain 方法即可。
 * 自动利用父类 ContainerType 的空容器管理功能。
 * </p>
 */
public abstract class AbstractMappedContainer extends ContainerType {
    protected final BiMap<Item, AbstractContent> contentBiMap;

    public AbstractMappedContainer(Identifier id, ContainerSettings settings) {
        super(id, settings);
        this.contentBiMap = HashBiMap.create();
    }

    /**
     * 注册一个新的内容物到物品的映射。
     *
     * @param content 内容物类型
     * @param item    对应的物品
     */
    public void registerContentMapping(AbstractContent content, Item item) {
        if (!canContain(content)) {
            FoodCraft.LOGGER.warn("Attempted to register invalid content to {} container: {}",
                    getId(), content.getId());
            return;
        }

        // 确保物品与空容器物品不同
        if (item == getEmptyItem()) {
            FoodCraft.LOGGER.error("Cannot register empty container item as content mapping: {}", item);
            return;
        }

        // 使用 forcePut 确保双向唯一性
        contentBiMap.forcePut(item, content);
    }

    /**
     * 检查物品堆栈是否为空容器。
     * 基于父类提供的 emptyItem 进行判断。
     *
     * @param stack 要检查的物品堆栈
     * @return 如果是空容器则返回true
     */
    protected boolean isEmptyContainer(ItemStack stack) {
        return stack.isOf(getEmptyItem());
    }

    /**
     * 获取所有支持的内容物类型。
     *
     * @return 支持的内容物集合（不可修改）
     */
    public Set<AbstractContent> getSupportedContents() {
        return Collections.unmodifiableSet(contentBiMap.values());
    }

    /**
     * 获取所有支持的物品（不包括空容器物品）。
     *
     * @return 支持的物品集合（不可修改）
     */
    public Set<Item> getSupportedItems() {
        return Collections.unmodifiableSet(contentBiMap.keySet());
    }

    /**
     * 检查是否支持指定的物品。
     *
     * @param item 要检查的物品
     * @return 如果支持该物品则返回true
     */
    public boolean supportsItem(Item item) {
        return contentBiMap.containsKey(item);
    }

    /**
     * 检查是否支持指定的内容物。
     *
     * @param content 要检查的内容物
     * @return 如果支持该内容物则返回true
     */
    public boolean supportsContent(AbstractContent content) {
        return contentBiMap.containsValue(content);
    }

    /**
     * 通过内容物获取对应的物品。
     *
     * @param content 内容物
     * @return 对应的物品，如果没有映射则返回null
     */
    @Nullable
    public Item getItemForContent(AbstractContent content) {
        return contentBiMap.inverse().get(content);
    }

    /**
     * 通过物品获取对应的内容物。
     *
     * @param item 物品
     * @return 对应的内容物，如果没有映射则返回null
     */
    @Nullable
    public AbstractContent getContentForItem(Item item) {
        return contentBiMap.get(item);
    }

    /**
     * 移除指定物品的映射。
     *
     * @param item 要移除的物品
     * @return 被移除的内容物，如果没有映射则返回null
     */
    @Nullable
    public AbstractContent removeMappingByItem(Item item) {
        return contentBiMap.remove(item);
    }

    /**
     * 移除指定内容物的映射。
     *
     * @param content 要移除的内容物
     * @return 被移除的物品，如果没有映射则返回null
     */
    @Nullable
    public Item removeMappingByContent(AbstractContent content) {
        return contentBiMap.inverse().remove(content);
    }

    @Override
    public @Nullable AbstractContent extractContent(ItemStack stack) {
        if (stack.isEmpty() || !matches(stack)) {
            return null;
        }

        // 检查是否是空容器
        if (isEmptyContainer(stack)) {
            return null;
        }

        // 查找对应内容物
        return contentBiMap.get(stack.getItem());
    }

    @Override
    public @NotNull ItemStack replaceContent(@NotNull ItemStack stack, @Nullable AbstractContent content) {
        // 检查堆栈是否是有效的容器
        if (!matches(stack)) {
            invalidContainer(stack);
        }

        // content 为 null，清空容器
        if (content == null) {
            ItemStack result = new ItemStack(getEmptyItem(), stack.getCount());
            if (stack.hasNbt()) {
                result.setNbt(stack.getNbt().copy());
            }
            return result;
        }

        // content 不为 null，检查是否可以容纳
        if (!canContain(content)) {
            invalidContent(content);
        }

        // 查找对应物品
        Item item = contentBiMap.inverse().get(content);
        if (item == null) {
            // 内容物没有对应的物品映射，无法替换
            FoodCraft.LOGGER.warn("No item mapping found for content: {} in container {}",
                    content.getId(), getId());
            return stack.copy();
        }

        // 创建新的物品堆栈并保留NBT
        ItemStack result = new ItemStack(item, stack.getCount());
        if (stack.hasNbt()) {
            result.setNbt(stack.getNbt().copy());
        }
        return result;
    }

    @Override
    @NotNull
    public ItemStack createItemStack(AbstractContent content, int amount) {
        if (!canContain(content)) {
            throw new IllegalArgumentException("Container cannot contain content: " + content.getId());
        }

        Item item = contentBiMap.inverse().get(content);
        if (item == null) {
            throw new IllegalArgumentException("No item mapping found for content: " + content.getId());
        }

        return new ItemStack(item, amount);
    }
}