package org.foodcraft.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

/**
 * 自定义动画状态管理类
 * <p>
 * 用于跟踪和控制动画的播放状态、时间进度和循环行为。
 * 支持 NBT 序列化，可在服务端和客户端之间同步状态。
 * </p>
 *
 * <p><b>重要提示：</b>此类设计为可在服务端和客户端同时使用，通过 NBT 序列化确保数据同步。</p>
 * <p>如果动画状态未正确同步，可能导致不同客户端观察到不一致的动画表现。</p>
 */
public class ModAnimationState {
    /** 停止状态的特殊标记值 */
    private static final long STOPPED = Long.MAX_VALUE;


    private static final String NBT_KEY_IS_RUNNING = "IsRunning";

    // 状态字段
    /** 动画是否正在运行 。一般由服务端保存 ，因为服务端无法直接操作{@linkplain #updatedAt}。*/
    public boolean isRunning;

    /** 上次更新时间（毫秒） */
    public long updatedAt = STOPPED;

    /** 动画已运行时间（毫秒） */
    public long timeRunning;

    /** 部分时间，用于确保动画流畅渲染 */
    public float partialTime;

    /** 是否循环播放动画 */
    private boolean looping = false;

    /** 动画长度（秒） */
    private float animationLength = 0;

    /** 上次记录的运行时间，用于确保动画进度不会回退 */
    private long lastTimeRunning = 0;

    /**
     * 创建新的动画状态实例
     */
    public ModAnimationState() {
        // 默认构造函数
    }

    // NBT 序列化方法

    /**
     * 将动画状态写入 NBT 数据
     *
     * @param nbt 要写入的 NBT 数据对象
     * @return NBT 数据
     */
    public NbtCompound writeToNbt(NbtCompound nbt) {
        nbt.putBoolean(NBT_KEY_IS_RUNNING, this.isRunning);

        return nbt;
    }

    /**
     * 从 NBT 数据读取动画状态
     *
     * @param nbt NBT 数据
     */
    public void readFromNbt(NbtCompound nbt) {
        if (nbt == null) {
            return;
        }

        if (nbt.contains(NBT_KEY_IS_RUNNING)) {
            this.isRunning = nbt.getBoolean(NBT_KEY_IS_RUNNING);
        }
    }

    /**
     * 创建包含当前动画状态的 NBT 数据
     *
     * @return 包含动画状态的 NBT 数据
     */
    public NbtCompound toNbt() {
        return writeToNbt(new NbtCompound());
    }

    /**
     * 复制另一个动画状态实例的状态
     *
     * @param other 要复制的动画状态实例
     */
    public void copyFrom(ModAnimationState other) {
        this.updatedAt = other.updatedAt;
        this.timeRunning = other.timeRunning;
        this.partialTime = other.partialTime;
        this.looping = other.looping;
        this.animationLength = other.animationLength;
        this.lastTimeRunning = other.lastTimeRunning;
    }

    // 动画控制方法

    /**
     * 检查动画是否应该运行（用于服务端逻辑判断）
     *
     * @param shouldRun 根据业务逻辑判断是否应该运行
     * @param age 实体年龄（tick数）
     */
    public void updateRunningState(boolean shouldRun, int age) {
        if (shouldRun && !this.isRunning()) {
            this.startIfNotRunning(age);
        } else if (!shouldRun && this.isRunning()) {
            this.stop();
        }
    }

    /**
     * 安全地设置运行状态（避免不必要的状态切换）
     *
     * @param running 是否运行
     * @param age 实体年龄（tick数）
     */
    public void setRunningSafely(boolean running, int age) {
        if (running && !this.isRunning()) {
            this.startIfNotRunning(age);
        } else if (!running && this.isRunning()) {
            this.stop();
        }
    }

    /**
     * 检查动画是否处于有效运行状态（同时检查标记和更新时间）
     */
    public boolean isEffectivelyRunning() {
        return this.isRunning && this.isRunning();
    }

    /**
     * 强制启动动画（无论是否已在运行）
     * <p>
     * 会重置运行时间并启动动画。
     * </p>
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
     * @param age     实体年龄（tick数）
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
     * <p>
     * 根据动画进度和参数更新动画的运行时间和状态。
     * </p>
     *
     * @param animationProgress 动画进度（0.0-1.0）
     * @param speedMultiplier   速度乘数
     * @param looping           是否循环播放
     * @param animationLength   动画长度（秒）
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

    // 状态获取方法

    /**
     * 获取包含部分时间的运行时间（毫秒）
     * <p>
     * 对于循环动画，会确保时间在动画长度范围内。
     * </p>
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
     * <p>
     * 对于循环动画，会确保时间在动画长度范围内。
     * </p>
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
     * @return 如果动画正在运行，返回 true
     */
    public boolean isRunning() {
        return this.updatedAt != STOPPED;
    }

    /**
     * 获取动画长度（秒）
     *
     * @return 动画长度（秒）
     */
    public float getAnimationLength() {
        return this.animationLength;
    }

    /**
     * 检查动画是否循环播放
     *
     * @return 如果动画循环播放，返回 true
     */
    public boolean isLooping() {
        return this.looping;
    }

    // 重置方法

    /**
     * 重置运行时间
     */
    public void resetRunningTime() {
        this.timeRunning = 0;
        this.lastTimeRunning = 0;
        this.partialTime = 0.0F;
    }

    /**
     * 完全重置动画状态
     * <p>
     * 重置所有状态到初始值，相当于创建新实例。
     * </p>
     */
    public void reset() {
        this.updatedAt = STOPPED;
        this.timeRunning = 0;
        this.partialTime = 0.0F;
        this.looping = false;
        this.animationLength = 0;
        this.lastTimeRunning = 0;
    }

    // 工具方法

    /**
     * 获取动画进度比例（0.0-1.0）
     * <p>
     * 计算当前动画进度的比例，对于非循环动画可能超过 1.0。
     * </p>
     *
     * @return 动画进度比例
     */
    public float getProgressRatio() {
        if (animationLength <= 0) {
            return 0.0F;
        }

        long animationLengthMs = (long) (animationLength * 1000);
        if (looping) {
            return (this.getTimeRunningWithPartial() % animationLengthMs) / (float) animationLengthMs;
        } else {
            return Math.min(this.getTimeRunningWithPartial() / (float) animationLengthMs, 1.0F);
        }
    }

    /**
     * 检查动画是否已完成（仅对非循环动画有效）
     *
     * @return 如果动画已完成且不循环，返回 true
     */
    public boolean isCompleted() {
        if (looping || animationLength <= 0) {
            return false;
        }

        long animationLengthMs = (long) (animationLength * 1000);
        return this.timeRunning >= animationLengthMs;
    }

    @Override
    public String toString() {
        return String.format("ModAnimationState{running=%s, timeRunning=%dms, progress=%.2f%%, looping=%s, length=%.2fs}",
                isRunning(),
                getTimeRunning(),
                getProgressRatio() * 100,
                looping,
                animationLength);
    }
}