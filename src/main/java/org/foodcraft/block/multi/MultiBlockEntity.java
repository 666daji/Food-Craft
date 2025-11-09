package org.foodcraft.block.multi;

import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * 表示可以保存多方块引用的方块实体接口。
 *
 * <p>任何需要与多方块系统交互的方块实体都应该实现此接口，
 * 以便在多方块结构变化时自动更新引用。</p>
 *
 * <h2>引用类型</h2>
 * <ul>
 *   <li><strong>服务端</strong> - 使用 {@link ServerMultiBlockReference}，包含完整逻辑</li>
 *   <li><strong>客户端</strong> - 使用 {@link ClientMultiBlockReference}，只读视图</li>
 * </ul>
 *
 * @see MultiBlockReference
 * @see ServerMultiBlockReference
 * @see ClientMultiBlockReference
 */
public interface MultiBlockEntity {

    /**
     * 设置该方块实体所属的多方块引用。
     *
     * <p><strong>注意：</strong></p>
     * <ul>
     *   <li>当方块被添加到多方块结构时，引用不为null</li>
     *   <li>当方块从多方块结构中移除时，引用为null</li>
     *   <li>实现应该处理引用变化时的状态更新</li>
     *   <li>服务端和客户端可能持有不同类型的引用</li>
     * </ul>
     *
     * @param reference 多方块引用，如果为null表示不属于任何多方块
     */
    void setMultiBlockReference(@Nullable MultiBlockReference reference);

    /**
     * 获取该方块实体所属的多方块引用。
     *
     * @return 多方块引用，如果不属于任何多方块返回null
     */
    @Nullable
    MultiBlockReference getMultiBlockReference();

    /**
     * 获取该方块实体的位置。
     * 用于在多方块系统中定位和识别方块。
     *
     * @return 方块实体的世界位置
     */
    BlockPos getMultiBlockPos();

    /**
     * 检查该方块实体是否属于一个有效的多方块结构。
     *
     * <p><strong>有效性条件：</strong></p>
     * <ul>
     *   <li>多方块引用不为null</li>
     *   <li>引用本身是有效的（{@link MultiBlockReference#isValid()}）</li>
     *   <li>引用位置与方块实体位置匹配</li>
     * </ul>
     *
     * @return 如果属于有效的多方块结构返回true，否则返回false
     */
    default boolean hasValidMultiBlock() {
        MultiBlockReference ref = getMultiBlockReference();
        return ref != null && ref.isValid() && ref.getWorldPos().equals(getMultiBlockPos());
    }

    /**
     * 检查多方块引用是否为服务端引用。
     *
     * @return 如果是服务端引用返回true，客户端引用返回false
     */
    default boolean isServerReference() {
        MultiBlockReference ref = getMultiBlockReference();
        return ref != null && this instanceof ServerMultiBlockReference;
    }

    /**
     * 检查多方块引用是否为客户端引用。
     *
     * @return 如果是客户端引用返回true，服务端引用返回false
     */
    default boolean isClientReference() {
        MultiBlockReference ref = getMultiBlockReference();
        return ref != null && this instanceof ClientMultiBlockReference;
    }

    /**
     * 当多方块结构发生变化时调用此方法。
     * 实现可以在此方法中更新外观、状态或其他依赖多方块的功能。
     *
     * <p><strong>触发时机：</strong></p>
     * <ul>
     *   <li>方块被添加到多方块结构时</li>
     *   <li>方块从多方块结构中移除时</li>
     *   <li>多方块结构被拆分或合并时</li>
     *   <li>引用类型发生变化时（服务端/客户端）</li>
     * </ul>
     *
     * @param oldReference 变化前的多方块引用（可能为null）
     * @param newReference 变化后的多方块引用（可能为null）
     */
    default void onMultiBlockChanged(@Nullable MultiBlockReference oldReference,
                                     @Nullable MultiBlockReference newReference) {
    }

    /**
     * 获取多方块结构信息用于显示。
     * 主要用于GUI、提示文本等显示用途。
     *
     * @return 结构信息字符串，如果没有多方块引用返回null
     */
    @Nullable
    default String getMultiBlockInfo() {
        MultiBlockReference ref = getMultiBlockReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }

        return String.format("MultiBlock %dx%dx%d at %s",
                ref.getStructureWidth(),
                ref.getStructureHeight(),
                ref.getStructureDepth(),
                ref.getMasterWorldPos());
    }
}