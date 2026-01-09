package org.foodcraft.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.foodcraft.block.entity.PlatableBlockEntity;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;

import java.util.List;

/**
 * 摆盘配方类，表示一个完整的摆盘配方。
 *
 * <p>摆盘配方由以下部分组成：</p>
 * <ul>
 *   <li><strong>容器</strong>：配方的承载容器（如铁盘、木盘）</li>
 *   <li><strong>步骤序列</strong>：按顺序放置的物品列表</li>
 *   <li><strong>输出</strong>：完成摆盘后得到的最终物品</li>
 * </ul>
 *
 * <p><strong>配方匹配规则：</strong></p>
 * <ol>
 *   <li>必须使用正确的容器类型</li>
 *   <li>必须按照步骤序列的顺序放置物品</li>
 *   <li>不允许跳过任何步骤</li>
 *   <li>当所有步骤完成后，使用特定的完成物品触发输出</li>
 * </ol>
 *
 * <p><strong>注意：</strong>摆盘配方不限制步骤数量，完全由配方的定义决定。</p>
 *
 * @see PlatableBlockEntity
 * @see Recipe
 */
public class PlatingRecipe implements Recipe<PlatableBlockEntity> {

    /** 配方ID，用于唯一标识此配方 */
    private final Identifier id;

    /** 容器物品类型，表示此配方所需的容器（如铁盘） */
    private final Item container;

    /** 步骤序列，按顺序放置的物品列表 */
    private final List<Item> steps;

    /** 配方输出物品，完成所有步骤后获得 */
    private final ItemStack output;

    /**
     * 创建摆盘配方。
     *
     * @param id 配方ID，用于唯一标识此配方
     * @param container 容器物品类型
     * @param steps 步骤物品列表，列表顺序即为放置顺序
     * @param output 配方输出物品
     */
    public PlatingRecipe(Identifier id, Item container, List<Item> steps, ItemStack output) {
        this.id = id;
        this.container = container;
        this.steps = List.copyOf(steps);
        this.output = output.copy();
    }

