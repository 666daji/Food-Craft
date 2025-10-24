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
 * 提供通用的动画管理和模型状态保存/恢复功能
 * <h2>关于 {@link BlockEntityRenderer} 的说明</h2>
 * <p>
 * 在 Minecraft 的渲染系统中，为了优化内存使用和性能，每种类型的 {@link BlockEntityRenderer} 实现类
 * 在全局范围内只会创建一个实例。这意味着所有同类型的方块实体将共享同一个渲染器实例。
 * </p>
 * <p>
 * 这种设计带来了一个重要的因素：如果在渲染过程中直接修改模型部件（ModelPart）的状态，
 * 由于所有方块实体共享同一个模型实例，会导致所有方块实体的动画状态同步，表现为"整齐划一"的动画效果。
 * </p>
 * <p>
 * 为了解决这个问题，可以使用本类的状态保存与恢复机制：
 * <ul>
 *   <li>在渲染开始时创建新的{@link PartState}实例保存模型部件的初始状态</li>
 *   <li>在渲染过程中应用动画变换</li>
 *   <li>在渲染结束时调用{@link PartState#applyTo}恢复模型部件的初始状态</li>
 * </ul>
 * 这种方法确保了每个方块实体的动画都是独立的，不会相互影响。
 * </p>
 *
 * <p>对于需要应用动画的{@link ModelPart},请在构造函数中向{@link #modelParts}加入对应的映射</p>
 * <p>对于需要重置的{@link ModelPart}，请在构造函数中调用{@link #saveInitialState(String, ModelPart)}保存初始状态</p>
 *
 * @param <T> 方块实体类型
 */
public abstract class WithAnimationBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    /** 动画计算使用的临时向量，避免重复创建对象 */
    protected static final Vector3f ANIMATION_VEC = new Vector3f();

    /** 模型部件映射表，键为骨骼名称，值为对应的ModelPart实例 */
    protected final Map<String, ModelPart> modelParts = new HashMap<>();

    /** 模型部件初始状态存储，用于在动画前后保存和恢复模型状态 */
    protected final Map<String, PartState> initialPartStates = new HashMap<>();

    /**
     * 模型部件状态存储类
     * 用于保存和恢复模型部件的变换状态（位置和旋转）
     */
    protected static class PartState {
        /** 模型部件的枢轴点坐标 */
        public float pivotX, pivotY, pivotZ;

        /** 模型部件的旋转角度 */
        public float pitch, yaw, roll;

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
        }

        /**
         * 将保存的状态应用到模型部件
         *
         * @param part 要恢复状态的模型部件
         */
        public void applyTo(ModelPart part) {
            part.setPivot(pivotX, pivotY, pivotZ);
            part.setAngles(pitch, yaw, roll);
        }
    }

    /**
     * 保存模型部件的初始状态
     * 应在渲染器初始化时调用此方法保存所有需要动画的模型部件的初始状态
     *
     * @param name 模型部件名称（标识符）
     * @param part 要保存状态的模型部件
     */
    protected void saveInitialState(String name, ModelPart part) {
        initialPartStates.put(name, new PartState(part));
    }

    /**
     * 重置模型部件到初始状态
     * 在应用动画前调用此方法，确保模型部件从初始状态开始变换
     *
     * @param name 模型部件名称（标识符）
     * @param part 要重置状态的模型部件
     */
    protected void resetPart(String name, ModelPart part) {
        PartState state = initialPartStates.get(name);
        if (state != null) {
            state.applyTo(part);
        }
    }

    protected float getAnimationProgress(float age, float tickDelta) {
        return age + tickDelta;
    }

    /**
     * 更新动画状态并应用动画变换
     * 仅在动画运行时应用变换，动画停止时不应用任何变换
     *
     * @param animationState 动画状态对象
     * @param animation 要应用的动画
     * @param animationProgress 动画进度（通常为实体年龄 + 部分时间）
     * @param speedMultiplier 动画速度乘数
     * @param scale 动画缩放比例
     */
    protected void updateAnimation(ModAnimationState animationState, Animation animation, float animationProgress, float speedMultiplier, float scale) {
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
     * 无论动画是否运行都会应用变换，适用于需要持续动画的情况
     *
     * @param animationState 动画状态对象
     * @param animation 要应用的动画
     * @param animationProgress 动画进度（通常为实体年龄 + 部分时间）
     * @param speedMultiplier 动画速度乘数
     * @param scale 动画缩放比例
     */
    protected void alwaysUpdateAnimation(ModAnimationState animationState, Animation animation, float animationProgress, float speedMultiplier, float scale) {
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
     * 应用动画的初始状态（时间点为0）
     * 适用于不需要时间进度的静态姿势
     *
     * @param animation 要应用的动画
     */
    protected void animate(Animation animation) {
        ModAnimationHelper.animate(modelParts, animation, 0L, 1.0F, ANIMATION_VEC);
    }

    /**
     * 基于肢体运动参数应用动画
     * 适用于与实体运动相关的动画（如行走、奔跑等）
     *
     * @param animation 要应用的动画
     * @param limbAngle 肢体角度（通常为实体的limbAngle）
     * @param limbDistance 肢体距离（通常为实体的limbDistance）
     * @param limbAngleScale 肢体角度缩放比例
     * @param limbDistanceScale 肢体距离缩放比例
     */
    protected void animateMovement(Animation animation, float limbAngle, float limbDistance, float limbAngleScale, float limbDistanceScale) {
        long l = (long)(limbAngle * 50.0F * limbAngleScale);
        float f = Math.min(limbDistance * limbDistanceScale, 1.0F);
        ModAnimationHelper.animate(modelParts, animation, l, f, ANIMATION_VEC);
    }
}