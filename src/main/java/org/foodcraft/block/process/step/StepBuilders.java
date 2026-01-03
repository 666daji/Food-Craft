package org.foodcraft.block.process.step;

import net.minecraft.util.ActionResult;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 步骤构建器工具类，提供创建常见类型步骤的工厂方法。
 *
 * <p>这些工厂方法简化了步骤的创建过程，使流程定义更加简洁。</p>
 */
public final class StepBuilders {

    private StepBuilders() {
        // 工具类，防止实例化
    }

    // ============ 工厂方法 ============

    /**
     * 创建简单步骤：执行后总是转移到下一步。
     *
     * <p>适用于只需要执行一次的步骤。</p>
     *
     * @param <T> 方块实体类型
     * @param executor 步骤执行逻辑
     * @param nextStepId 下一步骤ID
     * @return 步骤实例
     */
    public static <T> Step<T> simple(
            Function<StepExecutionContext<T>, ActionResult> executor,
            String nextStepId) {
        return new SimpleStep<>(executor, nextStepId);
    }

    /**
     * 创建循环步骤：执行后检查条件，满足条件才跳出循环。
     *
     * <p>适用于需要多次执行的步骤（如添加3次面粉）。
     * 每次执行后检查完成条件，如果满足则转移到下一步，
     * 否则继续执行当前步骤。</p>
     *
     * @param <T> 方块实体类型
     * @param executor 步骤执行逻辑
     * @param completionCondition 完成条件，满足时跳出循环
     * @param nextStepId 完成后的下一步骤ID
     * @return 步骤实例
     */
    public static <T> Step<T> looping(
            Function<StepExecutionContext<T>, ActionResult> executor,
            Predicate<StepExecutionContext<T>> completionCondition,
            String nextStepId) {
        return new LoopingStep<>(executor, completionCondition, nextStepId);
    }

    /**
     * 创建条件步骤：根据条件动态选择下一步。
     *
     * <p>适用于需要根据执行结果选择不同分支的步骤。</p>
     *
     * @param <T> 方块实体类型
     * @param executor 步骤执行逻辑
     * @param nextStepSelector 下一步选择器，根据执行上下文返回下一步ID
     * @return 步骤实例
     */
    public static <T> Step<T> conditional(
            Function<StepExecutionContext<T>, ActionResult> executor,
            Function<StepExecutionContext<T>, String> nextStepSelector) {
        return new ConditionalStep<>(executor, nextStepSelector);
    }

    /**
     * 创建可失败步骤：根据成功条件决定下一步或失败回退。
     *
     * <p>适用于可能失败的步骤，失败时可以回退到指定步骤。</p>
     *
     * @param <T> 方块实体类型
     * @param executor 步骤执行逻辑
     * @param successCondition 成功条件
     * @param successNextStepId 成功后的下一步ID
     * @param failureNextStepId 失败后的回退步骤ID
     * @return 步骤实例
     */
    public static <T> Step<T> fallible(
            Function<StepExecutionContext<T>, ActionResult> executor,
            Predicate<StepExecutionContext<T>> successCondition,
            String successNextStepId,
            String failureNextStepId) {
        return new FallibleStep<>(executor, successCondition, successNextStepId, failureNextStepId);
    }

    /**
     * 创建完成步骤：执行后完成整个流程。
     *
     * <p>适用于流程的最后一步。</p>
     *
     * @param <T> 方块实体类型
     * @param executor 步骤执行逻辑
     * @return 步骤实例
     */
    public static <T> Step<T> complete(
            Function<StepExecutionContext<T>, ActionResult> executor) {
        return new CompleteStep<>(executor);
    }

    /**
     * 创建限制方块实体类型的步骤。
     *
     * <p>为现有步骤添加方块实体类型限制。</p>
     *
     * @param <T> 方块实体类型
     * @param step 原始步骤
     * @param blockEntityFilter 方块实体过滤器
     * @return 包装后的步骤实例
     */
    public static <T> Step<T> withEntityFilter(
            Step<T> step,
            Predicate<T> blockEntityFilter) {
        return new FilteredStep<>(step, blockEntityFilter);
    }

