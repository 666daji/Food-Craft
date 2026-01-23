package org.foodcraft.block.process;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.foodcraft.block.entity.PlatableBlockEntity;
import org.foodcraft.block.process.playeraction.PlayerAction;
import org.foodcraft.block.process.playeraction.impl.AddItemPlayerAction;
import org.foodcraft.block.process.step.Step;
import org.foodcraft.block.process.step.StepExecutionContext;
import org.foodcraft.block.process.step.StepResult;
import org.foodcraft.recipe.PlatingRecipe;
import org.foodcraft.registry.ModRecipeTypes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 摆盘流程类，管理摆盘的多步骤交互流程。
 *
 * <p><strong>设计特点：</strong></p>
 * <ul>
 *   <li>通用候选配方初始化：支持从任意状态恢复流程</li>
 *   <li>简化的状态管理：方块实体只存储操作，流程只管理候选列表</li>
 *   <li>无需NBT恢复：退出重进后自动重新初始化候选列表</li>
 *   <li>支持撤销操作：完成物品放置后仍可撤回继续</li>
 * </ul>
 */
public class PlatingProcess<T extends BlockEntity & PlatableBlockEntity> extends AbstractProcess<T> {
    /** 执行操作步骤的ID */
    public static final String STEP_PERFORM_ACTION = "perform_action";
    /** 完成流程步骤的ID */
    public static final String STEP_COMPLETE = "complete";

    /** 当前步骤的候选配方列表 */
    private final List<PlatingRecipe> candidateRecipes = new ArrayList<>();

    /** 当前完全匹配的配方（如果存在） */
    @Nullable
    private PlatingRecipe matchedRecipe = null;

    /** 标志：是否已初始化候选配方列表 */
    private boolean hasInitializedCandidates = false;

    /** 标志：是否正在匹配配方，防止重入 */
    private boolean isMatchingRecipes = false;

    // ==================== 构造器和初始化 ====================

    public PlatingProcess() {
        registerSteps();
        setInitialStep(STEP_PERFORM_ACTION);
    }

    private void registerSteps() {
        registerStep(STEP_PERFORM_ACTION, new PerformActionStep());
        registerStep(STEP_COMPLETE, new CompleteStep());
    }

    // ==================== 步骤实现类 ====================

    /**
     * 执行操作步骤，处理配方操作的执行。
     *
     * <p>此步骤按照严格顺序执行：</p>
     * <ol>
     *   <li>防止重入</li>
     *   <li>如果候选列表未初始化，调用 initializeCandidates 初始化</li>
     *   <li>从上下文创建 PlayerAction</li>
     *   <li>根据是否已初始化选择不同逻辑</li>
     * </ol>
     */
    protected class PerformActionStep implements Step<T> {
        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            // 防止重入逻辑
            if (isMatchingRecipes) {
                return StepResult.continueSameStep(ActionResult.PASS);
            }

            // 操作的方块实体
            PlatableBlockEntity plate = context.blockEntity();

            // 从上下文创建预期操作
            PlayerAction expectedAction = createActionFromContext(context);

            // 已执行操作列表
            List<PlayerAction> performedActions = plate.getPerformedActions();
            int currentStep = plate.getStepCount();

            // 如果候选列表未初始化，尝试初始化
            if (!hasInitializedCandidates) {
                // 使用预期操作（可能为null）初始化候选列表
                if (!initializeCandidates(context.world(), plate, expectedAction)) {
                    // 初始化失败，尝试不包含当前操作的初始化
                    initializeCandidates(context.world(), plate);

                    if (performedActions.isEmpty()) {
                        // 如果此时操作列表为空则重置流程
                        reset();
                    }

                    return StepResult.fail(STEP_PERFORM_ACTION, ActionResult.PASS);
                }

                // 如果预期操作为null，步骤返回PASS（无感知初始化）
                if (expectedAction == null) {
                    return StepResult.continueSameStep(ActionResult.PASS);
                }
            }
            // 候选列表已初始化
            else {
                // 如果预期操作为null，步骤直接返回PASS
                if (expectedAction == null) {
                    return StepResult.continueSameStep(ActionResult.PASS);
                }

                // 使用预期操作过滤候选列表
                List<PlatingRecipe> matchingRecipes = filterCandidatesByNextAction(
                        expectedAction, performedActions
                );

                // 过滤到的列表为空时，步骤失败
                if (matchingRecipes.isEmpty()) {
                    return StepResult.fail(STEP_PERFORM_ACTION, ActionResult.FAIL);
                }

                // 更新候选列表
                candidateRecipes.clear();
                candidateRecipes.addAll(matchingRecipes);
            }

