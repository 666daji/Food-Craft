package org.foodcraft.block.process.step;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.block.process.AbstractProcess;
import org.foodcraft.util.FoodCraftUtils;

import java.util.Map;
import java.util.Random;

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
 * @param <T>         方块实体类型
 * @param process     当前执行的流程实例
 * @param blockEntity 执行步骤的方块实体
 * @param blockState  方块的当前状态
 * @param world       方块所在的世界
 * @param pos         方块的位置
 * @param player      执行交互的玩家
 * @param hand        玩家使用的手（主手或副手）
 * @param hit         方块点击信息
 * @param contextData 临时上下文数据存储（仅当前步骤执行期间有效）
 */
public record StepExecutionContext<T>(AbstractProcess<T> process, T blockEntity, BlockState blockState, World world,
                                      BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit,
                                      Map<String, Object> contextData) {

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
     * 从临时上下文数据中获取值。
     *
     * @param key 数据的键
     * @param <V> 数据的类型
     * @return 数据的值，如果不存在则返回null
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

    /**
     * 获取玩家当前手持物品的方块音效组。
     * @return 获取的音效组，物品不是方块时返回石头音效组
     */
    public BlockSoundGroup getItemSounds() {
        BlockSoundGroup group = FoodCraftUtils.getSoundGroupFromItem(getHeldItemStack());
        if (group != null) {
            return group;
        }

        return BlockSoundGroup.STONE;
    }

    /**
     * 在方块的位置播放一段声音。
     *
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
     * 在方块位置生成物品粒子效果。
     *
     * @param stack 用于粒子效果的物品堆栈
     */
    public void spawnItemParticles(ItemStack stack) {
        Random random = new Random();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.05;
        double z = pos.getZ() + 0.5;

        int particles = 8;

        for (int i = 0; i < particles; i++) {
            double vx = (random.nextDouble() - 0.5) * 0.4;
            double vy = random.nextDouble() * 0.3;
            double vz = (random.nextDouble() - 0.5) * 0.4;

            world.addParticle(
                    new ItemStackParticleEffect(ParticleTypes.ITEM, stack),
                    x, y, z, vx, vy, vz
            );
        }
    }
}