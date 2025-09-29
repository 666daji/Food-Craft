package org.foodcraft.block.multi;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.foodcraft.FoodCraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * 表示方块堆中的一个方块的引用
 * 包含方块堆实例和该方块在方块堆中的相对坐标
 */
public class MultiBlockReference {
    private static final Logger LOGGER = FoodCraft.LOGGER;

    private final MultiBlock multiBlock;
    private final BlockPos relativePos;
    private final BlockPos worldPos;

    private MultiBlockReference(@NotNull MultiBlock multiBlock, @NotNull BlockPos relativePos) {
        this.multiBlock = Objects.requireNonNull(multiBlock, "MultiBlock cannot be null");
        this.relativePos = Objects.requireNonNull(relativePos, "Relative position cannot be null");
        this.worldPos = multiBlock.getWorldPos(relativePos);

        // 验证相对坐标是否在有效范围内
        validateRelativePosition();
    }

    /**
     * 验证相对坐标是否在方块堆的有效范围内
     */
    private void validateRelativePosition() {
        if (relativePos.getX() < 0 || relativePos.getX() >= multiBlock.getRange().getWidth() ||
                relativePos.getY() < 0 || relativePos.getY() >= multiBlock.getRange().getHeight() ||
                relativePos.getZ() < 0 || relativePos.getZ() >= multiBlock.getRange().getDepth()) {
            throw new IllegalArgumentException("Relative position " + relativePos +
                    " is out of MultiBlock range " + multiBlock.getRange());
        }
    }

    /**
     * 检查传入的方块是否与方块堆的基础方块相同
     */
    public boolean matchesBlock(Block block) {
        if (multiBlock.isDisposed()) {
            LOGGER.warn("Attempted to check block match with disposed MultiBlock");
            return false;
        }
        return multiBlock.getBaseBlock() == block;
    }

    /**
     * 检查传入的方块状态对应的方块是否与方块堆的基础方块相同
     */
    public boolean matchesBlockState(BlockState blockState) {
        return matchesBlock(blockState.getBlock());
    }

    /**
     * 检查传入的方块实体对应的方块是否与方块堆的基础方块相同
     */
    public boolean matchesBlockEntity(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        return matchesBlockState(blockEntity.getCachedState());
    }

    /**
     * 判断当前引用位置是否为主方块（起始坐标位置）
     */
    public boolean isMasterBlock() {
        return relativePos.getX() == 0 && relativePos.getY() == 0 && relativePos.getZ() == 0;
    }

    /**
     * 获取主方块的世界坐标
     */
    public BlockPos getMasterWorldPos() {
        return multiBlock.getMasterPos();
    }

    /**
     * 获取当前方块的世界坐标
     */
    public BlockPos getWorldPos() {
        return worldPos;
    }

    /**
     * 获取相对坐标
     */
    public BlockPos getRelativePos() {
        return relativePos;
    }

    /**
     * 获取方块堆实例
     */
    public MultiBlock getMultiBlock() {
        return multiBlock;
    }

    /**
     * 检查方块堆的完整性
     */
    public boolean checkIntegrity() {
        if (multiBlock.isDisposed()) {
            LOGGER.warn("Attempted to check integrity of disposed MultiBlock");
            return false;
        }
        return multiBlock.checkIntegrity();
    }

    /**
     * 获取基础方块类型
     */
    public Block getBaseBlock() {
        return multiBlock.getBaseBlock();
    }

    /**
     * 获取方块堆的体积
     */
    public int getVolume() {
        return multiBlock.getVolume();
    }

    /**
     * 检查方块堆是否包含指定的世界坐标
     */
    public boolean containsWorldPos(BlockPos worldPos) {
        return multiBlock.getRange().contains(worldPos);
    }

    /**
     * 根据当前方块的相对坐标获取另一个相对坐标的世界位置
     */
    public BlockPos getWorldPosFromRelative(int relativeX, int relativeY, int relativeZ) {
        return multiBlock.getWorldPos(relativeX, relativeY, relativeZ);
    }

    /**
     * 根据相对坐标获取世界位置
     */
    public BlockPos getWorldPosFromRelative(BlockPos relativePos) {
        return multiBlock.getWorldPos(relativePos);
    }

    /**
     * 获取当前方块在X方向上的相对位置
     */
    public int getRelativeX() {
        return relativePos.getX();
    }

    /**
     * 获取当前方块在Y方向上的相对位置
     */
    public int getRelativeY() {
        return relativePos.getY();
    }

    /**
     * 获取当前方块在Z方向上的相对位置
     */
    public int getRelativeZ() {
        return relativePos.getZ();
    }

