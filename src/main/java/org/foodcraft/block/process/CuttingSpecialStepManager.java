package org.foodcraft.block.process;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.foodcraft.registry.ModItems;

import java.util.HashMap;
import java.util.Map;

/**
 * 切菜特殊步骤管理器
 *
 * <p>管理所有需要特殊处理的切割步骤，支持动态注册和查询。</p>
 * <p>职责：</p>
 * <ul>
 *   <li>存储特殊步骤的触发条件（物品 + 切割次数）</li>
 *   <li>提供统一的注册接口</li>
 *   <li>快速查询当前切割是否需要特殊步骤</li>
 * </ul>
 */
public class CuttingSpecialStepManager {
    // 特殊步骤标识
    public static final String STEP_CARROT_12 = "cut_carrot_12";
    public static final String STEP_COD_8 = "cut_cod_8";
    public static final String STEP_COD_9 = "cut_cod_9";
    public static final String STEP_COOKED_COD_8 = "cut_cooked_cod_8";
    public static final String STEP_COOKED_COD_9 = "cut_cooked_cod_9";
    public static final String STEP_SALMON_6 = "cut_salmon_6";
    public static final String STEP_SALMON_7 = "cut_salmon_7";
    public static final String STEP_COOKED_SALMON_6 = "cut_cooked_salmon_6";
    public static final String STEP_COOKED_SALMON_7 = "cut_cooked_salmon_7";

    /**
     * 特殊步骤配置记录
     */
    public static record SpecialStepConfig(
            int cutNumber,           // 触发切割次数（0-based）
            String stepId,           // 步骤ID
            ItemStack[] outputItems, // 要给予的物品
            boolean requireTool      // 是否需要工具
    ) {}

    /**
     * 特殊步骤触发映射：物品 -> 切割次数 -> 配置
     */
    private final Map<Item, Map<Integer, SpecialStepConfig>> stepTriggers = new HashMap<>();

    /**
     * 步骤ID到物品的映射（用于反向查找）
     */
    private final Map<String, Item> stepIdToItem = new HashMap<>();

    // ============ 单例模式 ============

    private static final CuttingSpecialStepManager INSTANCE = new CuttingSpecialStepManager();

    private CuttingSpecialStepManager() {
        registerDefaultSteps();
    }

    public static CuttingSpecialStepManager getInstance() {
        return INSTANCE;
    }

    // ============ 注册方法 ============

    /**
     * 注册一个特殊切割步骤
     *
     * @param item   触发物品
     * @param config 步骤配置
     */
    public void registerSpecialStep(Item item, SpecialStepConfig config) {
        if (item == null || config == null || config.stepId == null) {
            return;
        }

        // 注册到映射
        stepTriggers
                .computeIfAbsent(item, k -> new HashMap<>())
                .put(config.cutNumber, config);

        stepIdToItem.put(config.stepId, item);
    }

    /**
     * 批量注册特殊切割步骤
     *
     * @param item 触发物品
     * @param configs 步骤配置数组
     */
    public void registerSpecialSteps(Item item, SpecialStepConfig... configs) {
        for (SpecialStepConfig config : configs) {
            registerSpecialStep(item, config);
        }
    }

    // ============ 查询方法 ============

    /**
     * 检查给定物品和切割次数是否需要特殊步骤
     *
     * @param item 物品
     * @param cutNumber 切割次数（0-based）
     * @return 特殊步骤配置，如果没有则返回null
     */
    public SpecialStepConfig getSpecialStep(Item item, int cutNumber) {
        if (item == null) return null;

        Map<Integer, SpecialStepConfig> itemTriggers = stepTriggers.get(item);
        if (itemTriggers == null) return null;

        return itemTriggers.get(cutNumber);
    }

    /**
     * 获取步骤ID对应的触发物品
     *
     * @param stepId 步骤ID
     * @return 触发物品
     */
    public Item getItemForStep(String stepId) {
        return stepIdToItem.get(stepId);
    }

