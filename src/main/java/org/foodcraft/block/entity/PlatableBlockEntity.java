package org.foodcraft.block.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.block.process.PlatingProcess;
import org.foodcraft.recipe.PlatingRecipe;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 可摆盘的方块实体接口，定义摆盘方块必须实现的功能。
 *
 * <p><strong>设计理念：</strong></p>
 * <ul>
 *   <li><strong>状态管理</strong>：方块实体只管理已放置的物品状态，不管理配方逻辑</li>
 *   <li><strong>流程分离</strong>：配方匹配和流程逻辑由 {@link PlatingProcess} 处理</li>
 *   <li><strong>最小接口</strong>：只暴露必要的操作，保持接口简洁</li>
 *   <li><strong>类型安全</strong>：通过泛型确保流程只能操作兼容的方块实体</li>
 * </ul>
 *
 * <p><strong>实现要求：</strong></p>
 * <ol>
 *   <li>必须正确处理物品的放置和移除，确保步骤连续性</li>
 *   <li>必须在状态改变时调用 {@code markDirty()} 来保存数据</li>
 *   <li>应该实现 NBT 序列化来保存已放置的物品</li>
 * </ol>
 *
 * <h2>{@linkplain Inventory}接口实现说明</h2>
 * <p>{@linkplain PlatableBlockEntity}与常规的物品栏不同，其中保存的物品堆栈严格受限于步骤。</p>
 * <p>所以对于物品堆栈的相关操作方法都被重定向至该接口的特定方法，一般情况下无需重写它们。
 * 除非库存接口不完全服务于{@linkplain PlatingProcess}。</p>
 *
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * public class PlateBlockEntity extends BlockEntity implements PlatableBlockEntity {
 *     // 实现所有接口方法...
 * }
 * }</pre>
 *
 * @see Inventory
 * @see PlatingProcess
 */
public interface PlatableBlockEntity extends Inventory {

    // ==================== 容器信息方法 ====================

    /**
     * 获取容器的物品类型。
     *
     * <p>此方法返回容器本身的物品类型（如铁盘、陶瓷盘等），用于配方匹配。
     * 不同的容器类型支持不同的配方集合。</p>
     *
     * <p><strong>实现要求：</strong></p>
     * <ul>
     *   <li>必须返回非空物品类型</li>
     *   <li>对于同一类型的容器，此方法应始终返回相同的物品</li>
     *   <li>如果容器类型可能改变，应通过方块状态或 NBT 数据管理</li>
     * </ul>
     *
     * @return 容器的物品类型，不能为 {@code null}
     */
    Item getContainerType();

    // ==================== 物品管理方法 ====================

    /**
     * 获取当前已放置的所有物品堆栈。
     *
     * <p>返回的列表按照放置顺序排列，第一个元素是第一步放置的物品。
     * 列表中的物品堆栈通常是数量为 1 的副本。</p>
     *
     * <p><strong>实现要求：</strong></p>
     * <ul>
     *   <li>必须返回按顺序排列的已放置物品列表</li>
     *   <li>如果没有放置任何物品，应返回空列表</li>
     *   <li>返回的物品堆栈应该是副本，防止外部修改内部状态</li>
     *   <li>列表应该只包含实际存在的物品，不包含空位</li>
     * </ul>
     *
     * @return 已放置物品的列表，按步骤顺序排列
     */
    List<ItemStack> getPlacedItems();

    /**
     * 在指定步骤位置放置物品。
     *
     * <p>此方法尝试在给定的步骤索引处放置一个物品。实现必须验证：</p>
     * <ol>
     *   <li>步骤索引是否有效</li>
     *   <li>物品是否非空</li>
     *   <li>步骤连续性：不能跳过步骤放置（即前面的步骤必须有物品）</li>
     *   <li>目标步骤位置是否为空</li>
     * </ol>
     *
     * <p><strong>步骤连续性规则：</strong></p>
     * <ul>
     *   <li>第 0 步可以直接放置物品</li>
     *   <li>第 n 步（n > 0）只有在第 n-1 步有物品时才能放置</li>
     *   <li>不允许跳过步骤（如第 0 步为空，直接放置第 1 步）</li>
     * </ul>
     *
     * @param step 步骤索引（从 0 开始）
     * @param item 要放置的物品堆栈，通常数量为 1
     * @return 如果放置成功返回 {@code true}，否则返回 {@code false}
     *
     * @throws IllegalArgumentException 如果步骤索引无效或物品为空
     */
    boolean placeItem(int step, ItemStack item);

