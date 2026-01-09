package org.foodcraft.block.process;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.foodcraft.block.entity.PlatableBlockEntity;
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
 *   <li>简化的状态管理：方块实体只存储物品，流程只管理候选列表</li>
 *   <li>无需NBT恢复：退出重进后自动重新初始化候选列表</li>
 *   <li>支持撤销操作：完成物品放置后仍可撤回继续</li>
 * </ul>
 *
 * @param <T> 支持此流程的方块实体类型，必须同时是 {@link BlockEntity} 和 {@link PlatableBlockEntity}
 */
public class PlatingProcess<T extends BlockEntity & PlatableBlockEntity> extends AbstractProcess<T> {
    /** 放置物品步骤的ID */
    public static final String STEP_PLACE_ITEM = "place_item";
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

    /**
     * 创建摆盘流程实例。
     */
    public PlatingProcess() {
        registerSteps();
        setInitialStep(STEP_PLACE_ITEM);
    }

    /**
     * 注册流程的所有步骤。
     */
    private void registerSteps() {
        registerStep(STEP_PLACE_ITEM, new PlaceItemStep());
        registerStep(STEP_COMPLETE, new CompleteStep());
    }

    // ==================== 步骤实现类 ====================

    /**
     * 放置物品步骤，处理配方物品的放置。
     *
     * <p>此步骤按照严格顺序执行：</p>
     * <ol>
     *   <li>防止重入</li>
     *   <li>如果候选列表未初始化，调用 initializeCandidates 初始化</li>
     *   <li>如果手持物品为空，返回 PASS（玩家无感知）</li>
     *   <li>如果手持物品不为空，根据是否已初始化选择不同逻辑</li>
     * </ol>
     */
    protected class PlaceItemStep implements Step<T> {
        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            // 防止重入逻辑
            if (isMatchingRecipes) {
                return StepResult.continueSameStep(ActionResult.PASS);
            }

            // 获取必要的变量
            PlatableBlockEntity plate = context.blockEntity();
            ItemStack heldItem = context.getHeldItemStack();
            List<Item> placedItemTypes = plate.getPlacedItemTypes();
            int currentStep = placedItemTypes.size();

            // 如果候选列表未初始化，尝试初始化
            if (!hasInitializedCandidates) {
                if (!initializeCandidates(context.world(), plate, heldItem)) {
                    // 恢复失败则步骤失败
                    resetCandidateState();
                    return StepResult.fail(STEP_PLACE_ITEM, ActionResult.FAIL);
                }

                // 恢复成功，标记已初始化
                hasInitializedCandidates = true;

                // 如果手持物品为空，步骤返回 PASS（无感知初始化）
                if (heldItem.isEmpty()) {
                    return StepResult.continueSameStep(ActionResult.PASS);
                }
            }
            // 候选列表已初始化
            else {
                // 如果玩家为空手交互，步骤直接返回 PASS
                if (heldItem.isEmpty()) {
                    return StepResult.continueSameStep(ActionResult.PASS);
                }

                // 手持物品不为空，需要过滤候选列表
                List<PlatingRecipe> matchingRecipes = filterCandidatesByNextStep(heldItem.getItem(), placedItemTypes);

                // 过滤到的列表为空时，步骤失败
                if (matchingRecipes.isEmpty()) {
                    return StepResult.fail(STEP_PLACE_ITEM, ActionResult.FAIL);
                }

                // 更新候选列表
                candidateRecipes.clear();
                candidateRecipes.addAll(matchingRecipes);
            }

            // 执行物品放置逻辑
            return executeItemPlacement(context, plate, heldItem, currentStep);
        }

        /**
         * 执行物品放置逻辑的公共部分。
         */
        private StepResult executeItemPlacement(StepExecutionContext<T> context, PlatableBlockEntity plate,
                                                ItemStack heldItem, int currentStep) {
            // 在服务器端执行物品放置
            if (context.isServerSide()) {
                // 验证是否可以在此步骤放置物品
                if (!plate.canPlaceItemAtStep(currentStep)) {
                    resetCandidateState();
                    return StepResult.fail(STEP_PLACE_ITEM, ActionResult.FAIL);
                }

                // 创建要放置的物品副本（数量为1）
                ItemStack itemToPlace = heldItem.copy();
                itemToPlace.setCount(1);

                // 尝试放置物品
                if (!plate.placeItem(currentStep, itemToPlace)) {
                    resetCandidateState();
                    return StepResult.fail(STEP_PLACE_ITEM, ActionResult.FAIL);
                }

                // 如果不是创造模式，消耗玩家物品
                if (!context.isCreateMode()) {
                    heldItem.decrement(1);
                }

                // 播放放置音效
                context.playSound(SoundEvents.BLOCK_WOOD_PLACE);
                plate.markDirty();
            }

            // 检查是否有完全匹配的配方
            checkForExactMatch(plate);

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
                return StepResult.fail(STEP_PLACE_ITEM, ActionResult.FAIL);
            }

            // 检查是否有完全匹配的配方
            if (matchedRecipe == null) {
                // 如果没有匹配的配方，但玩家手持完成物品，尝试重新检查
                checkForExactMatch(plate);
                if (matchedRecipe == null) {
                    return StepResult.fail(STEP_PLACE_ITEM, ActionResult.FAIL);
                }
            }