    /**
     * 检查物品是否有特殊步骤
     *
     * @param item 物品
     * @return 是否有特殊步骤
     */
    public boolean hasSpecialSteps(Item item) {
        return item != null && stepTriggers.containsKey(item);
    }

    /**
     * 获取物品的所有特殊步骤配置
     *
     * @param item 物品
     * @return 特殊步骤配置映射（切割次数 -> 配置）
     */
    public Map<Integer, SpecialStepConfig> getSpecialStepsForItem(Item item) {
        return stepTriggers.getOrDefault(item, Map.of());
    }

    /**
     * 获取所有已注册的物品
     *
     * @return 物品集合
     */
    public Iterable<Item> getAllRegisteredItems() {
        return stepTriggers.keySet();
    }

    // ============ 默认配置 ============

    /**
     * 注册默认的特殊步骤配置
     */
    private void registerDefaultSteps() {
        // 胡萝卜的特殊步骤
        registerSpecialSteps(
                Items.CARROT,
                new SpecialStepConfig(9, CuttingProcess.STEP_EMPTY,
                        new ItemStack[0], false),
                new SpecialStepConfig(10, CuttingProcess.STEP_EMPTY,
                        new ItemStack[0], false),
                new SpecialStepConfig(11, STEP_CARROT_12,
                        new ItemStack[]{
                                new ItemStack(ModItems.CARROT_SLICES, 1),
                                new ItemStack(ModItems.CARROT_HEAD, 1)
                        }, false)
        );

        // 苹果的特殊步骤
        registerSpecialSteps(
                Items.APPLE,
                new SpecialStepConfig(5, CuttingProcess.STEP_EMPTY,
                        new ItemStack[0], false)
        );

        // 鳕鱼的特殊步骤
        registerSpecialSteps(
                Items.COD,
                new SpecialStepConfig(6, CuttingProcess.STEP_EMPTY,
                        new ItemStack[0], false),
                new SpecialStepConfig(7, STEP_COD_8,
                        new ItemStack[]{new ItemStack(ModItems.COD_CUBES, 1)}, false),
                new SpecialStepConfig(8, STEP_COD_9,
                        new ItemStack[]{new ItemStack(ModItems.COD_CUBES, 1)}, false)
        );

        // 烤鳕鱼的特殊步骤
        registerSpecialSteps(
                Items.COOKED_COD,
                new SpecialStepConfig(6, CuttingProcess.STEP_EMPTY,
                        new ItemStack[0], false),
                new SpecialStepConfig(7, STEP_COOKED_COD_8,
                        new ItemStack[]{new ItemStack(ModItems.COOKED_COD_CUBES, 1)}, false),
                new SpecialStepConfig(8, STEP_COOKED_COD_9,
                        new ItemStack[]{new ItemStack(ModItems.COOKED_COD_CUBES, 1)}, false)
        );

        // 三文鱼的特殊步骤
        registerSpecialSteps(
                Items.SALMON,
                new SpecialStepConfig(5, STEP_SALMON_6,
                        new ItemStack[]{new ItemStack(ModItems.SALMON_CUBES, 1)}, false),
                new SpecialStepConfig(6, STEP_SALMON_7,
                        new ItemStack[]{new ItemStack(ModItems.SALMON_CUBES, 1)}, false)
        );

        // 烤三文鱼的特殊步骤
        registerSpecialSteps(
                Items.COOKED_SALMON,
                new SpecialStepConfig(5, STEP_COOKED_SALMON_6,
                        new ItemStack[]{new ItemStack(ModItems.COOKED_SALMON_CUBES, 1)}, false),
                new SpecialStepConfig(6, STEP_COOKED_SALMON_7,
                        new ItemStack[]{new ItemStack(ModItems.COOKED_SALMON_CUBES, 1)}, false)
        );
    }

    // ============ 工具方法 ============

    /**
     * 清除所有注册的特殊步骤
     */
    public void clearAll() {
        stepTriggers.clear();
        stepIdToItem.clear();
    }

    /**
     * 获取注册的特殊步骤数量
     *
     * @return 特殊步骤数量
     */
    public int getStepCount() {
        return stepIdToItem.size();
    }
}