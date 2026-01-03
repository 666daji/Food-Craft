package org.foodcraft.block.process.step;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.block.process.AbstractProcess;

import java.util.Map;

/**
 * 步骤执行上下文，封装了执行步骤所需的所有信息。
 *
 * <p>当步骤执行时，流程系统会创建一个上下文对象并传递给步骤的{@link Step#execute}方法。
 * 上下文包含：</p>
 * <ul>
 *   <li>流程实例：可以访问流程状态数据</li>
 *   <li>方块实体、状态、位置和世界</li>
 *   <li>玩家信息和交互信息</li>
 *   <li>临时上下文数据存储（仅当前步骤执行期间有效）</li>
 * </ul>
 *
 * <p>使用上下文对象，步骤可以：</p>
 * <ul>
 *   <li>访问和修改流程状态数据</li>
 *   <li>操作方块实体和方块状态</li>
 *   <li>与玩家交互（给予物品、播放音效等）</li>
 *   <li>在步骤执行期间存储临时数据</li>
 * </ul>
 *
 * @param <T> 方块实体类型
 */
public class StepExecutionContext<T> {

    // ============ 公共字段 ============

    /** 当前执行的流程实例 */
    public final AbstractProcess<T> process;

    /** 执行步骤的方块实体 */
    public final T blockEntity;

    /** 方块的当前状态 */
    public final BlockState blockState;

    /** 方块所在的世界 */
    public final World world;

    /** 方块的位置 */
    public final BlockPos pos;

    /** 执行交互的玩家 */
    public final PlayerEntity player;

    /** 玩家使用的手（主手或副手） */
    public final Hand hand;

    /** 方块点击信息 */
    public final BlockHitResult hit;

    /** 临时上下文数据存储（仅当前步骤执行期间有效） */
    public final Map<String, Object> contextData;

    // ============ 构造函数 ============

    /**
     * 创建步骤执行上下文。
     *
     * @param process 流程实例
     * @param blockEntity 方块实体
     * @param blockState 方块状态
     * @param world 世界
     * @param pos 位置
     * @param player 玩家
     * @param hand 使用的手
     * @param hit 点击信息
     * @param contextData 临时上下文数据存储
     */
    public StepExecutionContext(AbstractProcess<T> process, T blockEntity,
                                BlockState blockState, World world, BlockPos pos,
                                PlayerEntity player, Hand hand, BlockHitResult hit,
                                Map<String, Object> contextData) {
        this.process = process;
        this.blockEntity = blockEntity;
        this.blockState = blockState;
        this.world = world;
        this.pos = pos;
        this.player = player;
        this.hand = hand;
        this.hit = hit;
        this.contextData = contextData;
    }

    // ============ 辅助方法 ============

    /**
     * 获取玩家当前手持的物品堆栈。
     *
     * <p>这是{@code player.getStackInHand(hand)}的快捷方式。</p>
     *
     * @return 玩家手持的物品堆栈
     */
    public ItemStack getHeldItemStack() {
        return player.getStackInHand(hand);
    }

    /**
     * 检查交互的玩家是否为创造模式。
     *
     * @return 玩家是否为创造模式
     */
    public boolean isCreateMode() {
        return player.isCreative();
    }

    /**
     * 将物品堆栈给予给玩家。
     *
     * @param stack 要给予的物品
     */
    public void giveStack(ItemStack stack) {
        if (isCreateMode()) {
            return;
        }

        if (!player.giveItemStack(stack)) {
            player.dropItem(stack, false);
        }
    }

    /**
     * 在方块的位置播放一段声音。
     * @param event 播放的声音事件
     */
    public void playSound(SoundEvent event) {
        world.playSound(
                null,
                pos,
                event,
                SoundCategory.BLOCKS,
                0.5F,
                1.0F
        );
    }

    /**
     * 从临时上下文数据中获取值。
     *
     * @param key 数据的键
     * @return 数据的值，如果不存在则返回null
     * @param <V> 数据的类型
     */
    @SuppressWarnings("unchecked")
    public <V> V getContextData(String key) {
        return (V) contextData.get(key);
    }

    /**
     * 向临时上下文数据中设置值。
     *
     * @param key 数据的键
     * @param value 数据的值
     */
    public void setContextData(String key, Object value) {
        contextData.put(key, value);
    }

    /**
     * 检查步骤执行是否在客户端。
     *
     * <p>这是{@code world.isClient}的快捷方式。</p>
     *
     * @return 如果在客户端执行，则返回true
     */
    public boolean isClientSide() {
        return world.isClient;
    }

    /**
     * 检查步骤执行是否在服务器端。
     *
     * <p>这是{@code !world.isClient}的快捷方式。</p>
     *
     * @return 如果在服务器端执行，则返回true
     */
    public boolean isServerSide() {
        return !world.isClient;
    }
}