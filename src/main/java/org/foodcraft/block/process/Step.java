package org.foodcraft.block.process;

/**
 * 步骤接口，定义流程中的单个交互步骤。
 *
 * <p>每个步骤代表一次玩家与方块的交互。步骤执行后返回一个{@link StepResult}，
 * 指示下一步应该做什么（继续当前步骤、进入下一步、完成流程等）。</p>
 *
 * <p>步骤可以限制可执行的方块实体类型，通过实现{@link #canExecuteOn}方法。</p>
 *
 * @param <T> 步骤支持的方块实体类型
 *
 * @see StepResult
 * @see StepExecutionContext
 */
public interface Step<T> {

    /**
     * 执行步骤。
     *
     * <p>此方法包含步骤的核心逻辑，如检查玩家手持物品、消耗物品、
     * 更新方块状态、播放音效等。</p>
     *
     * <p>步骤执行后必须返回一个{@link StepResult}，指示下一步动作：</p>
     * <ul>
     *   <li>{@link StepResult.Type#CONTINUE_SAME_STEP}: 继续执行当前步骤（循环结构）</li>
     *   <li>{@link StepResult.Type#NEXT_STEP}: 执行下一步骤</li>
     *   <li>{@link StepResult.Type#COMPLETE}: 完成整个流程</li>
     *   <li>{@link StepResult.Type#FAIL}: 步骤执行失败</li>
     *   <li>{@link StepResult.Type#RESET}: 重置整个流程</li>
     * </ul>
     *
     * @param context 步骤执行上下文，包含所有执行步骤所需的信息
     * @return 步骤执行结果，指示下一步动作
     */
    StepResult execute(StepExecutionContext<T> context);

    /**
     * 检查此步骤是否可以在给定的方块实体上执行。
     *
     * <p>默认实现返回true，表示可以在任何类型的方块实体上执行。
     * 子类可以重写此方法以限制步骤可执行的方块实体类型。</p>
     *
     * @param blockEntity 要检查的方块实体
     * @return 如果步骤可以在该方块实体上执行，则返回true
     */
    default boolean canExecuteOn(T blockEntity) {
        return true;
    }

    /**
     * 获取步骤的默认下一步ID。
     *
     * <p>当步骤返回{@link StepResult.Type#NEXT_STEP}但未指定下一步ID时，
     * 将使用此方法返回的ID。默认返回null，表示没有默认下一步。</p>
     *
     * @return 默认下一步ID，或null表示没有默认下一步
     */
    default String getDefaultNextStepId() {
        return null;
    }
}