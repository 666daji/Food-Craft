package org.foodcraft.block.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.block.process.PlatingProcess;
import org.foodcraft.block.process.playeraction.PlayerAction;
import org.foodcraft.block.process.playeraction.impl.AddItemPlayerAction;
import org.foodcraft.contentsystem.content.DishesContent;
import org.foodcraft.recipe.PlatingRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 可摆盘的方块实体接口，定义摆盘方块必须实现的功能。
 *
 * <p><strong>设计理念：</strong></p>
 * <ul>
 *   <li><strong>操作导向</strong>：方块实体管理已执行的操作（PlayerAction），而非物品</li>
 *   <li><strong>流程分离</strong>：配方匹配和流程逻辑由 {@link org.foodcraft.block.process.PlatingProcess} 处理</li>
 *   <li><strong>灵活扩展</strong>：支持各种类型的玩家操作（添加物品、剪切、倾倒等）</li>
 *   <li><strong>库存兼容</strong>：通过操作转换为物品堆栈，兼容原版库存接口</li>
 * </ul>
 *
 * <p><strong>核心概念：</strong></p>
 * <ol>
 *   <li><strong>操作（PlayerAction）</strong>：代表玩家执行的具体交互，如添加物品、使用工具等</li>
 *   <li><strong>操作序列</strong>：按顺序执行的操作列表，构成完整的摆盘流程</li>
 *   <li><strong>容器类型</strong>：配方的承载容器，决定可用的配方集合</li>
 *   <li><strong>完成操作</strong>：触发流程完成的特殊操作（如盖上盖子）</li>
 * </ol>
 *
 * <p><strong>实现要求：</strong></p>
 * <ol>
 *   <li>必须正确管理操作序列的连续性和完整性</li>
 *   <li>必须在状态改变时调用 {@code markDirty()} 来保存数据</li>
 *   <li>必须实现 NBT 序列化来保存已执行的操作</li>
 *   <li>必须确保操作到物品堆栈的转换正确无误</li>
 * </ol>
 *
 * @see PlatingProcess
 * @see PlayerAction
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

    /**
     * 获取当前容器中可能的菜肴。
     *
     * <p>当摆盘流程完成时，容器中会生成菜肴内容。
     * 如果流程尚未完成，此方法应返回 {@code null}。</p>
     *
     * @return 当前容器中的菜肴，没有时为 {@code null}
     */
    @Nullable
    DishesContent getOutcome();

    // ==================== 操作管理方法 ====================

    /**
     * 获取当前已执行的所有操作。
     *
     * <p>返回的列表按照执行顺序排列，第一个元素是第一步执行的操作。
     * 操作列表必须是连续的，不允许有空位或跳过步骤。</p>
     *
     * <p><strong>实现要求：</strong></p>
     * <ul>
     *   <li>必须返回按顺序排列的已执行操作列表</li>
     *   <li>如果没有执行任何操作，应返回空列表</li>
     *   <li>返回的操作应该是副本或不可修改的视图，防止外部修改内部状态</li>
     *   <li>列表应该只包含实际存在的操作，不包含空位</li>
     * </ul>
     *
     * @return 已执行操作的列表，按步骤顺序排列
     */
    List<PlayerAction> getPerformedActions();

    /**
     * 在指定步骤位置执行操作。
     *
     * <p>此方法尝试在给定的步骤索引处执行一个操作。实现必须验证：</p>
     * <ol>
     *   <li>步骤索引是否有效（0 ≤ index < getMaxSteps()）</li>
     *   <li>操作是否非空</li>
     *   <li>步骤连续性：不能跳过步骤执行（即前面的步骤必须有操作）</li>
     *   <li>目标步骤位置是否为空</li>
     * </ol>
     *
     * <p><strong>步骤连续性规则：</strong></p>
     * <ul>
     *   <li>第 0 步可以直接执行操作</li>
     *   <li>第 n 步（n > 0）只有在第 n-1 步有操作时才能执行</li>
     *   <li>不允许跳过步骤（如第 0 步为空，直接执行第 1 步）</li>
     * </ul>
     *
     * @param step 步骤索引（从 0 开始）
     * @param action 要执行的操作
     * @return 如果执行成功返回 {@code true}，否则返回 {@code false}
     *
     * @throws IllegalArgumentException 如果步骤索引无效或操作为空
     */
    boolean performAction(int step, PlayerAction action);

    /**
     * 移除指定步骤的操作。
     *
     * <p>此方法移除指定步骤的操作。根据摆盘逻辑，如果移除的是中间步骤的操作，
     * 则后续所有步骤的操作都应该被移除，以保持步骤连续性。</p>
     *
     * <p><strong>连锁移除规则：</strong></p>
     * <ul>
     *   <li>移除第 n 步的操作时，所有步骤 > n 的操作都应该被移除</li>
     *   <li>返回被移除的操作</li>
     *   <li>如果步骤无效或为空，返回 {@code null}</li>
     * </ul>
     *
     * @param step 要移除操作的步骤索引（从 0 开始）
     * @return 被移除的操作，如果步骤为空或无效则返回 {@code null}
     */
    @Nullable
    PlayerAction removeAction(int step);

    /**
     * 清空所有已执行的操作。
     *
     * <p>此方法清除所有步骤的操作，将摆盘状态重置为初始空状态。
     * 通常在配方完成或流程重置时调用。</p>
     */
    void clearPerformedActions();

    /**
     * 检查物品是否为该容器的完成物品。
     *
     * <p>完成物品用于触发摆盘流程的完成步骤（如盖子、酱汁等）。
     * 当玩家手持完成物品右键摆盘方块时，流程会检查当前摆盘状态是否匹配某个配方，
     * 如果匹配则输出最终菜肴。</p>
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
     * <p>此方法在摆盘流程成功完成、输出菜肴后被调用，方块实体可以在此方法中：</p>
     * <ul>
     *   <li>清空已执行的操作</li>
     *   <li>设置菜肴内容</li>
     *   <li>播放自定义音效或粒子效果</li>
     *   <li>更新方块状态</li>
     *   <li>触发其他事件</li>
     * </ul>
     *
     * <p><strong>注意：</strong>此方法由摆盘流程在完成步骤中调用，
     * 流程本身不会直接操作方块实体的操作状态。</p>
     *
     * @param world 世界实例
     * @param pos 方块位置
     * @param recipe 完成的配方
     * @param player 操作的玩家
     * @param hand 玩家的手
     * @param hit 操作的上下文
     */
    void onPlatingComplete(World world, BlockPos pos, PlatingRecipe recipe, PlayerEntity player, Hand hand, HitResult hit);

    // ==================== Inventory 接口适配 ====================

    /**
     * 设置指定槽位的物品堆栈。
     *
     * <p>此方法将物品堆栈转换为默认操作并执行。主要用于：
     * <ul>
     *   <li>从 NBT 恢复状态时重建操作序列</li>
     *   <li>外部系统与摆盘方块交互</li>
     * </ul>
     *
     * <p><strong>注意：</strong>此方法不执行操作的消耗逻辑，仅用于状态恢复。</p>
     *
     * @param slot 槽位索引
     * @param stack 要设置的物品堆栈
     */
    @Override
    default void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= size()) {
            return;
        }

        // 将物品堆栈转换为 AddItemPlayerAction
        if (!stack.isEmpty()) {
            PlayerAction action = createActionFromItemStack(stack);
            if (action != null) {
                performAction(slot, action);
            }
        } else {
            // 如果堆栈为空，移除该槽位的操作
            removeAction(slot);
        }
    }

    /**
     * 从指定槽位移除物品堆栈。
     *
     * <p>此方法移除指定槽位的操作，并返回对应的物品堆栈表示。</p>
     *
     * @param slot 槽位索引
     * @return 被移除操作对应的物品堆栈
     */
    @Override
    default ItemStack removeStack(int slot) {
        PlayerAction action = removeAction(slot);
        return action != null ? action.toItemStack() : ItemStack.EMPTY;
    }

    /**
     * 从指定槽位移除指定数量的物品堆栈。
     *
     * <p>对于摆盘系统，操作是不可分割的，因此此方法与 {@link #removeStack(int)} 行为相同。</p>
     *
     * @param slot 槽位索引
     * @param amount 要移除的数量（对于操作系统，此参数被忽略）
     * @return 被移除操作对应的物品堆栈
     */
    @Override
    default ItemStack removeStack(int slot, int amount) {
        return removeStack(slot);
    }

    /**
     * 检查玩家是否可以使用此方块实体。
     *
     * <p>默认实现允许所有玩家使用。子类可以重写此方法以添加权限检查。</p>
     *
     * @param player 要检查的玩家
     * @return 如果玩家可以使用此方块实体，则返回 {@code true}
     */
    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    /**
     * 清空所有槽位。
     *
     * <p>此方法清空所有已执行的操作。</p>
     */
    @Override
    default void clear() {
        clearPerformedActions();
    }

    /**
     * 获取指定槽位的物品堆栈。
     *
     * <p>此方法将槽位对应的操作转换为物品堆栈返回。</p>
     *
     * @param slot 槽位索引
     * @return 操作对应的物品堆栈，如果槽位为空则返回 {@link ItemStack#EMPTY}
     */
    @Override
    default ItemStack getStack(int slot) {
        List<PlayerAction> actions = getPerformedActions();
        if (slot >= 0 && slot < actions.size()) {
            PlayerAction action = actions.get(slot);
            return action != null ? action.toItemStack() : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 检查方块实体是否为空。
     *
     * <p>如果没有任何已执行的操作，则视为空。</p>
     *
     * @return 如果没有任何操作则返回 {@code true}
     */
    @Override
    default boolean isEmpty() {
        return getPerformedActions().isEmpty();
    }

    // ==================== 便捷默认方法 ====================

    /**
     * 获取当前已完成的步骤数量。
     *
     * <p>这是一个便捷方法，等价于 {@code getPerformedActions().size()}。</p>
     *
     * @return 已执行操作的数量（当前步骤数）
     */
    default int getStepCount() {
        return getPerformedActions().size();
    }

    /**
     * 检查指定步骤是否有操作。
     *
     * @param step 要检查的步骤索引（从 0 开始）
     * @return 如果该步骤有操作返回 {@code true}，否则返回 {@code false}
     */
    default boolean hasActionAt(int step) {
        List<PlayerAction> actions = getPerformedActions();
        return step >= 0 && step < actions.size() && actions.get(step) != null;
    }

    /**
     * 获取最大可记录操作数量。
     *
     * <p>这是一个可选方法，用于限制单个摆盘的容量。
     * 默认返回 {@link #size()} 表示和对应的方块实体容量相关。</p>
     *
     * @return 最大可执行步骤数量
     */
    default int getMaxSteps() {
        return size();
    }

    /**
     * 检查是否可以在指定步骤执行操作。
     *
     * <p>此方法综合检查步骤有效性、连续性和容量限制。</p>
     *
     * @param step 要检查的步骤索引
     * @return 如果可以在该步骤执行操作返回 {@code true}
     */
    default boolean canPerformActionAtStep(int step) {
        // 检查步骤是否在有效范围内
        if (step < 0 || step >= getMaxSteps()) {
            return false;
        }

        // 检查步骤连续性：只能按顺序执行
        for (int i = 0; i < step; i++) {
            if (!hasActionAt(i)) {
                return false;
            }
        }

        // 检查该步骤是否已有操作
        return !hasActionAt(step);
    }

    /**
     * 检查当前操作序列是否完全匹配指定配方。
     *
     * <p>此方法检查容器的物品类型和所有已执行操作是否与配方完全匹配。</p>
     *
     * @param recipe 要检查的配方
     * @return 如果容器类型和所有操作都匹配返回 {@code true}，否则返回 {@code false}
     */
    default boolean matchesRecipeExactly(PlatingRecipe recipe) {
        if (recipe == null) return false;
        // 检查容器类型
        if (getContainerType() != recipe.getContainer()) return false;
        // 检查操作序列
        return recipe.matchesActions(getPerformedActions());
    }

    /**
     * 检查当前操作序列是否是指定配方的有效前缀。
     *
     * <p>有效前缀意味着当前操作序列是配方操作序列的开头部分，且顺序完全一致。
     * 用于实时验证玩家是否在正确的配方路径上。</p>
     *
     * @param recipe 要检查的配方
     * @return 如果容器类型相同且当前操作序列是配方的有效前缀返回 {@code true}
     */
    default boolean matchesRecipePrefix(PlatingRecipe recipe) {
        if (recipe == null) return false;
        // 检查容器类型
        if (getContainerType() != recipe.getContainer()) return false;
        // 检查操作前缀
        return recipe.matchesPrefix(getPerformedActions());
    }

    /**
     * 检查是否可以开始新的摆盘流程。
     *
     * <p>此方法检查当前摆盘状态是否允许开始新的流程。
     * 通常当摆盘为空且没有菜肴时允许开始新的流程。</p>
     *
     * @return 如果可以开始新流程返回 {@code true}，否则返回 {@code false}
     */
    default boolean canStartNewProcess() {
        return getPerformedActions().isEmpty() && getOutcome() == null;
    }

    /**
     * 获取操作的显示名称列表。
     *
     * <p>用于调试和UI显示。</p>
     *
     * @return 操作显示名称的列表
     */
    default List<Text> getActionDisplayNames() {
        return getPerformedActions().stream()
                .map(PlayerAction::getDisplayName)
                .toList();
    }

    /**
     * 检查是否有完全匹配的配方。
     *
     * <p>这是一个便捷方法，通常用于UI显示完成状态。</p>
     *
     * @return 如果当前状态匹配某个配方则返回 {@code true}
     */
    default boolean hasCompleteRecipe() {
        return getOutcome() != null;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 从物品堆栈创建操作。
     *
     * <p>默认实现创建 {@code AddItemPlayerAction}。子类可以重写此方法以支持更多操作类型。</p>
     *
     * @param stack 物品堆栈
     * @return 对应的操作，如果无法创建则返回 {@code null}
     */
    default PlayerAction createActionFromItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        // 默认创建添加物品操作
        return new AddItemPlayerAction(stack.getItem(), stack.getCount());
    }

    /**
     * 获取操作的类型标识符列表。
     *
     * <p>用于调试和序列化。</p>
     *
     * @return 操作类型标识符的列表
     */
    default List<String> getActionTypes() {
        return getPerformedActions().stream()
                .map(PlayerAction::getType)
                .toList();
    }
}