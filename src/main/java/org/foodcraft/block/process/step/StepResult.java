package org.foodcraft.block.process.step;

import net.minecraft.util.ActionResult;

/**
 * 步骤执行结果，包含步骤执行后的交互结果和下一步指示。
 *
 * <p>每个步骤执行后必须返回一个{@code StepResult}对象，指示：</p>
 * <ul>
 *   <li>玩家交互的结果（成功、失败等）</li>
 *   <li>下一步应该做什么（步骤转移类型）</li>
 *   <li>如果需要转移，目标步骤是什么</li>
 * </ul>
 *
 * <p>使用工厂方法创建不同类型的步骤结果：</p>
 * <pre>{@code
 * // 继续执行当前步骤（用于循环）
 * StepResult.continueSameStep(ActionResult.SUCCESS);
 *
 * // 执行下一步
 * StepResult.nextStep("next_step_id", ActionResult.SUCCESS);
 *
 * // 完成整个流程
 * StepResult.complete(ActionResult.SUCCESS);
 *
 * // 步骤失败，回退到指定步骤
 * StepResult.fail("fallback_step_id", ActionResult.FAIL);
 *
 * // 重置整个流程
 * StepResult.reset(ActionResult.PASS);
 * }</pre>
 */
public class StepResult {

    /** 步骤结果类型枚举 */
    public enum Type {
        /** 继续执行当前步骤（用于需要多次执行的步骤） */
        CONTINUE_SAME_STEP,

        /** 执行下一个步骤 */
        NEXT_STEP,

        /** 完成整个流程 */
        COMPLETE,

        /** 步骤执行失败 */
        FAIL,

        /** 重置整个流程 */
        RESET
    }

    // ============ 私有字段 ============

    private final ActionResult actionResult;
    private final Type type;
    private final String nextStepId;
    private final String fallbackStepId;

    // ============ 私有构造函数 ============

    /**
     * 私有构造函数，使用工厂方法创建实例。
     *
     * @param actionResult 玩家交互结果
     * @param type 步骤结果类型
     * @param nextStepId 下一步骤ID（仅{@link Type#NEXT_STEP}类型需要）
     * @param fallbackStepId 回退步骤ID（仅{@link Type#FAIL}类型需要）
     */
    private StepResult(ActionResult actionResult, Type type,
                       String nextStepId, String fallbackStepId) {
        this.actionResult = actionResult;
        this.type = type;
        this.nextStepId = nextStepId;
        this.fallbackStepId = fallbackStepId;
    }

    // ============ 工厂方法 ============

    /**
     * 创建"继续执行当前步骤"的结果。
     *
     * <p>此结果类型用于需要多次执行的步骤（如添加3次面粉）。
     * 流程将继续执行当前步骤，直到满足跳出条件。</p>
     *
     * @param actionResult 玩家交互结果
     * @return 步骤结果实例
     */
    public static StepResult continueSameStep(ActionResult actionResult) {
        return new StepResult(actionResult, Type.CONTINUE_SAME_STEP, null, null);
    }

    /**
     * 创建"执行下一步骤"的结果。
     *
     * <p>此结果类型指示流程转移到下一步骤。必须提供下一步骤的ID。</p>
     *
     * @param nextStepId 下一步骤的ID
     * @param actionResult 玩家交互结果
     * @return 步骤结果实例
     */
    public static StepResult nextStep(String nextStepId, ActionResult actionResult) {
        return new StepResult(actionResult, Type.NEXT_STEP, nextStepId, null);
    }

    /**
     * 创建"完成整个流程"的结果。
     *
     * <p>此结果类型指示流程已成功完成。流程将重置为初始状态。</p>
     *
     * @param actionResult 玩家交互结果
     * @return 步骤结果实例
     */
    public static StepResult complete(ActionResult actionResult) {
        return new StepResult(actionResult, Type.COMPLETE, null, null);
    }

    /**
     * 创建"步骤执行失败"的结果。
     *
     * <p>此结果类型指示步骤执行失败。流程可以回退到指定步骤继续执行，
     * 或者重置流程。如果提供回退步骤ID，流程将跳转到该步骤继续执行；
     * 否则流程将重置。</p>
     *
     * @param fallbackStepId 回退步骤ID（可选的）
     * @param actionResult 玩家交互结果
     * @return 步骤结果实例
     */
    public static StepResult fail(String fallbackStepId, ActionResult actionResult) {
        return new StepResult(actionResult, Type.FAIL, null, fallbackStepId);
    }

    /**
     * 创建"重置整个流程"的结果。
     *
     * <p>此结果类型指示流程应该重置到初始状态。所有状态数据将被清除，
     * 当前步骤将重置为初始步骤。</p>
     *
     * @param actionResult 玩家交互结果
     * @return 步骤结果实例
     */
    public static StepResult reset(ActionResult actionResult) {
        return new StepResult(actionResult, Type.RESET, null, null);
    }

    // ============ 访问器方法 ============

    /**
     * 获取玩家交互结果。
     *
     * @return 玩家交互结果
     */
    public ActionResult getActionResult() {
        return actionResult;
    }

    /**
     * 获取步骤结果类型。
     *
     * @return 步骤结果类型
     */
    public Type getType() {
        return type;
    }

    /**
     * 获取下一步骤ID。
     *
     * @return 下一步骤ID，仅{@link Type#NEXT_STEP}类型有效
     */
    public String getNextStepId() {
        return nextStepId;
    }

    /**
     * 获取回退步骤ID。
     *
     * @return 回退步骤ID，仅{@link Type#FAIL}类型有效
     */
    public String getFallbackStepId() {
        return fallbackStepId;
    }
}