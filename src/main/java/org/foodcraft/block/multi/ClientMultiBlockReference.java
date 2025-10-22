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
 * @see ServerMultiBlockReference
 */
public class ClientMultiBlockReference implements MultiBlockReference {
    private static final Logger LOGGER = FoodCraft.LOGGER;

    // 序列化键名
    private static final String MASTER_POS_KEY = "MasterPos";
    private static final String RELATIVE_POS_KEY = "RelativePos";
    private static final String BASE_BLOCK_KEY = "BaseBlock";

    private final BlockPos masterWorldPos;
    private final BlockPos relativePos;
    private final Block baseBlock;
    private final BlockPos worldPos;
    private final int volume;

    private ClientMultiBlockReference(@NotNull BlockPos masterWorldPos, @NotNull BlockPos relativePos,
                                     @NotNull Block baseBlock, int volume) {
        this.masterWorldPos = Objects.requireNonNull(masterWorldPos, "Master world position cannot be null");
        this.relativePos = Objects.requireNonNull(relativePos, "Relative position cannot be null");
        this.baseBlock = Objects.requireNonNull(baseBlock, "Base block cannot be null");
        this.volume = volume;

        // 计算世界坐标
        this.worldPos = new BlockPos(
                masterWorldPos.getX() + relativePos.getX(),
                masterWorldPos.getY() + relativePos.getY(),
                masterWorldPos.getZ() + relativePos.getZ()
        );
    }

    public ClientMultiBlockReference(@NotNull NbtCompound nbt) {
        // 从NBT反序列化
        this.masterWorldPos = NbtHelper.toBlockPos(nbt.getCompound(MASTER_POS_KEY));
        this.relativePos = NbtHelper.toBlockPos(nbt.getCompound(RELATIVE_POS_KEY));

        String blockId = nbt.getString(BASE_BLOCK_KEY);
        this.baseBlock = net.minecraft.registry.Registries.BLOCK.get(new net.minecraft.util.Identifier(blockId));

        // 客户端无法知道实际体积，使用默认值或从NBT读取（如果有的话）
        this.volume = nbt.contains("Volume") ? nbt.getInt("Volume") : 1;

        // 计算世界坐标
        this.worldPos = new BlockPos(
                masterWorldPos.getX() + relativePos.getX(),
                masterWorldPos.getY() + relativePos.getY(),
                masterWorldPos.getZ() + relativePos.getZ()
        );

        LOGGER.debug("Created ClientMultiBlockReference: master={}, relative={}, block={}",
                masterWorldPos, relativePos, baseBlock);
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
        nbt.putString(BASE_BLOCK_KEY, baseBlock.getRegistryEntry().registryKey().getValue().toString());

        // 体积
        nbt.putInt("Volume", volume);

        LOGGER.debug("Serialized ClientMultiBlockReference: master={}, relative={}, block={}",
                masterWorldPos, relativePos, baseBlock);

        return nbt;
    }

    @Override
    public boolean matchesBlock(Block block) {
        return baseBlock == block;
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
        return masterWorldPos;
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
        return baseBlock;
    }

    @Override
    public int getVolume() {
        return volume;
    }

    @Override
    public boolean containsWorldPos(BlockPos worldPos) {
        // 客户端无法精确判断，基于已知信息估算范围
        int width = 1, height = 1, depth = 1; // 默认大小

        // 如果是主方块，可以假设有一定的大小范围
        if (isMasterBlock() && volume > 1) {
            // 简单估算：假设是立方体
            int side = (int) Math.cbrt(volume);
            width = height = depth = Math.max(1, side);
        }

        BlockPos endPos = new BlockPos(
                masterWorldPos.getX() + width - 1,
                masterWorldPos.getY() + height - 1,
                masterWorldPos.getZ() + depth - 1
        );

        return worldPos.getX() >= masterWorldPos.getX() && worldPos.getX() <= endPos.getX() &&
                worldPos.getY() >= masterWorldPos.getY() && worldPos.getY() <= endPos.getY() &&
                worldPos.getZ() >= masterWorldPos.getZ() && worldPos.getZ() <= endPos.getZ();
    }

    @Override
    public boolean checkIntegrity() {
        // 客户端无法检查完整性，默认返回true
        return true;
    }

    @Override
    public boolean isDisposed() {
        // 客户端引用不会被dispose，由服务器同步状态
        return false;
    }

    @Override
    public void dispose() {
        // 客户端引用不需要dispose，由服务器同步管理
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
        return String.format("ClientMultiBlockReference{masterPos=%s, relativePos=%s, worldPos=%s, isMaster=%b}",
                masterWorldPos, relativePos, worldPos, isMasterBlock());
    }
}