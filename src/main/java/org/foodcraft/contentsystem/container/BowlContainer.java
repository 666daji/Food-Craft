package org.foodcraft.contentsystem.container;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.SoupContent;
import org.foodcraft.contentsystem.registry.ContentRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 碗容器类型。
 * <p>
 * 用于承载汤类内容物，如蘑菇煲、甜菜汤等。
 * 原版中碗可以装蘑菇煲和甜菜汤。
 * </p>
 */
public class BowlContainer extends ContainerType {
    // 映射：原版物品到内容物的映射
    private final Map<Item, AbstractContent> itemToContentMap;
    // 映射：内容物到原版物品的映射
    private final Map<AbstractContent, Item> contentToItemMap;

    public BowlContainer(Identifier id, ContainerSettings settings) {
        super(id, settings);

        this.itemToContentMap = new HashMap<>();
        this.contentToItemMap = new HashMap<>();

        // 初始化原版汤的硬编码映射
        initializeVanillaMappings();
    }

    /**
     * 初始化原版汤的硬编码映射。
     */
    private void initializeVanillaMappings() {
        // 蘑菇煲
        AbstractContent mushroomStew = ContentRegistry.get(new Identifier(FoodCraft.MOD_ID, "mushroom_stew"));
        if (mushroomStew != null) {
            itemToContentMap.put(Items.MUSHROOM_STEW, mushroomStew);
            contentToItemMap.put(mushroomStew, Items.MUSHROOM_STEW);
        }

        // 甜菜汤
        AbstractContent beetrootSoup = ContentRegistry.get(new Identifier(FoodCraft.MOD_ID, "beetroot_soup"));
        if (beetrootSoup != null) {
            itemToContentMap.put(Items.BEETROOT_SOUP, beetrootSoup);
            contentToItemMap.put(beetrootSoup, Items.BEETROOT_SOUP);
        }

        //兔肉煲
        AbstractContent rabbitStew = ContentRegistry.get(new Identifier(FoodCraft.MOD_ID, "rabbit_stew"));
        if (rabbitStew != null) {
            itemToContentMap.put(Items.RABBIT_STEW, rabbitStew);
            contentToItemMap.put(rabbitStew, Items.RABBIT_STEW);
        }
    }

    @Override
    public boolean matches(ItemStack stack) {
        // 检查是否是碗或已知的汤类物品
        if (stack.isOf(Items.BOWL)) {
            return true;
        }

        // 检查是否是已知的汤类物品
        for (Item item : itemToContentMap.keySet()) {
            if (stack.isOf(item)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canContain(AbstractContent content) {
        // 碗可以装汤类内容物
        return SoupContent.CATEGORY.equals(content.getCategory());
    }

    @Override
    public @Nullable AbstractContent extractContent(ItemStack stack) {
        if (stack.isEmpty() || !matches(stack)) {
            return null;
        }

        // 空碗返回null
        if (stack.isOf(Items.BOWL)) {
            return null;
        }

        // 查找对应内容物
        return itemToContentMap.get(stack.getItem());
    }

    @Override
    public @NotNull ItemStack createItemStack(AbstractContent content, int amount) {
        if (!canContain(content)) {
            FoodCraft.LOGGER.warn("Attempted to create bowl with unsupported content: {}", content.getId());
            return createEmptyItemStack(amount);
        }

        // 查找对应物品
        Item item = contentToItemMap.get(content);
        if (item != null) {
            return new ItemStack(item, amount);
        }

        // 如果内容物没有对应的物品，返回空碗
        return createEmptyItemStack(amount);
    }

    /**
     * 注册一个新的内容物到物品的映射。
     * <p>
     * 这允许扩展模组为碗容器添加新的汤类内容物。
     * </p>
     *
     * @param content 内容物类型
     * @param item 对应的物品
     * @return 当前容器实例，用于链式调用
     */
    public BowlContainer registerContentMapping(AbstractContent content, Item item) {
        if (!SoupContent.CATEGORY.equals(content.getCategory())) {
            FoodCraft.LOGGER.warn("Attempted to register non-soup content to bowl container: {}", content.getId());
            return this;
        }

        contentToItemMap.put(content, item);
        itemToContentMap.put(item, content);
        return this;
    }

    /**
     * 获取所有支持的内容物类型。
     *
     * @return 支持的内容物集合
     */
    public Set<AbstractContent> getSupportedContents() {
        return Collections.unmodifiableSet(contentToItemMap.keySet());
    }

    /**
     * 获取所有支持的物品。
     *
     * @return 支持的物品集合
     */
    public Set<Item> getSupportedItems() {
        return Collections.unmodifiableSet(itemToContentMap.keySet());
    }

    /**
     * 检查是否支持指定的物品。
     *
     * @param item 要检查的物品
     * @return 如果支持该物品则返回true
     */
    public boolean supportsItem(Item item) {
        return itemToContentMap.containsKey(item);
    }
}