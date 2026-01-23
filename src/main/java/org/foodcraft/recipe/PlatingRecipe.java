package org.foodcraft.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.foodcraft.block.entity.PlatableBlockEntity;
import org.foodcraft.block.process.playeraction.PlayerAction;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.DishesContent;
import org.foodcraft.contentsystem.occupy.OccupyUtil;
import org.foodcraft.registry.ModRecipeSerializers;
import org.foodcraft.registry.ModRecipeTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 摆盘配方类，表示一个完整的摆盘配方。
 *
 * <p>摆盘配方由以下部分组成：</p>
 * <ul>
 *   <li><strong>容器</strong>：配方的承载容器（如铁盘、木盘）</li>
 *   <li><strong>操作序列</strong>：按顺序执行的操作列表</li>
 *   <li><strong>输出</strong>：完成摆盘后得到的最终物品</li>
 * </ul>
 *
 * <p><strong>配方匹配规则：</strong></p>
 * <ol>
 *   <li>必须使用正确的容器类型</li>
 *   <li>必须按照操作序列的顺序执行操作</li>
 *   <li>不允许跳过任何操作</li>
 *   <li>当所有操作完成后，使用特定的完成物品触发输出</li>
 * </ol>
 */
public class PlatingRecipe implements Recipe<PlatableBlockEntity> {
    /** 基础容器 -> 菜肴 -> 配方列表的映射。用于回溯配方 */
    private static final Map<Item, Map<DishesContent, PlatingRecipe>> RESTORE = new HashMap<>();

    /** 配方ID，用于唯一标识此配方 */
    private final Identifier id;

    /** 容器物品类型，表示此配方所需的容器（如铁盘） */
    private final Item container;

    /** 操作序列，按顺序执行的操作列表 */
    private final List<PlayerAction> actions;

    /** 配方输出菜肴，完成所有操作后获得 */
    private final DishesContent output;

    /**
     * 创建摆盘配方。
     *
     * @param id 配方ID，用于唯一标识此配方
     * @param container 容器物品类型
     * @param actions 操作列表，列表顺序即为执行顺序
     * @param output 配方输出物品
     */
    public PlatingRecipe(Identifier id, Item container, List<PlayerAction> actions, AbstractContent output) {
        if (output instanceof DishesContent dishes) {
            this.id = id;
            this.container = container;
            this.actions = List.copyOf(actions);
            this.output = dishes;

            // 将配方添加到RESTORE映射中，用于回溯
            Map<DishesContent, PlatingRecipe> containerMap = RESTORE.computeIfAbsent(container, k -> new HashMap<>());
            // 将当前配方放入映射中，以菜肴内容为键
            containerMap.put(dishes, this);
        } else {
            throw new IllegalArgumentException("The product of the recipe for the dish must be dishes");
        }
    }

    @Override
    public boolean matches(PlatableBlockEntity inventory, World world) {
        // 首先检查容器类型是否匹配
        if (inventory.getContainerType() != this.container) {
            return false;
        }

        // 获取当前已执行的操作列表
        List<PlayerAction> performedActions = inventory.getPerformedActions();

        // 检查操作数量是否匹配
        if (performedActions.size() != this.actions.size()) {
            return false;
        }

        // 检查每个操作是否匹配
        for (int i = 0; i < actions.size(); i++) {
            if (!actions.get(i).matches(performedActions.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack craft(PlatableBlockEntity inventory, DynamicRegistryManager registryManager) {
        return OccupyUtil.createAbstractOccupy(output);
    }

    @Override
    public boolean fits(int width, int height) {
        // 摆盘配方不使用传统的物品栏格子，总是返回true
        return true;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return OccupyUtil.createAbstractOccupy(output);
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
     */
    public Item getContainer() {
        return container;
    }

    /**
     * 获取配方的操作序列。
     *
     * <p>返回的是操作序列的不可修改副本，确保外部不能修改内部状态。</p>
     */
    public List<PlayerAction> getActions() {
        return actions;
    }

    /**
     * 获取配方操作数量。
     */
    public int getActionCount() {
        return actions.size();
    }

    /**
     * 获取指定索引的操作。
     *
     * @param index 操作索引（从0开始）
     * @return 该步骤所需的操作
     * @throws IndexOutOfBoundsException 如果索引超出范围
     */
    public PlayerAction getActionAt(int index) {
        return actions.get(index);
    }

    /**
     * 获取配方的成品
     * @return 制作出的菜肴
     */
    public DishesContent getDishes() {
        return this.output;
    }

    // ==================== 配方匹配辅助方法 ====================

    /**
     * 检查给定的已执行操作列表是否与配方的所有操作完全匹配。
     */
    public boolean matchesActions(List<PlayerAction> performedActions) {
        // 检查操作数量是否相同
        if (performedActions.size() != this.actions.size()) {
            return false;
        }

        // 检查每个操作
        for (int i = 0; i < actions.size(); i++) {
            if (!actions.get(i).matches(performedActions.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查给定的已执行操作列表是否是此配方的有效前缀。
     */
    public boolean matchesPrefix(List<PlayerAction> performedActions) {
        // 如果已执行的操作数量超过配方操作数量，则不是有效前缀
        if (performedActions.size() > this.actions.size()) {
            return false;
        }

        // 检查已执行的每个操作是否与对应操作匹配
        for (int i = 0; i < performedActions.size(); i++) {
            if (!actions.get(i).matches(performedActions.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取配方所需的输入物品列表（用于UI显示）。
     */
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> ingredients = DefaultedList.of();

        // 将每个操作转换为Ingredient
        for (PlayerAction action : actions) {
            ItemStack stack = action.toItemStack();
            if (!stack.isEmpty()) {
                ingredients.add(Ingredient.ofStacks(stack));
            } else {
                // 对于没有物品表示的操作，添加空Ingredient
                ingredients.add(Ingredient.EMPTY);
            }
        }

        return ingredients;
    }

    // ==================== 静态方法 ====================

    /**
     * 根据可摆盘方块实体的容器和菜肴内容查找对应的配方。
     */
    public static PlatingRecipe getRecipeFromEntity(PlatableBlockEntity entity) {
        if (entity == null) {
            return null;
        }

        // 获取容器的菜肴内容
        DishesContent outcome = entity.getOutcome();
        if (outcome == null) {
            return null;
        }

        return getRecipeByContainerAndDishes(entity.getContainerType(), outcome);
    }

    /**
     * 根据容器和菜肴内容查找对应的配方。
     */
    public static PlatingRecipe getRecipeByContainerAndDishes(Item container, DishesContent dishes) {
        if (container == null || dishes == null) {
            return null;
        }

        Map<DishesContent, PlatingRecipe> containerMap = RESTORE.get(container);
        if (containerMap != null) {
            return containerMap.get(dishes);
        }
        return null;
    }

    /**
     * 获取下一个操作（如果有）。
     */
    public PlayerAction getNextAction(int currentStep) {
        if (currentStep < 0 || currentStep >= actions.size()) {
            return null;
        }
        return actions.get(currentStep);
    }

    @Override
    public String toString() {
        return String.format("PlatingRecipe{id=%s, container=%s, actions=%d, output=%s}",
                id, container, actions.size(), output);
    }
}