    /**
     * 检查给定的摆盘方块实体是否匹配此配方。
     *
     * <p><strong>注意：</strong>摆盘配方的匹配是实时、逐步进行的。
     * 此方法仅用于判断是否<b>完全匹配</b>，即当前摆盘状态是否与配方的所有步骤一致。</p>
     *
     * @param inventory 摆盘方块实体
     * @param world 世界，用于获取额外上下文信息（未使用）
     * @return 如果当前摆放的容器和所有步骤物品都与此配方匹配，则返回true
     */
    @Override
    public boolean matches(PlatableBlockEntity inventory, World world) {
        // 首先检查容器类型是否匹配
        if (inventory.getContainerType() != this.container) {
            return false;
        }

        // 获取当前摆放的物品类型列表
        List<Item> placedItems = inventory.getPlacedItemTypes();

        // 检查步骤数量是否匹配
        if (placedItems.size() != this.steps.size()) {
            return false;
        }

        // 检查每个步骤的物品是否匹配
        for (int i = 0; i < steps.size(); i++) {
            if (placedItems.get(i) != steps.get(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 根据当前摆盘状态制作输出物品。
     *
     * <p>此方法会消耗摆盘上的所有物品，并返回配方输出。
     * 注意：摆盘系统通常不直接调用此方法，而是通过流程完成步骤处理。</p>
     *
     * @param inventory 摆盘方块实体
     * @param registryManager 动态注册管理器，用于获取物品注册信息
     * @return 配方输出物品的副本
     */
    @Override
    public ItemStack craft(PlatableBlockEntity inventory, DynamicRegistryManager registryManager) {
        return this.output.copy();
    }

    /**
     * 检查配方的容器是否适合给定的物品栏大小。
     *
     * <p><strong>注意：</strong>摆盘配方不使用传统的物品栏格子，因此此方法意义有限。
     * 通常返回true表示配方可以"适配"任意大小的物品栏。</p>
     *
     * @param width 物品栏宽度（未使用）
     * @param height 物品栏高度（未使用）
     * @return 总是返回true，表示配方总是适配
     */
    @Override
    public boolean fits(int width, int height) {
        // 摆盘配方不使用传统的物品栏格子，总是返回true
        return true;
    }

    /**
     * 获取配方的输出物品（不包含消耗的输入物品）。
     *
     * @param registryManager 动态注册管理器，用于获取物品注册信息
     * @return 配方输出物品的副本
     */
    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return this.output.copy();
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.PLATING;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.PLATING;
    }

    /**
     * 获取配方所需的容器物品类型。
     *
     * @return 容器物品类型
     */
    public Item getContainer() {
        return container;
    }

    /**
     * 获取配方的步骤序列。
     *
     * <p>返回的是步骤序列的不可修改副本，确保外部不能修改内部状态。</p>
     *
     * @return 步骤物品列表的不可修改视图
     */
    public List<Item> getSteps() {
        return steps;
    }

    /**
     * 获取配方步骤数量。
     *
     * @return 步骤数量
     */
    public int getStepCount() {
        return steps.size();
    }

    /**
     * 获取指定索引的步骤物品。
     *
     * @param index 步骤索引（从0开始）
     * @return 该步骤所需的物品类型
     * @throws IndexOutOfBoundsException 如果索引超出范围
     */
    public Item getStepAt(int index) {
        return steps.get(index);
    }

    /**
     * 获取配方的输出物品。
     *
     * <p>返回的是输出物品的副本，确保外部不能修改内部状态。</p>
     *
     * @return 输出物品的副本
     */
    public ItemStack getOutput() {
        return output.copy();
    }

    // ==================== 配方匹配辅助方法 ====================

    /**
     * 检查给定的已放置物品列表是否与配方的所有步骤完全匹配。
     *
     * <p>此方法仅比较物品类型，不检查容器类型。用于快速检查而不需要完整的方块实体。</p>
     *
     * @param placedItems 已放置的物品类型列表
     * @return 如果每个步骤的物品类型都匹配，则返回true
     */
    public boolean matchesSteps(List<Item> placedItems) {
        // 检查步骤数量是否相同
        if (placedItems.size() != this.steps.size()) {
            return false;
        }

        // 检查每个步骤的物品类型
        for (int i = 0; i < steps.size(); i++) {
            if (placedItems.get(i) != steps.get(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查给定的已放置物品列表是否是此配方的有效前缀。
     *
     * <p>有效前缀意味着已放置的物品序列是配方步骤序列的开头部分，
     * 且顺序完全一致。用于实时验证玩家是否在正确的配方路径上。</p>
     *
     * @param placedItems 已放置的物品类型列表
     * @return 如果已放置的物品序列是配方步骤序列的前缀，则返回true
     */
    public boolean matchesPrefix(List<Item> placedItems) {
        // 如果已放置的物品数量超过配方步骤数量，则不是有效前缀
        if (placedItems.size() > this.steps.size()) {
            return false;
        }

        // 检查已放置的每个物品是否与对应步骤匹配
        for (int i = 0; i < placedItems.size(); i++) {
            if (placedItems.get(i) != steps.get(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取配方所需的输入物品列表。
     *
     * <p>此方法返回配方所有步骤所需的物品，用于显示配方需求。
     * 注意：这不包括容器物品。</p>
     *
     * @return 输入物品列表
     */
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> ingredients = DefaultedList.of();

        // 将每个步骤的物品转换为Ingredient
        for (Item stepItem : steps) {
            ingredients.add(Ingredient.ofItems(stepItem));
        }

        return ingredients;
    }

    /**
     * 检查此配方是否是另一个配方的子集。
     *
     * <p>例如，配方A的步骤是[面包, 奶酪]，配方B的步骤是[面包, 奶酪, 火腿]，
     * 那么配方A就是配方B的子集。用于检测配方冲突。</p>
     *
     * @param other 要比较的另一个配方
     * @return 如果此配方的步骤是另一个配方步骤的前缀，则返回true
     */
    public boolean isPrefixOf(PlatingRecipe other) {
        // 如果此配方的步骤数量大于另一个配方，则不可能是前缀
        if (this.steps.size() > other.steps.size()) {
            return false;
        }

        // 检查容器类型是否相同
        if (this.container != other.container) {
            return false;
        }

        // 检查每个步骤是否匹配
        for (int i = 0; i < this.steps.size(); i++) {
            if (this.steps.get(i) != other.steps.get(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取下一个步骤所需的物品（如果有）。
     *
     * @param currentStep 当前已完成的步骤数（0表示还未开始）
     * @return 下一个步骤所需的物品，如果所有步骤已完成则返回null
     */
    public Item getNextStepItem(int currentStep) {
        if (currentStep < 0 || currentStep >= steps.size()) {
            return null;
        }
        return steps.get(currentStep);
    }

    /**
     * 获取配方的描述性名称，用于日志和调试。
     *
     * @return 配方的描述性字符串
     */
    @Override
    public String toString() {
        return String.format("PlatingRecipe{id=%s, container=%s, steps=%d, output=%s}",
                id, container, steps.size(), output);
    }
}