    /**
     * 移除指定步骤的物品。
     *
     * <p>此方法移除指定步骤的物品。根据摆盘逻辑，如果移除的是中间步骤的物品，
     * 则后续所有步骤的物品都应该被移除，以保持步骤连续性。</p>
     *
     * <p><strong>连锁移除规则：</strong></p>
     * <ul>
     *   <li>移除第 n 步的物品时，所有步骤 > n 的物品都应该被移除</li>
     *   <li>返回被移除的单个物品堆栈（通常是第 n 步的物品）</li>
     *   <li>如果步骤无效或为空，返回空堆栈</li>
     * </ul>
     *
     * @param step 要移除物品的步骤索引（从 0 开始）
     * @return 被移除的物品堆栈，如果步骤为空或无效则返回 {@link ItemStack#EMPTY}
     */
    ItemStack removeItem(int step);

    /**
     * 清空所有已放置的物品。
     *
     * <p>此方法清除所有步骤的物品，将摆盘状态重置为初始空状态。
     * 通常在配方完成或流程重置时调用。</p>
     */
    void clearPlacedItems();

    /**
     * 检查物品是否为该容器的完成物品。
     *
     * <p>完成物品用于触发摆盘流程的完成步骤（如盖子、酱汁等）。
     * 当玩家手持完成物品右键摆盘方块时，流程会检查当前摆盘状态是否匹配某个配方，
     * 如果匹配则输出最终物品。</p>
     *
     * <p><strong>实现建议：</strong></p>
     * <ul>
     *   <li>可以通过物品标签或特定物品类型来判断</li>
     *   <li>不同的容器类型可以有相同的完成物品（如通用盖子）</li>
     *   <li>也可以有容器特定的完成物品（如特定酱汁）</li>
     * </ul>
     *
     * @param stack 要检查的物品堆栈
     * @return 如果是完成物品返回 {@code true}，否则返回 {@code false}
     */
    boolean isCompletionItem(ItemStack stack);

    /**
     * 当摆盘流程成功完成时调用。
     *
     * <p>此方法在摆盘流程成功完成、输出物品后被调用，方块实体可以在此方法中：</p>
     * <ul>
     *   <li>清空已放置的物品</li>
     *   <li>播放自定义音效或粒子效果</li>
     *   <li>更新方块状态</li>
     *   <li>触发其他事件</li>
     * </ul>
     *
     * <p><strong>注意：</strong>此方法由 {@link PlatingProcess} 在完成步骤中调用，
     * 流程本身不会直接操作方块实体的物品状态。</p>
     *
     * @param world 世界实例
     * @param pos 方块位置
     * @param recipe 完成的配方
     */
    void onPlatingComplete(World world, BlockPos pos, PlatingRecipe recipe);

    // ==================== Inventory ====================

    /**
     * 重定向至{@link #placeItem(int, ItemStack)}
     */
    @Override
    default void setStack(int slot, ItemStack stack) {
        placeItem(slot, stack);
    }

    /**
     * 重定向至{@link #removeItem(int)}
     */
    @Override
    default ItemStack removeStack(int slot) {
        return removeItem(slot);
    }

    /**
     * 等价于{@link #removeStack(int)}
     */
    @Override
    default ItemStack removeStack(int slot, int amount) {
        return removeItem(slot);
    }

    /**
     * 默认玩家始终可以操作
     */
    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    /**
     * @apiNote  如果是为了清空步骤放置的物品，请使用{@link #clearPlacedItems()}
     */
    @Override
    default void clear() {
        clearPlacedItems();
    }

