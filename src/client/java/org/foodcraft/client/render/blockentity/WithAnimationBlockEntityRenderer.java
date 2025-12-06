package org.foodcraft.client.render.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.entity.animation.Animation;
import org.foodcraft.client.util.ModAnimationHelper;
import org.foodcraft.util.ModAnimationState;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * 带有动画功能的方块实体渲染器基类
 * <p>
 * 提供通用的动画管理、模型状态保存/恢复和自动状态重置功能，解决共享渲染器实例导致的动画同步问题。
 * </p>
 *
 * <h2>核心问题与解决方案</h2>
 * <p>
 * 在 Minecraft 渲染系统中，每种 {@link BlockEntityRenderer} 实现类只有一个全局实例，
 * 所有同类型的方块实体共享同一个渲染器实例。如果在渲染过程中直接修改 {@link ModelPart} 状态，
 * 会导致所有方块实体的动画状态同步，表现为"整齐划一"的动画效果。
 * </p>
 * <p>
 * 本类通过状态保存与恢复机制解决此问题：
 * <ol>
 *   <li>在初始化时保存每个模型部件的初始状态</li>
 *   <li>在渲染前重置所有模型部件到初始状态</li>
 *   <li>在渲染过程中应用动画变换</li>
 *   <li>渲染后自动恢复到初始状态</li>
 * </ol>
 * </p>
 *
 * <h2>使用步骤</h2>
 * <ol>
 *   <li>在构造函数中调用 {@link #registerModelPart} 注册需要动画的模型部件</li>
 *   <li>在 render 方法中调用 {@link #applyAnimation} 或相关动画方法应用动画,
 *   一般需要在应用动画前调用重置方法重置模型部件至初始状态。</li>
 *   <li>按常规方式渲染模型部件</li>
 * </ol>
 *
 * <h2>动画方法说明</h2>
 * <ul>
 *   <li>{@link #applyAnimation} - 自动重置并应用动画，适用于大多数情况</li>
 *   <li>{@link #updateAnimation} - 仅在动画运行时应用变换，动画停止时不应用</li>
 *   <li>{@link #alwaysUpdateAnimation} - 无论动画是否运行都会应用变换</li>
 *   <li>{@link #animate} - 应用动画的初始状态（时间点为0）</li>
 *   <li>{@link #animateMovement} - 基于肢体运动参数应用动画</li>
 * </ul>
 *
 * <h2>重置模型部件方法说明</h2>
 * <ul>
 *     <li>{@link #resetPart(String, ModelPart)}重置指定模型部件到初始状态</li>
 *     <li>{@link #resetAllModelParts()}重置所有已注册模型部件到初始状态</li>
 * </ul>
 *
 * @param <T> 方块实体类型
 */
public abstract class WithAnimationBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    /** 动画计算使用的临时向量，避免重复创建对象 */
    protected static final Vector3f ANIMATION_VEC = new Vector3f();

    /** 模型部件映射表，键为骨骼名称，值为对应的 ModelPart 实例 */
    protected final Map<String, ModelPart> modelParts = new HashMap<>();

    /** 模型部件初始状态存储，用于在动画前后保存和恢复模型状态 */
    protected final Map<String, PartState> initialPartStates = new HashMap<>();

    /**
     * 模型部件状态存储类
     * <p>
     * 用于保存和恢复模型部件的完整变换状态（位置、旋转和缩放）。
     * </p>
     */
    protected static class PartState {

        /** 模型部件的枢轴点坐标 */
        public float pivotX, pivotY, pivotZ;

        /** 模型部件的旋转角度 */
        public float pitch, yaw, roll;

        /** 模型部件的缩放比例 */
        public float xScale, yScale, zScale;

        /**
         * 从模型部件创建状态快照
         *
         * @param part 要保存状态的模型部件
         */
        public PartState(ModelPart part) {
            this.pivotX = part.pivotX;
            this.pivotY = part.pivotY;
            this.pivotZ = part.pivotZ;
            this.pitch = part.pitch;
            this.yaw = part.yaw;
            this.roll = part.roll;
            this.xScale = part.xScale;
            this.yScale = part.yScale;
            this.zScale = part.zScale;
        }

        /**
         * 将保存的状态应用到模型部件
         *
         * @param part 要恢复状态的模型部件
         */
        public void applyTo(ModelPart part) {
            part.setPivot(pivotX, pivotY, pivotZ);
            part.setAngles(pitch, yaw, roll);
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
        }
    }

    // 模型部件管理方法

    /**
     * 注册模型部件并保存其初始状态
     * <p>
     * 应在构造函数中调用此方法注册所有需要动画的模型部件。
     * </p>
     *
     * @param name 模型部件标识符
     * @param part 要注册的模型部件实例
     */
    protected void registerModelPart(String name, ModelPart part) {
        modelParts.put(name, part);
        initialPartStates.put(name, new PartState(part));
    }

    /**
     * 重置指定模型部件到初始状态
     *
     * @param name 模型部件标识符
     * @param part 要重置的模型部件
     */
    protected void resetPart(String name, ModelPart part) {
        PartState state = initialPartStates.get(name);
        if (state != null) {
            state.applyTo(part);
        }
    }

    /**
     * 重置所有已注册模型部件到初始状态
     * <p>
     * 应在渲染开始前调用此方法，确保每个方块实体从相同的初始状态开始动画。
     * </p>
     */
    protected void resetAllModelParts() {
        for (Map.Entry<String, ModelPart> entry : modelParts.entrySet()) {
            resetPart(entry.getKey(), entry.getValue());
        }
    }

    // 动画进度计算方法

    /**
     * 计算动画进度
     * <p>
     * 默认实现返回实体年龄 + 部分时间，子类可重写此方法以提供自定义进度计算。
     * </p>
     *
     * @param age       实体年龄（tick数）
     * @param tickDelta 部分时间（0.0-1.0）
     * @return 动画进度
     */
    protected float getAnimationProgress(float age, float tickDelta) {
        return age + tickDelta;
    }

    // 动画应用方法

    /**
     * 应用动画（推荐使用）
     * <p>
     * 自动重置模型部件并应用动画，简化了动画应用流程。
     * </p>
     *
     * @param animationState    动画状态对象
     * @param animation         要应用的动画
     * @param animationProgress 动画进度
     * @param speedMultiplier   动画速度乘数
     * @param scale             动画缩放比例
     */
    protected void applyAnimation(ModAnimationState animationState, Animation animation,
                                  float animationProgress, float speedMultiplier, float scale) {
        resetAllModelParts();
        alwaysUpdateAnimation(animationState, animation, animationProgress, speedMultiplier, scale);
    }

    /**
     * 更新动画状态并应用动画变换
     * <p>
     * 仅在动画运行时应用变换，动画停止时不应用任何变换。
     * 适用于需要根据动画状态控制是否应用变换的情况。
     * </p>
     *
     * @param animationState    动画状态对象
     * @param animation         要应用的动画
     * @param animationProgress 动画进度
     * @param speedMultiplier   动画速度乘数
     * @param scale             动画缩放比例
     */
    protected void updateAnimation(ModAnimationState animationState, Animation animation,
                                   float animationProgress, float speedMultiplier, float scale) {
        animationState.update(
                animationProgress,
                speedMultiplier,
                animation.looping(),
                animation.lengthInSeconds()
        );

        animationState.run(state -> ModAnimationHelper.animate(
                modelParts,
                animation,
                state.getTimeRunning(),
                scale,
                ANIMATION_VEC
        ));
    }

    /**
     * 始终更新动画状态并应用动画变换
     * <p>
     * 无论动画是否运行都会应用变换，适用于需要持续动画的情况。
     * </p>
     *
     * @param animationState    动画状态对象
     * @param animation         要应用的动画
     * @param animationProgress 动画进度
     * @param speedMultiplier   动画速度乘数
     * @param scale             动画缩放比例
     */
    protected void alwaysUpdateAnimation(ModAnimationState animationState, Animation animation,
                                         float animationProgress, float speedMultiplier, float scale) {
        animationState.update(
                animationProgress,
                speedMultiplier,
                animation.looping(),
                animation.lengthInSeconds()
        );

        ModAnimationHelper.animate(
                modelParts,
                animation,
                animationState.getTimeRunning(),
                scale,
                ANIMATION_VEC
        );
    }

    /**
     * 应用动画的初始状态
     * <p>
     * 适用于不需要时间进度的静态姿势，或在特定时间点显示动画状态。
     * </p>
     *
     * @param animation 要应用的动画
     */
    protected void animate(Animation animation) {
        ModAnimationHelper.animate(modelParts, animation, 0L, 1.0F, ANIMATION_VEC);
    }

    /**
     * 基于肢体运动参数应用动画
     * <p>
     * 适用于与实体运动相关的动画（如行走、奔跑等），
     * 基于肢体角度和距离计算动画进度。
     * </p>
     *
     * @param animation          要应用的动画
     * @param limbAngle          肢体角度
     * @param limbDistance       肢体距离
     * @param limbAngleScale     肢体角度缩放比例
     * @param limbDistanceScale  肢体距离缩放比例
     */
    protected void animateMovement(Animation animation, float limbAngle, float limbDistance,
                                   float limbAngleScale, float limbDistanceScale) {
        long l = (long)(limbAngle * 50.0F * limbAngleScale);
        float f = Math.min(limbDistance * limbDistanceScale, 1.0F);
        ModAnimationHelper.animate(modelParts, animation, l, f, ANIMATION_VEC);
    }
}