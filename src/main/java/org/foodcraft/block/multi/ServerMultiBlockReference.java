package org.foodcraft.block.multi;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.Identifier;
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

    /**
     * 从世界坐标创建服务端引用
     */
    @Nullable
    public static ServerMultiBlockReference fromWorldPos(@NotNull MultiBlock multiBlock, @NotNull BlockPos worldPos) {
        if (multiBlock.isDisposed()) {
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
     * 从世界坐标创建通用引用（服务端或客户端）
     */
    @Nullable
    public static MultiBlockReference fromWorldPos(@NotNull WorldView world, @NotNull BlockPos worldPos, boolean createClientReference) {
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
            if (createClientReference && world.isClient()) {
                // 在客户端创建客户端引用
                ServerMultiBlockReference serverRef = new ServerMultiBlockReference(multiBlock, relativePos);
                return ClientMultiBlockReference.fromServerReference(serverRef);
            } else {
                // 在服务端创建服务端引用
                return new ServerMultiBlockReference(multiBlock, relativePos);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("World position {} is not within MultiBlock range", worldPos);
            return null;
        }
    }

    /**
     * 从相对坐标创建引用
     */
    @NotNull
    public static MultiBlockReference fromRelativePos(@NotNull MultiBlock multiBlock, @NotNull BlockPos relativePos) {
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
            Block baseBlock = net.minecraft.registry.Registries.BLOCK.get(Identifier.tryParse(blockId));

            // 查找对应的MultiBlock（服务端引用）
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

            // 创建服务端引用
            ServerMultiBlockReference ref = new ServerMultiBlockReference(multiBlock, relativePos);

            LOGGER.debug("Deserialized ServerMultiBlockReference: master={}, relative={}, block={}",
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
        nbt.putString(BASE_BLOCK_KEY, net.minecraft.registry.Registries.BLOCK.getId(multiBlock.getBaseBlock()).toString());

        // 结构尺寸
        nbt.putInt(STRUCTURE_WIDTH_KEY, getStructureWidth());
        nbt.putInt(STRUCTURE_HEIGHT_KEY, getStructureHeight());
        nbt.putInt(STRUCTURE_DEPTH_KEY, getStructureDepth());

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
        if (blockState != null) {
            return matchesBlock(blockState.getBlock());
        }
        return false;
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
    public @NotNull BlockPos getMasterWorldPos() {
        if (multiBlock.isDisposed()) {
            throw new IllegalStateException("MultiBlock has been disposed");
        }
        return multiBlock.getMasterPos();
    }

    @Override
    public @NotNull BlockPos getWorldPos() {
        return worldPos;
    }

    @Override
    public @NotNull BlockPos getRelativePos() {
        return relativePos;
    }

    @Override
    public @NotNull Block getBaseBlock() {
        if (multiBlock.isDisposed()) {
            throw new IllegalStateException("MultiBlock has been disposed");
        }
        return multiBlock.getBaseBlock();
    }

    @Override
    public int getVolume() {
        return multiBlock.isDisposed() ? 0 : multiBlock.getVolume();
    }

    @Override
    public boolean containsWorldPos(@NotNull BlockPos worldPos) {
        return !multiBlock.isDisposed() && multiBlock.getRange().contains(worldPos);
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
    public boolean isValid() {
        return !multiBlock.isDisposed() && multiBlock.checkIntegrity();
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

    @Override
    public int getStructureWidth() {
        return multiBlock.isDisposed() ? 0 : multiBlock.getRange().getWidth();
    }

    @Override
    public int getStructureHeight() {
        return multiBlock.isDisposed() ? 0 : multiBlock.getRange().getHeight();
    }

    @Override
    public int getStructureDepth() {
        return multiBlock.isDisposed() ? 0 : multiBlock.getRange().getDepth();
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
        return String.format("ServerMultiBlockReference{multiBlock=%s, relativePos=%s, worldPos=%s, isMaster=%b, size=%dx%dx%d}",
                multiBlock.getMasterPos(), relativePos, worldPos, isMasterBlock(),
                getStructureWidth(), getStructureHeight(), getStructureDepth());
    }
}