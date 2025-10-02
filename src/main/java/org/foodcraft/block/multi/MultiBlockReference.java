package org.foodcraft.block.multi;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * 多方块引用的接口定义
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