    /**
     * 检查方块堆是否已被销毁
     */
    public boolean isDisposed() {
        return multiBlock.isDisposed();
    }

    /**
     * 安全地销毁这个引用
     */
    public void dispose() {
        // 这里只是清理引用，实际的MultiBlock由MultiBlockManager管理
        LOGGER.debug("Disposed MultiBlockReference at relative position {}", relativePos);
    }

    /**
     * 创建从世界坐标到相对坐标的引用
     */
    @Nullable
    public static MultiBlockReference fromWorldPos(WorldView world, BlockPos worldPos) {
        MultiBlock multiBlock = MultiBlockManager.findMultiBlock(world, worldPos);
        if (multiBlock == null || multiBlock.isDisposed()) {
            return null;
        }

        BlockPos masterPos = multiBlock.getMasterPos();
        BlockPos relativePos = new BlockPos(
                worldPos.getX() - masterPos.getX(),
                worldPos.getY() - masterPos.getY(),
                worldPos.getZ() - masterPos.getZ()
        );

        try {
            return new MultiBlockReference(multiBlock, relativePos);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("World position {} is not within MultiBlock range", worldPos);
            return null;
        }
    }

    /**
     * 创建从已知方块堆和世界坐标的引用
     */
    @Nullable
    public static MultiBlockReference fromWorldPos(MultiBlock multiBlock, BlockPos worldPos) {
        if (multiBlock == null || multiBlock.isDisposed()) {
            return null;
        }

        if (!multiBlock.getRange().contains(worldPos)) {
            LOGGER.warn("World position {} is not within MultiBlock range {}",
                    worldPos, multiBlock.getRange());
            return null;
        }

        BlockPos masterPos = multiBlock.getMasterPos();
        BlockPos relativePos = new BlockPos(
                worldPos.getX() - masterPos.getX(),
                worldPos.getY() - masterPos.getY(),
                worldPos.getZ() - masterPos.getZ()
        );

        return new MultiBlockReference(multiBlock, relativePos);
    }

    /**
     * 创建从已知方块堆和相对坐标的引用
     */
    public static MultiBlockReference fromRelativePos(MultiBlock multiBlock, BlockPos relativePos) {
        return new MultiBlockReference(multiBlock, relativePos);
    }

    /**
     * 创建从已知方块堆和相对坐标的引用
     */
    public static MultiBlockReference fromRelativePos(MultiBlock multiBlock, int relativeX, int relativeY, int relativeZ) {
        return fromRelativePos(multiBlock, new BlockPos(relativeX, relativeY, relativeZ));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MultiBlockReference that = (MultiBlockReference) obj;
        return Objects.equals(multiBlock, that.multiBlock) &&
                Objects.equals(relativePos, that.relativePos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multiBlock, relativePos);
    }

    @Override
    public String toString() {
        return String.format("MultiBlockReference{multiBlock=%s, relativePos=%s, worldPos=%s, isMaster=%b}",
                multiBlock.getMasterPos(), relativePos, worldPos, isMasterBlock());
    }

    public static class Builder {
        private MultiBlock multiBlock;
        private BlockPos relativePos;
        private BlockPos worldPos;

        public Builder multiBlock(MultiBlock multiBlock) {
            this.multiBlock = multiBlock;
            return this;
        }

        public Builder relativePos(BlockPos relativePos) {
            this.relativePos = relativePos;
            this.worldPos = null; // 清除缓存的worldPos
            return this;
        }

        public Builder relativePos(int x, int y, int z) {
            return relativePos(new BlockPos(x, y, z));
        }

        public Builder worldPos(BlockPos worldPos) {
            this.worldPos = worldPos;
            this.relativePos = null; // 清除缓存的relativePos
            return this;
        }

        public Builder worldPos(int x, int y, int z) {
            return worldPos(new BlockPos(x, y, z));
        }

        @Nullable
        public MultiBlockReference build() {
            if (multiBlock == null) {
                LOGGER.error("MultiBlock cannot be null when building MultiBlockReference");
                return null;
            }

            if (multiBlock.isDisposed()) {
                LOGGER.error("Cannot create reference to disposed MultiBlock");
                return null;
            }

            try {
                if (relativePos != null) {
                    return new MultiBlockReference(multiBlock, relativePos);
                } else if (worldPos != null) {
                    return fromWorldPos(multiBlock, worldPos);
                } else {
                    LOGGER.error("Either relativePos or worldPos must be provided");
                    return null;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Failed to create MultiBlockReference: {}", e.getMessage());
                return null;
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}