    // ============ 内部实现类 ============

    /** 简单步骤实现 */
    private static class SimpleStep<T> implements Step<T> {
        private final Function<StepExecutionContext<T>, ActionResult> executor;
        private final String nextStepId;

        SimpleStep(Function<StepExecutionContext<T>, ActionResult> executor, String nextStepId) {
            this.executor = executor;
            this.nextStepId = nextStepId;
        }

        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            ActionResult result = executor.apply(context);
            return StepResult.nextStep(nextStepId, result);
        }

        @Override
        public String getDefaultNextStepId() {
            return nextStepId;
        }
    }

    /** 循环步骤实现 */
    private static class LoopingStep<T> implements Step<T> {
        private final Function<StepExecutionContext<T>, ActionResult> executor;
        private final Predicate<StepExecutionContext<T>> completionCondition;
        private final String nextStepId;

        LoopingStep(Function<StepExecutionContext<T>, ActionResult> executor,
                    Predicate<StepExecutionContext<T>> completionCondition,
                    String nextStepId) {
            this.executor = executor;
            this.completionCondition = completionCondition;
            this.nextStepId = nextStepId;
        }

        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            ActionResult result = executor.apply(context);

            // 检查是否满足完成条件
            if (completionCondition.test(context)) {
                return StepResult.nextStep(nextStepId, result);
            } else {
                return StepResult.continueSameStep(result);
            }
        }

        @Override
        public String getDefaultNextStepId() {
            return nextStepId;
        }
    }

    /** 条件步骤实现 */
    private static class ConditionalStep<T> implements Step<T> {
        private final Function<StepExecutionContext<T>, ActionResult> executor;
        private final Function<StepExecutionContext<T>, String> nextStepSelector;

        ConditionalStep(Function<StepExecutionContext<T>, ActionResult> executor,
                        Function<StepExecutionContext<T>, String> nextStepSelector) {
            this.executor = executor;
            this.nextStepSelector = nextStepSelector;
        }

        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            ActionResult result = executor.apply(context);
            String selectedNextStep = nextStepSelector.apply(context);
            return StepResult.nextStep(selectedNextStep, result);
        }
    }

    /** 可失败步骤实现 */
    private static class FallibleStep<T> implements Step<T> {
        private final Function<StepExecutionContext<T>, ActionResult> executor;
        private final Predicate<StepExecutionContext<T>> successCondition;
        private final String successNextStepId;
        private final String failureNextStepId;

        FallibleStep(Function<StepExecutionContext<T>, ActionResult> executor,
                     Predicate<StepExecutionContext<T>> successCondition,
                     String successNextStepId, String failureNextStepId) {
            this.executor = executor;
            this.successCondition = successCondition;
            this.successNextStepId = successNextStepId;
            this.failureNextStepId = failureNextStepId;
        }

        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            ActionResult result = executor.apply(context);

            if (successCondition.test(context)) {
                return StepResult.nextStep(successNextStepId, result);
            } else {
                return StepResult.fail(failureNextStepId, result);
            }
        }
    }

    /** 完成步骤实现 */
    private static class CompleteStep<T> implements Step<T> {
        private final Function<StepExecutionContext<T>, ActionResult> executor;

        CompleteStep(Function<StepExecutionContext<T>, ActionResult> executor) {
            this.executor = executor;
        }

        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            ActionResult result = executor.apply(context);
            return StepResult.complete(result);
        }
    }

    /** 过滤步骤实现 */
    private static class FilteredStep<T> implements Step<T> {
        private final Step<T> wrappedStep;
        private final Predicate<T> blockEntityFilter;

        FilteredStep(Step<T> wrappedStep, Predicate<T> blockEntityFilter) {
            this.wrappedStep = wrappedStep;
            this.blockEntityFilter = blockEntityFilter;
        }

        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            return wrappedStep.execute(context);
        }

        @Override
        public boolean canExecuteOn(T blockEntity) {
            return blockEntityFilter.test(blockEntity) && wrappedStep.canExecuteOn(blockEntity);
        }

        @Override
        public String getDefaultNextStepId() {
            return wrappedStep.getDefaultNextStepId();
        }
    }
}