            // 执行完成逻辑
            plate.onPlatingComplete(context.world(), context.pos(), matchedRecipe);

            return StepResult.complete(ActionResult.SUCCESS);
        }
    }

    // ==================== 核心算法方法 ====================

    /**
     * 通用候选配方初始化方法。
     *
     * <p>此方法根据方块实体的当前状态和玩家手持物品（可能为空）来初始化候选配方列表。
     * 方法会构建一个临时列表：已放置物品列表 + 手持物品（如果不为空）。
     * 然后匹配所有容器类型匹配且步骤前缀匹配的配方。</p>
     *
     * @param world 世界实例
     * @param plate 摆盘方块实体
     * @param heldItemStack 玩家手持物品堆栈（可能为空）
     * @return 如果找到至少一个候选配方返回 {@code true}
     */
    public boolean initializeCandidates(World world, PlatableBlockEntity plate, @Nullable ItemStack heldItemStack) {
        isMatchingRecipes = true;
        try {
            // 获取配方管理器
            RecipeManager recipeManager = world.getRecipeManager();

            // 获取所有摆盘配方
            List<PlatingRecipe> allRecipes = recipeManager.listAllOfType(ModRecipeTypes.PLATING);
            if (allRecipes.isEmpty()) {
                return false;
            }

            // 获取已放置物品列表
            List<Item> placedItems = plate.getPlacedItemTypes();
            Item containerType = plate.getContainerType();

            // 构建临时匹配列表：已放置物品 + 手持物品（如果存在）
            List<Item> tempMatchingList = new ArrayList<>(placedItems);

            // 如果手持物品不为空，将其添加到临时列表末尾
            if (heldItemStack != null && !heldItemStack.isEmpty()) {
                tempMatchingList.add(heldItemStack.getItem());
            }

            // 如果临时列表为空，无法匹配任何配方
            if (tempMatchingList.isEmpty()) {
                return false;
            }

            // 匹配所有容器类型匹配且步骤前缀匹配的配方
            List<PlatingRecipe> candidates = allRecipes.stream()
                    .filter(recipe -> recipe.getContainer() == containerType)
                    .filter(recipe -> {
                        // 检查临时列表是否是配方的有效前缀
                        if (tempMatchingList.size() > recipe.getStepCount()) {
                            return false;
                        }

                        for (int i = 0; i < tempMatchingList.size(); i++) {
                            if (tempMatchingList.get(i) != recipe.getStepAt(i)) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .toList();

            if (candidates.isEmpty()) {
                return false;
            }

            // 更新候选配方列表
            candidateRecipes.clear();
            candidateRecipes.addAll(candidates);

            // 检查完全匹配（仅针对已放置物品，不包含手持物品）
            checkForExactMatch(plate);

            return true;

        } finally {
            isMatchingRecipes = false;
        }
    }

    /**
     * 根据下一步物品过滤候选配方。
     *
     * <p>此方法从当前候选配方列表中过滤出下一步与指定物品匹配的配方。
     * 仅在候选列表已初始化且手持物品不为空时调用。</p>
     *
     * @param nextItem 下一步要放置的物品类型
     * @param placedItems 已放置的物品类型列表
     * @return 过滤后的候选配方列表，只包含下一步匹配的配方
     */
    private List<PlatingRecipe> filterCandidatesByNextStep(Item nextItem, List<Item> placedItems) {
        return candidateRecipes.stream()
                .filter(recipe -> {
                    // 如果已放置物品数量 >= 配方步骤数，不是有效候选
                    if (placedItems.size() >= recipe.getStepCount()) {
                        return false;
                    }

                    // 验证已放置物品与配方前缀匹配
                    for (int i = 0; i < placedItems.size(); i++) {
                        if (placedItems.get(i) != recipe.getStepAt(i)) {
                            return false;
                        }
                    }

                    // 检查下一步是否匹配
                    return recipe.getStepAt(placedItems.size()) == nextItem;
                })
                .collect(Collectors.toList());
    }

    /**
     * 检查当前摆盘状态是否有完全匹配的配方。
     *
     * @param plate 摆盘方块实体
     */
    private void checkForExactMatch(PlatableBlockEntity plate) {
        List<Item> placedItems = plate.getPlacedItemTypes();

        matchedRecipe = candidateRecipes.stream()
                .filter(recipe -> recipe.getStepCount() == placedItems.size())
                .filter(recipe -> {
                    // 验证每个步骤都匹配
                    for (int i = 0; i < placedItems.size(); i++) {
                        if (placedItems.get(i) != recipe.getStepAt(i)) {
                            return false;
                        }
                    }
                    return true;
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * 重置候选配方状态。
     *
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
            // 如果候选列表未初始化，尝试初始化
            if (!hasInitializedCandidates) {
                if (initializeCandidates(context.world(), plate, null)) {
                    hasInitializedCandidates = true;
                }
            }

            // 检查是否有完全匹配的配方
            checkForExactMatch(plate);
            if (matchedRecipe != null) {
                // 跳转到完成步骤
                jumpToStep(STEP_COMPLETE);
            }
        }
    }

    // ==================== 生命周期方法 ====================

    @Override
    protected String getInitialStepId() {
        return STEP_PLACE_ITEM;
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
}