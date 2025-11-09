package org.foodcraft.block.multi;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.foodcraft.FoodCraft;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多方块数据的持久化存储
 */
public class MultiBlockPersistentState extends PersistentState {
    private static final Logger LOGGER = FoodCraft.LOGGER;
    private static final String PERSISTENT_ID = "multiblocks";

    /**
     * 临时存储的方块堆数据
     * @see MultiBlockManager#multiBlockRegistry
     */
    private final Map<Identifier, Map<BlockPos, MultiBlockData>> worldData = new ConcurrentHashMap<>();

    public MultiBlockPersistentState() {
        super();
    }

    /**
     * 多方块数据的序列化表示
     */
    public record MultiBlockData(BlockPos masterPos, String baseBlockId, BlockPos start, int width, int height,
                                 int depth) {
        public @NotNull NbtCompound toNbt() {
                NbtCompound nbt = new NbtCompound();
                nbt.put("masterPos", NbtHelper.fromBlockPos(masterPos));
                nbt.putString("baseBlockId", baseBlockId);
                nbt.put("start", NbtHelper.fromBlockPos(start));
                nbt.putInt("width", width);
                nbt.putInt("height", height);
                nbt.putInt("depth", depth);
                return nbt;
            }

            public static @NotNull MultiBlockData fromNbt(@NotNull NbtCompound nbt) {
                BlockPos masterPos = NbtHelper.toBlockPos(nbt.getCompound("masterPos"));
                String baseBlockId = nbt.getString("baseBlockId");
                BlockPos start = NbtHelper.toBlockPos(nbt.getCompound("start"));
                int width = nbt.getInt("width");
                int height = nbt.getInt("height");
                int depth = nbt.getInt("depth");
                return new MultiBlockData(masterPos, baseBlockId, start, width, height, depth);
            }
        }

    /**
     * 添加多方块数据
     */
    public void addMultiBlock(@NotNull World world, @NotNull MultiBlock multiBlock) {
        Identifier worldId = world.getRegistryKey().getValue();
        Map<BlockPos, MultiBlockData> worldMap = worldData.computeIfAbsent(worldId, k -> new ConcurrentHashMap<>());

        MultiBlockData data = new MultiBlockData(
                multiBlock.getMasterPos(),
                multiBlock.getBaseBlock().getRegistryEntry().registryKey().getValue().toString(),
                multiBlock.getRange().getStart(),
                multiBlock.getRange().getWidth(),
                multiBlock.getRange().getHeight(),
                multiBlock.getRange().getDepth()
        );

        worldMap.put(multiBlock.getMasterPos(), data);
        markDirty();

        LOGGER.debug("Added MultiBlock data to persistent storage: {}", multiBlock.getMasterPos());
    }

    /**
     * 移除多方块数据
     */
    public void removeMultiBlock(@NotNull World world, BlockPos masterPos) {
        Identifier worldId = world.getRegistryKey().getValue();
        Map<BlockPos, MultiBlockData> worldMap = worldData.get(worldId);
        if (worldMap != null) {
            worldMap.remove(masterPos);
            markDirty();
            LOGGER.debug("Removed MultiBlock data from persistent storage: {}", masterPos);
        }
    }

    /**
     * 获取世界中所有的多方块数据
     */
    public Collection<MultiBlockData> getMultiBlocksForWorld(@NotNull World world) {
        Identifier worldId = world.getRegistryKey().getValue();
        Map<BlockPos, MultiBlockData> worldMap = worldData.get(worldId);
        return worldMap != null ? worldMap.values() : Collections.emptyList();
    }

    /**
     * 清除世界中的所有多方块数据
     */
    public void clearWorldData(@NotNull World world) {
        Identifier worldId = world.getRegistryKey().getValue();
        worldData.remove(worldId);
        markDirty();
        LOGGER.info("Cleared all MultiBlock data for world: {}", worldId);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList worldsList = new NbtList();

        for (Map.Entry<Identifier, Map<BlockPos, MultiBlockData>> worldEntry : worldData.entrySet()) {
            NbtCompound worldNbt = new NbtCompound();
            worldNbt.putString("worldId", worldEntry.getKey().toString());

            NbtList multiBlocksList = new NbtList();
            for (MultiBlockData data : worldEntry.getValue().values()) {
                multiBlocksList.add(data.toNbt());
            }

            worldNbt.put("multiBlocks", multiBlocksList);
            worldsList.add(worldNbt);
        }

        nbt.put("worlds", worldsList);
        LOGGER.info("Saved {} worlds with MultiBlock data to persistent storage", worldData.size());

        return nbt;
    }

    /**
     * 从NBT读取数据
     */
    public static @NotNull MultiBlockPersistentState fromNbt(NbtCompound nbt) {
        MultiBlockPersistentState state = new MultiBlockPersistentState();

        NbtList worldsList = nbt.getList("worlds", NbtElement.COMPOUND_TYPE);
        for (NbtElement worldElement : worldsList) {
            NbtCompound worldNbt = (NbtCompound) worldElement;
            Identifier worldId = new Identifier(worldNbt.getString("worldId"));

            Map<BlockPos, MultiBlockData> worldMap = new ConcurrentHashMap<>();
            NbtList multiBlocksList = worldNbt.getList("multiBlocks", NbtElement.COMPOUND_TYPE);

            for (NbtElement blockElement : multiBlocksList) {
                NbtCompound blockNbt = (NbtCompound) blockElement;
                try {
                    MultiBlockData data = MultiBlockData.fromNbt(blockNbt);
                    worldMap.put(data.masterPos, data);
                } catch (Exception e) {
                    LOGGER.error("Failed to load MultiBlock data from NBT: {}", e.getMessage());
                }
            }

            state.worldData.put(worldId, worldMap);
        }

        LOGGER.info("Loaded {} worlds with MultiBlock data from persistent storage", state.worldData.size());
        return state;
    }

    /**
     * 获取或创建持久化状态
     */
    public static MultiBlockPersistentState getOrCreate(ServerWorld world) {
        PersistentStateManager persistentStateManager = world.getPersistentStateManager();
        return persistentStateManager.getOrCreate(
                MultiBlockPersistentState::fromNbt,
                MultiBlockPersistentState::new,
                PERSISTENT_ID
        );
    }

    /**
     * 保存到文件（手动备份）
     */
    public void saveToFile(MinecraftServer server) {
        try {
            File worldDir = server.getSavePath(WorldSavePath.ROOT).toFile();
            File backupFile = new File(worldDir, "multiblocks_backup.dat");

            NbtCompound nbt = this.writeNbt(new NbtCompound());
            net.minecraft.nbt.NbtIo.write(nbt, backupFile);

            LOGGER.info("MultiBlock data backed up to: {}", backupFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to backup MultiBlock data: {}", e.getMessage());
        }
    }
}