            return executeAction(context, plate, expectedAction, currentStep);
        }

        @Nullable
        private PlayerAction createActionFromContext(StepExecutionContext<T> context) {
            ItemStack heldItem = context.getHeldItemStack();

            if (!heldItem.isEmpty()) {
                // 摆盘流程：手持物品 -> AddItemPlayerAction
                return new AddItemPlayerAction(heldItem.getItem(), 1);
            }

            return null;
        }

        /**
         * 执行操作逻辑的公共部分。
         */
        private StepResult executeAction(StepExecutionContext<T> context, PlatableBlockEntity plate,
                                         PlayerAction action, int currentStep) {
            // 在服务器端执行操作
            if (context.isServerSide()) {
                // 验证是否可以在此步骤执行操作
                if (!plate.canPerformActionAtStep(currentStep)) {
                    resetCandidateState();
                    return StepResult.fail(STEP_PERFORM_ACTION, ActionResult.FAIL);
                }

                // 尝试执行操作
                if (!plate.performAction(currentStep, action)) {
                    resetCandidateState();
                    return StepResult.fail(STEP_PERFORM_ACTION, ActionResult.FAIL);
                }

                // 执行操作的消耗逻辑
                action.consume(context);
                plate.markDirty();
            }

            // 检查是否有完全匹配的配方
            checkForExactMatch(plate, null);

            return StepResult.continueSameStep(ActionResult.SUCCESS);
        }
    }

    /**
     * 完成流程步骤，处理配方的完成和输出。
     */
    private class CompleteStep implements Step<T> {
        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            PlatableBlockEntity plate = context.blockEntity();
            ItemStack heldItem = context.getHeldItemStack();

            // 检查是否为完成物品
            if (!plate.isCompletionItem(heldItem) || heldItem.isEmpty()) {
                return StepResult.fail(STEP_PERFORM_ACTION, ActionResult.FAIL);
            }

            // 检查是否有完全匹配的配方
            if (matchedRecipe == null) {
                // 如果没有匹配的配方，但玩家手持完成物品，尝试重新检查
                checkForExactMatch(plate, context.world());
                if (matchedRecipe == null) {
                    return StepResult.fail(STEP_PERFORM_ACTION, ActionResult.FAIL);
                }
            }

            // 执行完成逻辑
            plate.onPlatingComplete(context.world(), context.pos(), matchedRecipe, context.player(), context.hand(), context.hit());

            return StepResult.complete(ActionResult.SUCCESS);
        }
    }

    // ==================== 核心算法方法 ====================

    /**
     * 通用候选配方初始化方法。
     *
     * @param world 世界实例
     * @param plate 摆盘方块实体
     * @param expectedAction 当前要执行的操作（可能为空）
     * @return 如果找到至少一个候选配方返回 {@code true}
     */
    public boolean initializeCandidates(World world, PlatableBlockEntity plate, @Nullable PlayerAction expectedAction) {
        isMatchingRecipes = true;
        try {
            RecipeManager recipeManager = world.getRecipeManager();
            List<PlatingRecipe> allRecipes = recipeManager.listAllOfType(ModRecipeTypes.PLATING);

            if (allRecipes.isEmpty()) {
                return false;
            }

            List<PlayerAction> performedActions = plate.getPerformedActions();
            Item containerType = plate.getContainerType();

            // 构建临时匹配列表：已执行操作 + 预期操作（如果不为null）
            List<PlayerAction> tempMatchingList = new ArrayList<>(performedActions);

            if (expectedAction != null) {
                tempMatchingList.add(expectedAction);
            }

            // 如果临时列表为空，无法匹配任何配方
            if (tempMatchingList.isEmpty()) {
                return false;
            }

            List<PlatingRecipe> candidates = allRecipes.stream()
                    .filter(recipe -> recipe.getContainer() == containerType)
                    .filter(recipe -> recipe.matchesPrefix(tempMatchingList))
                    .toList();

            if (candidates.isEmpty()) {
                return false;
            }

            candidateRecipes.clear();
            candidateRecipes.addAll(candidates);
            hasInitializedCandidates = true;

            // 检查完全匹配
            checkForExactMatch(plate, world);
            return true;
        } finally {
            isMatchingRecipes = false;
        }
    }

    public boolean initializeCandidates(World world, PlatableBlockEntity plate) {
        return initializeCandidates(world, plate, null);
    }

    /**
     * 根据下一步操作过滤候选配方。
     *
     * @param nextAction 下一步要执行的操作
     * @param performedActions 已执行的操作列表
     * @return 过滤后的候选配方列表，只包含下一步匹配的配方
     */
    private List<PlatingRecipe> filterCandidatesByNextAction(PlayerAction nextAction, List<PlayerAction> performedActions) {
        return candidateRecipes.stream()
                .filter(recipe -> {
                    // 如果已执行操作数量 >= 配方操作数量，不是有效候选
                    if (performedActions.size() >= recipe.getActionCount()) {
                        return false;
                    }

                    // 检查已执行操作是否是配方的有效前缀
                    if (!recipe.matchesPrefix(performedActions)) {
                        return false;
                    }

                    // 检查下一步是否匹配
                    PlayerAction nextRecipeAction = recipe.getNextAction(performedActions.size());
                    return nextRecipeAction != null && nextAction.matches(nextRecipeAction);
                })
                .collect(Collectors.toList());
    }

    /**
     * 检查当前摆盘状态是否有完全匹配的配方。
     */
    public void checkForExactMatch(PlatableBlockEntity plate, World world) {
        matchedRecipe = candidateRecipes.stream()
                .filter(recipe -> recipe.matches(plate, world))
                .findFirst()
                .orElse(null);
    }

    /**
     * 重置候选配方状态。
     */
    private void resetCandidateState() {
        candidateRecipes.clear();
        matchedRecipe = null;
        hasInitializedCandidates = false;
        isMatchingRecipes = false;
    }

    // ==================== 流程控制钩子 ====================

    /**
     * 步骤获取前的预处理钩子。
     *
     * <p>当玩家手持完成物品时，如果当前摆盘状态匹配某个配方，
     * 直接跳转到完成步骤。</p>
     */
    @Override
    protected void beforeGetStep(StepExecutionContext<T> context) {
        T plate = context.blockEntity();
        ItemStack heldItem = context.getHeldItemStack();

        // 检查是否是完成物品
        if (plate.isCompletionItem(heldItem) && !heldItem.isEmpty()) {
            // 检查是否有完全匹配的配方
            checkForExactMatch(plate, context.world());
            if (matchedRecipe != null) {
                // 跳转到完成步骤
                jumpToStep(STEP_COMPLETE);
            }
        }
    }

    // ==================== 生命周期方法 ====================

    @Override
    protected String getInitialStepId() {
        return STEP_PERFORM_ACTION;
    }

    @Override
    protected void onStart(World world, T blockEntity) {
        // 开始新流程时重置状态
        resetCandidateState();
    }

    @Override
    protected void onReset() {
        resetCandidateState();
    }

    // ==================== 状态查询方法 ====================

    /**
     * 获取当前候选配方数量。
     */
    public int getCandidateRecipeCount() {
        return candidateRecipes.size();
    }

    /**
     * 获取当前匹配的配方。
     */
    public @Nullable PlatingRecipe getMatchedRecipe() {
        return matchedRecipe;
    }

    /**
     * 检查是否已找到完全匹配的配方。
     */
    public boolean hasExactMatch() {
        return matchedRecipe != null;
    }

    /**
     * 检查候选列表是否已初始化。
     */
    public boolean isCandidatesInitialized() {
        return hasInitializedCandidates;
    }

    @Override
    protected String getCustomStatusInfo() {
        StringBuilder info = new StringBuilder();

        // 候选配方信息
        info.append("候选配方数量: ").append(candidateRecipes.size()).append("\n");

        // 匹配的配方信息
        if (matchedRecipe != null) {
            info.append("完全匹配的配方: ").append(matchedRecipe.getId().getPath()).append("\n");
            info.append("配方操作数: ").append(matchedRecipe.getActionCount()).append("\n");
            info.append("输出菜肴: ").append(matchedRecipe.getDishes()).append("\n");
        } else {
            info.append("完全匹配的配方: <无>\n");
        }

        // 初始化状态
        info.append("候选列表已初始化: ").append(hasInitializedCandidates).append("\n");

        // 匹配状态
        info.append("正在匹配配方: ").append(isMatchingRecipes).append("\n");

        // 候选配方详情（仅显示前3个，避免输出过长）
        if (!candidateRecipes.isEmpty()) {
            info.append("候选配方列表:\n");
            int limit = Math.min(candidateRecipes.size(), 3);
            for (int i = 0; i < limit; i++) {
                PlatingRecipe recipe = candidateRecipes.get(i);
                info.append("  ").append(i + 1).append(". ")
                        .append(recipe.getId().getPath())
                        .append(" (操作: ").append(recipe.getActionCount()).append(")\n");
            }
            if (candidateRecipes.size() > limit) {
                info.append("  ... 还有").append(candidateRecipes.size() - limit).append("个配方未显示\n");
            }
        }
        return info.toString();
    }
}