package org.foodcraft.block.multi;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.foodcraft.FoodCraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * 服务端多方块引用实现
 */
public class ServerMultiBlockReference implements MultiBlockReference {
    private static final Logger LOGGER = FoodCraft.LOGGER;

    // 序列化键名
    private static final String MASTER_POS_KEY = "MasterPos";
    private static final String RELATIVE_POS_KEY = "RelativePos";
    private static final String BASE_BLOCK_KEY = "BaseBlock";

    private final MultiBlock multiBlock;
    private final BlockPos relativePos;
    private final BlockPos worldPos;

    public ServerMultiBlockReference(@NotNull MultiBlock multiBlock, @NotNull BlockPos relativePos) {
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

    @Nullable
    public static ServerMultiBlockReference fromWorldPos(MultiBlock multiBlock, BlockPos worldPos) {
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

        return new ServerMultiBlockReference(multiBlock, relativePos);
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
            return new ServerMultiBlockReference(multiBlock, relativePos);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("World position {} is not within MultiBlock range", worldPos);
            return null;
        }
    }

    /**
     * 创建从已知方块堆和相对坐标的引用
     */
    public static MultiBlockReference fromRelativePos(MultiBlock multiBlock, BlockPos relativePos) {
        return new ServerMultiBlockReference(multiBlock, relativePos);
    }

    /**
     * 从NBT反序列化引用（服务端版本）
     */
    @Nullable
    public static ServerMultiBlockReference fromNbt(@NotNull WorldView world, @NotNull NbtCompound nbt) {
        try {
            // 读取主方块坐标
            BlockPos masterPos = NbtHelper.toBlockPos(nbt.getCompound(MASTER_POS_KEY));

            // 读取相对坐标
            BlockPos relativePos = NbtHelper.toBlockPos(nbt.getCompound(RELATIVE_POS_KEY));

            // 读取基础方块
            String blockId = nbt.getString(BASE_BLOCK_KEY);
            Block baseBlock = net.minecraft.registry.Registries.BLOCK.get(new net.minecraft.util.Identifier(blockId));

            // 查找对应的MultiBlock
            MultiBlock multiBlock = MultiBlockManager.findMultiBlock(world, masterPos);
            if (multiBlock == null || multiBlock.isDisposed()) {
                LOGGER.warn("MultiBlock at {} not found or disposed during deserialization", masterPos);
                return null;
            }

            // 验证基础方块是否匹配
            if (multiBlock.getBaseBlock() != baseBlock) {
                LOGGER.warn("Base block mismatch during deserialization. Expected: {}, Found: {}",
                        baseBlock, multiBlock.getBaseBlock());
                return null;
            }

            // 创建引用
            ServerMultiBlockReference ref = new ServerMultiBlockReference(multiBlock, relativePos);

            LOGGER.debug("Deserialized MultiBlockReference: master={}, relative={}, block={}",
                    masterPos, relativePos, baseBlock);

            return ref;

        } catch (Exception e) {
            LOGGER.error("Failed to deserialize MultiBlockReference from NBT: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @NotNull
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        // 主方块坐标
        nbt.put(MASTER_POS_KEY, NbtHelper.fromBlockPos(multiBlock.getMasterPos()));

        // 相对坐标
        nbt.put(RELATIVE_POS_KEY, NbtHelper.fromBlockPos(relativePos));

        // 基础方块
        nbt.putString(BASE_BLOCK_KEY, multiBlock.getBaseBlock().getRegistryEntry().registryKey().getValue().toString());

        LOGGER.debug("Serialized ServerMultiBlockReference: master={}, relative={}, block={}",
                multiBlock.getMasterPos(), relativePos, multiBlock.getBaseBlock());

        return nbt;
    }

    @Override
    public boolean matchesBlock(Block block) {
        if (multiBlock.isDisposed()) {
            LOGGER.warn("Attempted to check block match with disposed MultiBlock");
            return false;
        }
        return multiBlock.getBaseBlock() == block;
    }

    @Override
    public boolean matchesBlockState(BlockState blockState) {
        return matchesBlock(blockState.getBlock());
    }

    @Override
    public boolean matchesBlockEntity(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        return matchesBlockState(blockEntity.getCachedState());
    }

    @Override
    public boolean isMasterBlock() {
        return relativePos.getX() == 0 && relativePos.getY() == 0 && relativePos.getZ() == 0;
    }

    @Override
    public BlockPos getMasterWorldPos() {
        return multiBlock.getMasterPos();
    }

    @Override
    public BlockPos getWorldPos() {
        return worldPos;
    }

    @Override
    public BlockPos getRelativePos() {
        return relativePos;
    }

    @Override
    public Block getBaseBlock() {
        return multiBlock.getBaseBlock();
    }

    @Override
    public int getVolume() {
        return multiBlock.getVolume();
    }

    @Override
    public boolean containsWorldPos(BlockPos worldPos) {
        return multiBlock.getRange().contains(worldPos);
    }

    @Override
    public boolean checkIntegrity() {
        if (multiBlock.isDisposed()) {
            LOGGER.warn("Attempted to check integrity of disposed MultiBlock");
            return false;
        }
        return multiBlock.checkIntegrity();
    }

    @Override
    public boolean isDisposed() {
        return multiBlock.isDisposed();
    }

    @Override
    public void dispose() {
        // 这里只是清理引用，实际的MultiBlock由MultiBlockManager管理
        LOGGER.debug("Disposed ServerMultiBlockReference at relative position {}", relativePos);
    }

    @Override
    public int getRelativeX() {
        return relativePos.getX();
    }

    @Override
    public int getRelativeY() {
        return relativePos.getY();
    }

    @Override
    public int getRelativeZ() {
        return relativePos.getZ();
    }

    /**
     * 获取多方块堆实例（仅服务端可用）
     */
    public MultiBlock getMultiBlock() {
        return multiBlock;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ServerMultiBlockReference that = (ServerMultiBlockReference) obj;
        return Objects.equals(multiBlock, that.multiBlock) &&
                Objects.equals(relativePos, that.relativePos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multiBlock, relativePos);
    }

    @Override
    public String toString() {
        return String.format("ServerMultiBlockReference{multiBlock=%s, relativePos=%s, worldPos=%s, isMaster=%b}",
                multiBlock.getMasterPos(), relativePos, worldPos, isMasterBlock());
    }
}