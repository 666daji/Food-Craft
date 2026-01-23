package org.foodcraft.block.process.playeraction;

import org.foodcraft.block.process.step.StepExecutionContext;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家操作工厂，用于创建和管理不同类型的操作。
 *
 * <p>工厂模式允许动态注册新的操作类型，使系统易于扩展。</p>
 */
public final class PlayerActionFactory {

    private static final Map<String, PlayerActionParser> PARSERS = new HashMap<>();
    private static final Map<String, PlayerActionCreator> CREATORS = new HashMap<>();

    private PlayerActionFactory() {
        // 防止实例化
    }

    // ==================== 注册方法 ====================

    /**
     * 注册操作类型。
     *
     * @param type 操作类型标识符（如"add_item"）
     * @param parser 用于从字符串参数解析操作的解析器
     * @param creator 用于从上下文创建操作的创建器（可为null）
     */
    public static void register(String type, PlayerActionParser parser, @Nullable PlayerActionCreator creator) {
        if (PARSERS.containsKey(type)) {
            throw new IllegalArgumentException("The action type is registered: " + type);
        }

        PARSERS.put(type, parser);
        if (creator != null) {
            CREATORS.put(type, creator);
        }
    }

    /**
     * 仅注册解析器（用于配方解析，不支持从上下文创建）。
     */
    public static void registerParser(String type, PlayerActionParser parser) {
        register(type, parser, null);
    }

    // ==================== 创建方法 ====================

    /**
     * 从字符串创建操作。
     *
     * @param type 操作类型
     * @param params 参数字符串数组
     * @return 创建的操作实例
     */
    public static PlayerAction create(String type, String[] params) {
        PlayerActionParser parser = PARSERS.get(type);
        if (parser == null) {
            throw new IllegalArgumentException("Types of actions that are not registered: " + type);
        }

        return parser.parse(params);
    }

    /**
     * 尝试从上下文创建操作。
     *
     * @param type 操作类型
     * @param context 步骤执行上下文
     * @return 如果创建器存在且成功创建，则返回操作实例；否则返回null
     */
    @Nullable
    public static PlayerAction createFromContext(String type, StepExecutionContext<?> context) {
        PlayerActionCreator creator = CREATORS.get(type);
        if (creator == null) {
            return null;
        }

        return creator.create(context);
    }

    /**
     * 获取所有已注册的操作类型。
     */
    public static String[] getRegisteredTypes() {
        return PARSERS.keySet().toArray(new String[0]);
    }

    /**
     * 检查操作类型是否已注册。
     */
    public static boolean isRegistered(String type) {
        return PARSERS.containsKey(type);
    }

    // ==================== 内部接口 ====================

    /**
     * 操作解析器接口，用于从字符串参数创建操作。
     */
    @FunctionalInterface
    public interface PlayerActionParser {
        PlayerAction parse(String[] params);
    }

    /**
     * 操作创建器接口，用于从上下文创建操作。
     */
    @FunctionalInterface
    public interface PlayerActionCreator {
        PlayerAction create(StepExecutionContext<?> context);
    }
}