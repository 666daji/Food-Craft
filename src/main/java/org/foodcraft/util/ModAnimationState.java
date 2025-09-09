package org.foodcraft.util;

import java.util.function.Consumer;
import net.minecraft.util.math.MathHelper;

/**
 * 自定义动画状态管理类
 * 用于跟踪和控制动画的播放状态、时间进度和循环行为
 *
 * <p>注意：此类设计为可在服务端和客户端同时使用，但需要特别注意数据同步问题。 </p>
 * <p>* 不正确或未同步的动画状态数据可能导致不同客户端观察到不一致的动画表现。</p>
 *
 */
public class ModAnimationState {
    // 停止状态的特殊标记值
    private static final long STOPPED = Long.MAX_VALUE;

    // 上次更新时间（毫秒）
    public long updatedAt = Long.MAX_VALUE;

    // 动画已运行时间（毫秒）
    public long timeRunning;

    // 部分时间，用于确保动画流畅渲染
    public float partialTime;

    // 是否循环播放动画
    private boolean looping = false;

    // 动画长度（秒）
    private float animationLength = 0;

    // 上次记录的运行时间，用于确保动画进度不会回退
    private long lastTimeRunning = 0;

    /**
     * 强制启动动画（无论是否已在运行）
     * 会重置运行时间并启动动画
     *
     * @param age 实体年龄（tick数）
     */
    public void FStart(int age) {
        resetRunningTime();
        start(age);
    }

    /**
     * 启动动画
     *
     * @param age 实体年龄（tick数）
     */
    public void start(int age) {
        this.updatedAt = (long) (age * 1000.0F / 20.0F);
        this.partialTime = 0.0F;
        this.lastTimeRunning = timeRunning;
    }

    /**
     * 如果动画未运行，则启动动画
     *
     * @param age 实体年龄（tick数）
     */
    public void startIfNotRunning(int age) {
        if (!this.isRunning()) {
            this.start(age);
        }
    }

    /**
     * 设置动画运行状态
     *
     * @param running 是否运行
     * @param age 实体年龄（tick数）
     */
    public void setRunning(boolean running, int age) {
        if (running) {
            this.startIfNotRunning(age);
        } else {
            this.stop();
        }
    }

    /**
     * 停止动画
     */
    public void stop() {
        this.updatedAt = STOPPED;
        this.partialTime = 0.0F;
    }

    /**
     * 如果动画正在运行，则执行指定操作
     *
     * @param consumer 要执行的操作
     */
    public void run(Consumer<ModAnimationState> consumer) {
        if (this.isRunning()) {
            consumer.accept(this);
        }
    }

    /**
     * 更新动画状态
     *
     * @param animationProgress 动画进度（0.0-1.0）
     * @param speedMultiplier 速度乘数
     * @param looping 是否循环播放
     * @param animationLength 动画长度（秒）
     */
    public void update(float animationProgress, float speedMultiplier, boolean looping, float animationLength) {
        this.looping = looping;
        this.animationLength = animationLength;

        if (this.isRunning()) {
            // 计算当前时间和部分时间
            long currentTime = MathHelper.lfloor(animationProgress * 1000.0F / 20.0F);
            float partial = MathHelper.lfloor(animationProgress * 1000.0F / 20.0F) - currentTime;

            // 计算时间增量并更新运行时间
            long timeDelta = (long) ((float) (currentTime - this.updatedAt) * speedMultiplier);
            this.timeRunning += timeDelta;

            // 确保动画的进度总是向前的
            if (this.timeRunning < lastTimeRunning) {
                timeRunning = lastTimeRunning;
            } else {
                lastTimeRunning = timeRunning;
            }

            // 更新部分时间和最后更新时间
            this.partialTime = partial * speedMultiplier;
            this.updatedAt = currentTime;
        }
    }

    /**
     * 获取包含部分时间的运行时间（毫秒）
     * 对于循环动画，会确保时间在动画长度范围内
     *
     * @return 包含部分时间的运行时间（毫秒）
     */
    public float getTimeRunningWithPartial() {
        if (looping && animationLength > 0) {
            // 对于循环动画，确保时间在动画长度范围内
            long animationLengthMs = (long) (animationLength * 1000);
            return (this.timeRunning + this.partialTime) % animationLengthMs;
        }
        return this.timeRunning + this.partialTime;
    }

    /**
     * 获取运行时间（毫秒）
     * 对于循环动画，会确保时间在动画长度范围内
     *
     * @return 运行时间（毫秒）
     */
    public long getTimeRunning() {
        if (looping && animationLength > 0) {
            // 对于循环动画，确保时间在动画长度范围内
            long animationLengthMs = (long) (animationLength * 1000);
            return this.timeRunning % animationLengthMs;
        }
        return this.timeRunning;
    }

    /**
     * 检查动画是否正在运行
     *
     * @return 如果动画正在运行，返回true
     */
    public boolean isRunning() {
        return this.updatedAt != STOPPED;
    }

    /**
     * 重置运行时间
     */
    public void resetRunningTime() {
        this.timeRunning = 0;
        this.lastTimeRunning = 0;
    }
}