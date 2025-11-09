package org.foodcraft.block.multi;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import org.foodcraft.FoodCraft;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * 客户端多方块引用实现
 * 只包含显示所需的信息，不包含实际功能，数据完全从服务器同步
 */
public class ClientMultiBlockReference implements MultiBlockReference {
    private static final Logger LOGGER = FoodCraft.LOGGER;

    private final BlockPos masterWorldPos;
    private final BlockPos relativePos;
    private final Block baseBlock;
    private final BlockPos worldPos;
    private final int structureWidth;
    private final int structureHeight;
    private final int structureDepth;
    private boolean disposed = false;

    public ClientMultiBlockReference(@NotNull NbtCompound nbt) {
        // 从NBT反序列化
        this.masterWorldPos = NbtHelper.toBlockPos(nbt.getCompound(MASTER_POS_KEY));
        this.relativePos = NbtHelper.toBlockPos(nbt.getCompound(RELATIVE_POS_KEY));

        String blockId = nbt.getString(BASE_BLOCK_KEY);
        this.baseBlock = net.minecraft.registry.Registries.BLOCK.get(net.minecraft.util.Identifier.tryParse(blockId));

        // 读取结构尺寸
        this.structureWidth = nbt.getInt(STRUCTURE_WIDTH_KEY);
        this.structureHeight = nbt.getInt(STRUCTURE_HEIGHT_KEY);
        this.structureDepth = nbt.getInt(STRUCTURE_DEPTH_KEY);

        // 计算世界坐标
        this.worldPos = new BlockPos(
                masterWorldPos.getX() + relativePos.getX(),
                masterWorldPos.getY() + relativePos.getY(),
                masterWorldPos.getZ() + relativePos.getZ()
        );

        LOGGER.debug("Created ClientMultiBlockReference: master={}, relative={}, block={}, size={}x{}x{}",
                masterWorldPos, relativePos, baseBlock, structureWidth, structureHeight, structureDepth);
    }

    /**
     * 从服务端引用创建客户端引用
     */
    public static ClientMultiBlockReference fromServerReference(@NotNull MultiBlockReference serverReference) {
        if (serverReference.isDisposed()) {
            throw new IllegalArgumentException("Cannot create client reference from disposed server reference");
        }

        NbtCompound nbt = serverReference.toNbt();
        // 确保包含结构尺寸信息
        if (!nbt.contains(STRUCTURE_WIDTH_KEY)) {
            nbt.putInt(STRUCTURE_WIDTH_KEY, serverReference.getStructureWidth());
            nbt.putInt(STRUCTURE_HEIGHT_KEY, serverReference.getStructureHeight());
            nbt.putInt(STRUCTURE_DEPTH_KEY, serverReference.getStructureDepth());
        }

        return new ClientMultiBlockReference(nbt);
    }

    @Override
    @NotNull
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        // 主方块坐标
        nbt.put(MASTER_POS_KEY, NbtHelper.fromBlockPos(masterWorldPos));

        // 相对坐标
        nbt.put(RELATIVE_POS_KEY, NbtHelper.fromBlockPos(relativePos));

        // 基础方块
        if (baseBlock != null) {
            nbt.putString(BASE_BLOCK_KEY, net.minecraft.registry.Registries.BLOCK.getId(baseBlock).toString());
        } else {
            nbt.putString(BASE_BLOCK_KEY, "minecraft:air");
        }

        // 结构尺寸
        nbt.putInt(STRUCTURE_WIDTH_KEY, structureWidth);
        nbt.putInt(STRUCTURE_HEIGHT_KEY, structureHeight);
        nbt.putInt(STRUCTURE_DEPTH_KEY, structureDepth);

        LOGGER.debug("Serialized ClientMultiBlockReference: master={}, relative={}, block={}",
                masterWorldPos, relativePos, baseBlock);

        return nbt;
    }

    @Override
    public boolean matchesBlock(Block block) {
        return !disposed && baseBlock == block;
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
        return !disposed && relativePos.getX() == 0 && relativePos.getY() == 0 && relativePos.getZ() == 0;
    }

    @Override
    public @NotNull BlockPos getMasterWorldPos() {
        if (disposed) {
            throw new IllegalStateException("ClientMultiBlockReference has been disposed");
        }
        return masterWorldPos;
    }

    @Override
    public @NotNull BlockPos getWorldPos() {
        if (disposed) {
            throw new IllegalStateException("ClientMultiBlockReference has been disposed");
        }
        return worldPos;
    }

    @Override
    public @NotNull BlockPos getRelativePos() {
        if (disposed) {
            throw new IllegalStateException("ClientMultiBlockReference has been disposed");
        }
        return relativePos;
    }

    @Override
    public @NotNull Block getBaseBlock() {
        if (disposed) {
            throw new IllegalStateException("ClientMultiBlockReference has been disposed");
        }
        return baseBlock;
    }

    @Override
    public int getVolume() {
        return disposed ? 0 : structureWidth * structureHeight * structureDepth;
    }

    @Override
    public boolean containsWorldPos(@NotNull BlockPos worldPos) {
        if (disposed) return false;

        BlockPos endPos = new BlockPos(
                masterWorldPos.getX() + structureWidth - 1,
                masterWorldPos.getY() + structureHeight - 1,
                masterWorldPos.getZ() + structureDepth - 1
        );

        return worldPos.getX() >= masterWorldPos.getX() && worldPos.getX() <= endPos.getX() &&
                worldPos.getY() >= masterWorldPos.getY() && worldPos.getY() <= endPos.getY() &&
                worldPos.getZ() >= masterWorldPos.getZ() && worldPos.getZ() <= endPos.getZ();
    }

    @Override
    public boolean checkIntegrity() {
        // 客户端无法检查完整性，默认返回true
        return !disposed;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public boolean isValid() {
        return !disposed && masterWorldPos != null && relativePos != null && baseBlock != null &&
                structureWidth > 0 && structureHeight > 0 && structureDepth > 0;
    }

    @Override
    public void dispose() {
        disposed = true;
        LOGGER.debug("ClientMultiBlockReference disposed at relative position {}", relativePos);
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
        return structureWidth;
    }

    @Override
    public int getStructureHeight() {
        return structureHeight;
    }

    @Override
    public int getStructureDepth() {
        return structureDepth;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ClientMultiBlockReference that = (ClientMultiBlockReference) obj;
        return Objects.equals(masterWorldPos, that.masterWorldPos) &&
                Objects.equals(relativePos, that.relativePos) &&
                Objects.equals(baseBlock, that.baseBlock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterWorldPos, relativePos, baseBlock);
    }

    @Override
    public String toString() {
        return String.format("ClientMultiBlockReference{masterPos=%s, relativePos=%s, worldPos=%s, isMaster=%b, size=%dx%dx%d}",
                masterWorldPos, relativePos, worldPos, isMasterBlock(), structureWidth, structureHeight, structureDepth);
    }
}