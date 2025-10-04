package org.foodcraft.block.multi;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * 多方块引用接口 - 表示单个方块在多方块结构中的位置信息
 *
 * <p>该接口为多方块结构中的每个方块提供身份标识，记录方块所属的多方块结构以及在该结构中的相对位置。
 * 每个参与多方块的方块实体都应该持有此引用。
 *
 * <h2>核心功能</h2>
 * <ul>
 * <li><strong>位置标识</strong> - 记录方块在多方块结构中的相对位置</li>
 * <li><strong>完整性检查</strong> - 检查所属多方块结构是否完整</li>
 * <li><strong>数据序列化</strong> - 支持NBT序列化，确保游戏重启后数据不丢失</li>
 * <li><strong>类型验证</strong> - 验证方块类型是否与多方块基础方块匹配</li>
 * </ul>
 *
 * <h2>使用场景</h2>
 * <p>方块实体通过此引用可以：
 * <ul>
 * <li>知道自己属于哪个更大的结构</li>
 * <li>查询自己在结构中的具体位置</li>
 * <li>检查整个结构是否还完整</li>
 * <li>在GUI中显示结构信息</li>
 * </ul>
 *
 * @see MultiBlock
 */
public interface MultiBlockReference {

    /**
     * 将引用序列化为NBT
     */
    @NotNull
    NbtCompound toNbt();

    /**
     * 检查传入的方块是否与方块堆的基础方块相同
     */
    boolean matchesBlock(Block block);

    /**
     * 检查传入的方块状态对应的方块是否与方块堆的基础方块相同
     */
    boolean matchesBlockState(BlockState blockState);

    /**
     * 检查传入的方块实体对应的方块是否与方块堆的基础方块相同
     */
    boolean matchesBlockEntity(BlockEntity blockEntity);

    /**
     * 判断当前引用位置是否为主方块（起始坐标位置）
     */
    boolean isMasterBlock();

    /**
     * 获取主方块的世界坐标
     */
    BlockPos getMasterWorldPos();

    /**
     * 获取当前方块的世界坐标
     */
    BlockPos getWorldPos();

    /**
     * 获取相对坐标
     */
    BlockPos getRelativePos();

    /**
     * 获取基础方块类型
     */
    Block getBaseBlock();

    /**
     * 获取方块堆的体积
     */
    int getVolume();

    /**
     * 检查方块堆是否包含指定的世界坐标
     */
    boolean containsWorldPos(BlockPos worldPos);

    /**
     * 检查方块堆的完整性
     */
    boolean checkIntegrity();

    /**
     * 检查方块堆是否已被销毁
     */
    boolean isDisposed();

    /**
     * 安全地销毁这个引用
     */
    void dispose();

    /**
     * 获取当前方块在X方向上的相对位置
     */
    int getRelativeX();

    /**
     * 获取当前方块在Y方向上的相对位置
     */
    int getRelativeY();

    /**
     * 获取当前方块在Z方向上的相对位置
     */
    int getRelativeZ();
}