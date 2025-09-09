package org.foodcraft.client.util;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/**
 * 扩展的动画助手类，提供不依赖于 SinglePartEntityModel 的动画方法
 */
public class ModAnimationHelper {

    /**
     * 对模型部件映射应用动画
     *
     * @param modelParts 模型部件映射，键为骨骼名称，值为对应的 ModelPart
     * @param animation 要应用的动画
     * @param runningTime 动画运行时间（毫秒）
     * @param scale 动画缩放比例
     * @param tempVec 临时向量，用于计算中间值
     */
    public static void animate(Map<String, ModelPart> modelParts, Animation animation, long runningTime, float scale, Vector3f tempVec) {
        float f = getRunningSeconds(animation, runningTime);

        for (var entry : animation.boneAnimations().entrySet()) {
            String boneName = entry.getKey();
            ModelPart part = modelParts.get(boneName);

            if (part != null) {
                List<Transformation> transformations = entry.getValue();
                for (Transformation transformation : transformations) {
                    applyTransformation(part, transformation, f, scale, tempVec);
                }
            }
        }
    }

    /**
     * 对模型部件映射应用动画，支持部分时间
     *
     * @param modelParts 模型部件映射，键为骨骼名称，值为对应的 ModelPart
     * @param animation 要应用的动画
     * @param runningTime 动画运行时间（毫秒）
     * @param partialTime 部分时间（毫秒）
     * @param scale 动画缩放比例
     * @param tempVec 临时向量，用于计算中间值
     */
    public static void animateWithPartial(Map<String, ModelPart> modelParts, Animation animation, long runningTime, float partialTime, float scale, Vector3f tempVec) {
        // 对于循环动画，确保时间在动画长度范围内
        long effectiveTime = runningTime;
        if (animation.looping() && animation.lengthInSeconds() > 0) {
            long animationLengthMs = (long) (animation.lengthInSeconds() * 1000);
            effectiveTime = runningTime % animationLengthMs;
        }

        float totalTime = (effectiveTime + partialTime) / 1000.0F;
        float f = animation.looping() ? totalTime % animation.lengthInSeconds() : totalTime;

        for (var entry : animation.boneAnimations().entrySet()) {
            String boneName = entry.getKey();
            ModelPart part = modelParts.get(boneName);

            if (part != null) {
                List<Transformation> transformations = entry.getValue();
                for (Transformation transformation : transformations) {
                    applyTransformation(part, transformation, f, scale, tempVec);
                }
            }
        }
    }

    /**
     * 对单个模型部件应用变换
     *
     * @param part 模型部件
     * @param transformation 变换
     * @param time 当前时间（秒）
     * @param scale 缩放比例
     * @param tempVec 临时向量
     */
    private static void applyTransformation(ModelPart part, Transformation transformation, float time, float scale, Vector3f tempVec) {
        Keyframe[] keyframes = transformation.keyframes();
        int i = Math.max(0, MathHelper.binarySearch(0, keyframes.length, index -> time <= keyframes[index].timestamp()) - 1);
        int j = Math.min(keyframes.length - 1, i + 1);
        Keyframe keyframe = keyframes[i];
        Keyframe keyframe2 = keyframes[j];
        float h = time - keyframe.timestamp();
        float k;

        if (j != i) {
            k = MathHelper.clamp(h / (keyframe2.timestamp() - keyframe.timestamp()), 0.0F, 1.0F);
        } else {
            k = 0.0F;
        }

        keyframe2.interpolation().apply(tempVec, k, keyframes, i, j, scale);
        transformation.target().apply(part, tempVec);
    }

    /**
     * 计算动画运行时间（秒）
     *
     * @param animation 动画
     * @param runningTime 运行时间（毫秒）
     * @return 运行时间（秒）
     */
    private static float getRunningSeconds(Animation animation, long runningTime) {
        // 对于循环动画，确保时间在动画长度范围内
        long effectiveTime = runningTime;
        if (animation.looping() && animation.lengthInSeconds() > 0) {
            long animationLengthMs = (long) (animation.lengthInSeconds() * 1000);
            effectiveTime = runningTime % animationLengthMs;
        }

        float f = (float) effectiveTime / 1000.0F;
        return animation.looping() ? f % animation.lengthInSeconds() : f;
    }

    /**
     * 创建平移向量
     *
     * @param x X轴平移
     * @param y Y轴平移
     * @param z Z轴平移
     * @return 平移向量
     */
    public static Vector3f createTranslationalVector(float x, float y, float z) {
        return new Vector3f(x, -y, z);
    }

    /**
     * 创建旋转向量（角度转换为弧度）
     *
     * @param x X轴旋转角度
     * @param y Y轴旋转角度
     * @param z Z轴旋转角度
     * @return 旋转向量（弧度）
     */
    public static Vector3f createRotationalVector(float x, float y, float z) {
        return new Vector3f(
                x * (float) (Math.PI / 180.0),
                y * (float) (Math.PI / 180.0),
                z * (float) (Math.PI / 180.0)
        );
    }

    /**
     * 创建缩放向量
     *
     * @param x X轴缩放
     * @param y Y轴缩放
     * @param z Z轴缩放
     * @return 缩放向量
     */
    public static Vector3f createScalingVector(double x, double y, double z) {
        return new Vector3f((float)(x - 1.0), (float)(y - 1.0), (float)(z - 1.0));
    }
}