    // ==================== 便捷默认方法 ====================

    /**
     * 获取当前已完成的步骤数量。
     *
     * <p>这是一个便捷方法，等价于 {@code getPlacedItems().size()}。</p>
     *
     * @return 已放置物品的数量（当前步骤数）
     */
    default int getStepCount() {
        return getPlacedItems().size();
    }

    /**
     * 检查指定步骤是否有物品。
     *
     * @param step 要检查的步骤索引（从 0 开始）
     * @return 如果该步骤有物品返回 {@code true}，否则返回 {@code false}
     */
    default boolean hasItemAt(int step) {
        List<ItemStack> items = getPlacedItems();
        return step >= 0 && step < items.size() && !items.get(step).isEmpty();
    }

    /**
     * 获取已放置物品的类型列表。
     *
     * <p>这是一个便捷方法，返回已放置物品的物品类型列表。
     * 用于配方匹配时只需要物品类型而不关心堆栈的其他属性。</p>
     *
     * @return 已放置物品类型的列表，按步骤顺序排列
     */
    default List<Item> getPlacedItemTypes() {
        return getPlacedItems().stream()
                .map(ItemStack::getItem)
                .collect(Collectors.toList());
    }

    /**
     * 检查当前摆放是否完全匹配指定配方。
     *
     * <p>此方法检查容器的物品类型和所有已放置物品是否与配方完全匹配。</p>
     *
     * @param recipe 要检查的配方
     * @return 如果容器类型和所有步骤物品都匹配返回 {@code true}，否则返回 {@code false}
     */
    default boolean matchesRecipeExactly(PlatingRecipe recipe) {
        if (recipe == null) return false;
        // 检查容器类型
        if (getContainerType() != recipe.getContainer()) return false;
        // 检查步骤
        return recipe.matchesSteps(getPlacedItemTypes());
    }

    /**
     * 检查当前摆放是否是指定配方的有效前缀。
     *
     * <p>有效前缀意味着当前摆放是配方步骤序列的开头部分，且顺序完全一致。
     * 用于实时验证玩家是否在正确的配方路径上。</p>
     *
     * @param recipe 要检查的配方
     * @return 如果容器类型相同且当前摆放是配方的有效前缀返回 {@code true}
     */
    default boolean matchesRecipePrefix(PlatingRecipe recipe) {
        if (recipe == null) return false;
        // 检查容器类型
        if (getContainerType() != recipe.getContainer()) return false;
        // 检查步骤前缀
        return recipe.matchesPrefix(getPlacedItemTypes());
    }

    /**
     * 检查是否可以开始新的摆盘流程。
     *
     * <p>此方法检查当前摆盘状态是否允许开始新的流程。
     * 通常当摆盘为空时允许开始新的流程。</p>
     *
     * @return 如果可以开始新流程返回 {@code true}，否则返回 {@code false}
     */
    default boolean canStartNewProcess() {
        return getPlacedItems().isEmpty();
    }

    /**
     * 获取最大可放置步骤数量。
     *
     * <p>这是一个可选方法，用于限制单个摆盘的容量。
     * 默认返回 {@link #size()} 表示和对应的方块实体容量相关。</p>
     *
     * @return 最大可放置步骤数量
     */
    default int getMaxSteps() {
        return size();
    }

    /**
     * 检查是否可以在指定步骤放置物品。
     *
     * <p>此方法综合检查步骤有效性、连续性和容量限制。</p>
     *
     * @param step 要检查的步骤索引
     * @return 如果可以在该步骤放置物品返回 {@code true}
     */
    default boolean canPlaceItemAtStep(int step) {
        // 检查步骤是否在有效范围内
        if (step < 0 || step >= getMaxSteps()) {
            return false;
        }

        // 检查步骤连续性：只能按顺序放置
        for (int i = 0; i < step; i++) {
            if (!hasItemAt(i)) {
                return false;
            }
        }

        // 检查该步骤是否已有物品
        return !hasItemAt(step);
    }
}