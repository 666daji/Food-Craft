package org.foodcraft.block.process.playeraction;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.block.process.step.StepExecutionContext;

import java.util.Optional;

/**
 * 玩家操作基类，代表玩家在流程步骤中执行的具体交互动作。
 *
 * <p>每个操作可以：</p>
 * <ul>
 *   <li>从字符串反序列化（用于配方定义）</li>
 *   <li>从执行上下文创建（用于实时交互）</li>
 *   <li>转换为字符串（用于序列化）</li>
 *   <li>转换为物品堆栈（用于库存接口兼容）</li>
 *   <li>执行操作特定的消耗逻辑</li>
 * </ul>
 *
 * <p><strong>字符串格式：</strong></p>
 * <pre>
 * 操作类型|参数1|参数2|...
 * 例如：add_item|minecraft:beef
 * </pre>
 */
public abstract class PlayerAction {

    // ==================== 核心方法 ====================

    /**
     * 从字符串创建操作实例。
     *
     * <p>用于从配方JSON或NBT数据中恢复操作。</p>
     *
     * @param str 格式为"类型|参数1|参数2"的字符串
     * @return 对应的操作实例
     * @throws IllegalArgumentException 如果字符串格式无效或类型未注册
     */
    public static PlayerAction fromString(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("The operation string cannot be empty");
        }

        // 解析格式：类型|参数1|参数2...
        String[] parts = str.split("\\|");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid operation string formatting: " + str);
        }

        String type = parts[0].trim();
        String[] params = new String[parts.length - 1];
        System.arraycopy(parts, 1, params, 0, params.length);

        return PlayerActionFactory.create(type, params);
    }

    /**
     * 从执行上下文创建操作实例。
     *
     * <p>用于在流程步骤中根据玩家当前的交互情况创建操作。
     * 不是所有操作都能从任意上下文创建，子类可以重写此方法。</p>
     *
     * @param context 步骤执行上下文
     * @return 如果可以从上下文创建操作，则返回包含操作的Optional；否则返回空Optional
     */
    public static Optional<PlayerAction> fromContext(StepExecutionContext<?> context) {
        // 默认实现返回空，子类可以重写
        return Optional.empty();
    }

    /**
     * 将操作转换为字符串表示。
     *
     * <p>用于序列化到NBT或JSON。</p>
     *
     * @return 格式为"类型|参数1|参数2"的字符串
     */
    public abstract String toString();

    /**
     * 将操作转换为物品堆栈。
     *
     * <p>用于兼容原版库存接口，返回的物品堆栈可能用于：</p>
     * <ul>
     *   <li>在方块实体的物品栏中显示</li>
     *   <li>与库存系统交互</li>
     *   <li>提供物品形式的表示</li>
     * </ul>
     *
     * @return 代表此操作的物品堆栈
     */
    public abstract ItemStack toItemStack();

    /**
     * 执行操作的消耗逻辑。
     *
     * <p>当操作被执行时调用，用于处理物品消耗、工具耐久减少等。</p>
     *
     * @param context 步骤执行上下文
     */
    public abstract void consume(StepExecutionContext<?> context);

    /**
     * 检查两个操作是否匹配。
     *
     * <p>用于配方匹配：检查玩家执行的操作是否与配方要求的操作匹配。</p>
     *
     * @param other 要比较的另一个操作
     * @return 如果两个操作代表相同的动作则返回true
     */
    public abstract boolean matches(PlayerAction other);

    /**
     * 获取操作的简短编码表示。
     * <ul>
     *     <li>编码应尽可能简洁，不超过8个字符，且能唯一标识该操作（包括类型和参数）。</li>
     *     <li>编码应该稳定，相同的操作总是返回相同的编码。</li>
     * </ul>
     *
     * @return 操作的编码字符串（长度 ≤ 8）
     */
    public abstract String getCode();

    /**
     * 获取操作的显示名称。
     *
     * @return 操作的显示名称
     */
    public abstract Text getDisplayName();

    /**
     * 检查操作是否有效。
     *
     * @param world 世界实例
     * @param pos 位置
     * @return 如果操作在当前上下文中有效则返回true
     */
    public boolean isValid(World world, BlockPos pos) {
        return true;
    }

    /**
     * 获取操作类型的标识符。
     *
     * @return 操作类型标识符
     */
    public abstract String getType();

    // ==================== 对象方法重写 ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PlayerAction other = (PlayerAction) obj;
        return this.matches(other);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}