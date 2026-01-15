package org.foodcraft.food;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 将食物属性简单的封装起来供临时使用。
 *
 * @param Hunger             饥饿值（恢复的饥饿点数）
 * @param SaturationModifier 饱和度修饰符（决定饱食度恢复量）
 */
public record SimpleFoodComponent(int Hunger, float SaturationModifier) {

    /**
     * 从FoodComponent创建SimpleFoodComponent
     * @param foodComponent Minecraft原版食物组件
     * @return SimpleFoodComponent实例
     */
    public static SimpleFoodComponent fromFoodComponent(FoodComponent foodComponent) {
        return new SimpleFoodComponent(
                foodComponent.getHunger(),
                foodComponent.getSaturationModifier()
        );
    }

    /**
     * 拼接两个食物属性。
     * @param first 第一个食物属性
     * @param second 第二个食物属性
     */
    public static SimpleFoodComponent computeFoodComponent(FoodComponent first, FoodComponent second) {
        int hunger = second.getHunger() + first.getHunger();
        float saturationModifier = (second.getSaturationModifier() + first.getSaturationModifier()) / 2;
        return new SimpleFoodComponent(hunger, saturationModifier);
    }

    /**
     * 返回当前食物属性值的指定百分比。
     * @param percentage 百分比（0-100）
     * @return 新的SimpleFoodComponent实例，属性值为原始值的指定百分比
     */
    public SimpleFoodComponent percent(int percentage) {
        int validPercentage = Math.max(0, Math.min(percentage, 100));
        int newHunger = (int) Math.round(this.Hunger * validPercentage / 100.0);
        float newSaturationModifier = this.SaturationModifier * validPercentage / 100.0f;
        return new SimpleFoodComponent(newHunger, newSaturationModifier);
    }

    /**
     * 增加指定的饥饿值
     * @param amount 增加的饥饿值（可为负数）
     * @return 新的SimpleFoodComponent实例
     */
    public SimpleFoodComponent addHunger(int amount) {
        return new SimpleFoodComponent(Math.max(0, this.Hunger + amount), this.SaturationModifier);
    }

    /**
     * 增加指定的饱和度修饰符
     * @param amount 增加的饱和度修饰符（可为负数）
     * @return 新的SimpleFoodComponent实例
     */
    public SimpleFoodComponent addSaturationModifier(float amount) {
        return new SimpleFoodComponent(this.Hunger, Math.max(0, this.SaturationModifier + amount));
    }

    /**
     * 按比例调整饱和度修饰符
     * @param multiplier 倍数
     * @return 新的SimpleFoodComponent实例
     */
    public SimpleFoodComponent multiplySaturation(float multiplier) {
        return new SimpleFoodComponent(this.Hunger, Math.max(0, this.SaturationModifier * multiplier));
    }

    /**
     * 合并两个SimpleFoodComponent
     * @param other 另一个食物属性
     * @return 合并后的新实例（饥饿值相加，饱和度修饰符取平均值）
     */
    public SimpleFoodComponent merge(SimpleFoodComponent other) {
        int newHunger = this.Hunger + other.Hunger;
        float newSaturationModifier = (this.SaturationModifier + other.SaturationModifier) / 2;
        return new SimpleFoodComponent(newHunger, newSaturationModifier);
    }

    /**
     * 转换为Minecraft原版FoodComponent.Builder
     * @return FoodComponent.Builder实例
     */
    public FoodComponent.Builder toFoodComponentBuilder() {
        return new FoodComponent.Builder()
                .hunger(this.Hunger)
                .saturationModifier(this.SaturationModifier);
    }

    /**
     * 计算总饱和度值（饥饿值 * 饱和度修饰符 * 2）
     * @return 总饱和度
     */
    public float getTotalSaturation() {
        return this.Hunger * this.SaturationModifier * 2;
    }

    /**
     * 检查是否比另一个食物属性更好
     * @param other 另一个食物属性
     * @return 如果总饱和度更高则返回true
     */
    public boolean isBetterThan(SimpleFoodComponent other) {
        return this.getTotalSaturation() > other.getTotalSaturation();
    }

    /**
     * 获取显示用的字符串
     * @return 格式化的字符串
     */
    @Override
    public @NotNull String toString() {
        return String.format("Food[Hunger=%d, Saturation=%.2f, Total=%.1f]",
                Hunger, SaturationModifier, getTotalSaturation());
    }

    /**
     * 尝试为玩家增加饥饿值
     * @param player 目标玩家
     */
    public void eat(PlayerEntity player) {
        player.getHungerManager().add(
                Hunger(), SaturationModifier());
    }
}