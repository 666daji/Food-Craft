package org.foodcraft.block.multi;

import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.foodcraft.FoodCraft;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 多方块结构管理器 - 全局的多方块注册和查找服务
 *
 * <p>该类负责管理世界中所有的多方块结构实例，提供注册、查找、持久化和生命周期管理功能。
 * 使用弱引用映射来管理世界实例，防止内存泄漏。
 *
 * <h2>主要功能</h2>
 * <ul>
 * <li><strong>注册管理</strong> - 注册和注销多方块实例</li>
 * <li><strong>位置查找</strong> - 根据坐标快速找到对应的多方块结构</li>
 * <li><strong>数据持久化</strong> - 游戏重启时自动恢复多方块数据</li>
 * <li><strong>内存管理</strong> - 自动清理已销毁的多方块引用</li>
 * </ul>
 *
 * <h2>注意</h2>
 * <p>该类以{@link World}作为一集键来区分不同维度中的方块堆，因此不得注册客户端世界的{@link MultiBlock}</p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 查找指定位置的多方块
 * MultiBlock multiBlock = MultiBlockManager.findMultiBlock(world, pos);
 *
 * // 获取世界中的所有多方块
 * Collection<MultiBlock> allMultiBlocks = MultiBlockManager.getMultiBlocksInWorld(world);
 * }</pre>
 *
 * @see MultiBlockPersistentState
 */
public class MultiBlockManager {
    private static final Logger LOGGER = FoodCraft.LOGGER;

    /**
     * 存储了所有方块堆的映射。
     * <P>一级键存储不同世界中的方块堆映射，二级键使用主方块的坐标来标识一个方块堆</P>
     * <strong>注意：</strong>作为键的世界必须是一个服务器世界。
     */
    private static final Map<WorldView, Map<BlockPos, MultiBlock>> multiBlockRegistry = new WeakHashMap<>();
    private static final Object lock = new Object();

    /**
     * 注册一个新的MultiBlock
     * @param multiBlock 要注册的多方块实例
     * @return 如果成功注册返回true，否则返回false
     */
    public static boolean registerMultiBlock(MultiBlock multiBlock) {
        if (multiBlock == null) {
            LOGGER.error("Attempted to register null MultiBlock");
            return false;
        }

        if (multiBlock.getWorld() instanceof World world) {
            if (world.isClient) {
                LOGGER.error("Attempted to register MultiBlock on client world at {}", multiBlock.getMasterPos());
                multiBlock.dispose();
                return false;
            }
        }

        if (multiBlock.isDisposed()) {
            LOGGER.error("Attempted to register disposed MultiBlock at {}", multiBlock.getMasterPos());
            return false;
        }

        WorldView worldView = multiBlock.getWorld();
        BlockPos masterPos = multiBlock.getMasterPos();

        synchronized (lock) {
            Map<BlockPos, MultiBlock> worldMap = multiBlockRegistry.computeIfAbsent(worldView, k -> new ConcurrentHashMap<>());

            if (worldMap.containsKey(masterPos)) {
                MultiBlock existing = worldMap.get(masterPos);
                if (!existing.isDisposed()) {
                    LOGGER.warn("Position {} in world {} is already occupied by MultiBlock with base block {}",
                            masterPos, worldView, existing.getBaseBlock());
                    return false;
                } else {
                    // 清理已销毁的MultiBlock
                    worldMap.remove(masterPos);
                    LOGGER.debug("Cleaned up disposed MultiBlock at {}", masterPos);
                }
            }

            worldMap.put(masterPos, multiBlock);
            LOGGER.debug("Registered MultiBlock at {} in world", masterPos);

            // 持久化到存档
            if (worldView instanceof ServerWorld serverWorld) {
                MultiBlockPersistentState persistentState = MultiBlockPersistentState.getOrCreate(serverWorld);
                persistentState.addMultiBlock(serverWorld, multiBlock);
                LOGGER.debug("Persisted MultiBlock at {} to world storage", masterPos);
            }

            return true;
        }
    }

    /**
     * 注销一个MultiBlock。
     * @param multiBlock 要注销的多方块实例
     * @return 如果成功注销返回true，否则返回false
     * @apiNote 请不要直接调用该方法，如果需要注销方块堆，请直接调用{@link MultiBlock#dispose()}
     */
    public static boolean unregisterMultiBlock(MultiBlock multiBlock) {
        if (multiBlock == null) {
            LOGGER.warn("Attempted to unregister null MultiBlock");
            return false;
        }

        WorldView worldView = multiBlock.getWorld();
        BlockPos masterPos = multiBlock.getMasterPos();

        synchronized (lock) {
            Map<BlockPos, MultiBlock> worldMap = multiBlockRegistry.get(worldView);
            if (worldMap == null) {
                LOGGER.debug("No MultiBlocks registered for world");
                return false;
            }

            MultiBlock registered = worldMap.get(masterPos);
            if (registered == multiBlock) {
                worldMap.remove(masterPos);
                LOGGER.debug("Unregistered MultiBlock at {} from world", masterPos);

                // 从持久化存储中移除
                if (worldView instanceof ServerWorld serverWorld) {
                    MultiBlockPersistentState persistentState = MultiBlockPersistentState.getOrCreate(serverWorld);
                    persistentState.removeMultiBlock(serverWorld, masterPos);
                    LOGGER.debug("Removed MultiBlock at {} from persistent storage", masterPos);
                }

                // 如果这个世界没有其他MultiBlock了，清理世界映射
                if (worldMap.isEmpty()) {
                    multiBlockRegistry.remove(worldView);
                    LOGGER.debug("Removed empty world mapping");
                }
                return true;
            } else {
                LOGGER.warn("MultiBlock at {} was not the registered instance", masterPos);
                return false;
            }
        }
    }

    /**
     * 加载世界时恢复多方块数据
     */
    public static void loadWorldMultiBlocks(ServerWorld world) {
        synchronized (lock) {
            MultiBlockPersistentState persistentState = MultiBlockPersistentState.getOrCreate(world);
            Collection<MultiBlockPersistentState.MultiBlockData> multiBlockDataList = persistentState.getMultiBlocksForWorld(world);

            LOGGER.info("Loading {} MultiBlocks for world {}", multiBlockDataList.size(), world.getRegistryKey().getValue());

            int loadedCount = 0;
            for (MultiBlockPersistentState.MultiBlockData data : multiBlockDataList) {
                try {
                    // 重建MultiBlock
                    MultiBlock multiBlock = rebuildMultiBlockFromData(world, data);
                    if (multiBlock != null) {
                        // 注册到内存中（不重复持久化）
                        Map<BlockPos, MultiBlock> worldMap = multiBlockRegistry.computeIfAbsent(world, k -> new ConcurrentHashMap<>());
                        worldMap.put(data.masterPos(), multiBlock);
                        loadedCount++;
                        LOGGER.debug("Reloaded MultiBlock at {}", data.masterPos());
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to reload MultiBlock at {}: {}", data.masterPos(), e.getMessage());
                }
            }

            LOGGER.info("Successfully reloaded {}/{} MultiBlocks for world {}", loadedCount, multiBlockDataList.size(), world.getRegistryKey().getValue());
        }
    }

    /**
     * 从持久化数据重建MultiBlock
     */
    private static MultiBlock rebuildMultiBlockFromData(ServerWorld world, MultiBlockPersistentState.MultiBlockData data) {
        try {
            // 获取基础方块
            net.minecraft.util.Identifier blockId = new net.minecraft.util.Identifier(data.baseBlockId());
            net.minecraft.block.Block baseBlock = net.minecraft.registry.Registries.BLOCK.get(blockId);

            // 创建PatternRange
            MultiBlock.PatternRange range = new MultiBlock.PatternRange(data.start(), data.width(), data.height(), data.depth());

            // 重建MultiBlock
            return MultiBlock.builder()
                    .world(world)
                    .baseBlock(baseBlock)
                    .range(range)
                    .build();

        } catch (Exception e) {
            LOGGER.error("Failed to rebuild MultiBlock from data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 服务器关闭时清理
     */
    public static void onServerStopping(net.minecraft.server.MinecraftServer server) {
        synchronized (lock) {
            LOGGER.info("Server stopping, clearing MultiBlock registry");
            multiBlockRegistry.clear();
        }
    }

    /**
     * 手动备份多方块数据
     */
    public static void backupMultiBlockData(net.minecraft.server.MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            MultiBlockPersistentState persistentState = MultiBlockPersistentState.getOrCreate(world);
            persistentState.saveToFile(server);
        }
    }

    /**
     * 根据位置查找MultiBlock
     */
    @Nullable
    public static MultiBlock findMultiBlock(WorldView world, BlockPos pos) {
        synchronized (lock) {
            Map<BlockPos, MultiBlock> worldMap = multiBlockRegistry.get(world);
            if (worldMap == null) {
                return null;
            }

            // 首先检查精确匹配
            MultiBlock exactMatch = worldMap.get(pos);
            if (exactMatch != null && !exactMatch.isDisposed()) {
                return exactMatch;
            }

            // 如果没有精确匹配，检查该位置是否在某个MultiBlock的范围内
            for (MultiBlock multiBlock : worldMap.values()) {
                if (!multiBlock.isDisposed() && multiBlock.getRange().contains(pos)) {
                    LOGGER.debug("Found MultiBlock at {} containing position {}",
                            multiBlock.getMasterPos(), pos);
                    return multiBlock;
                }
            }

            return null;
        }
    }

    /**
     * 检查MultiBlock是否已注册且未销毁
     */
    public static boolean isRegistered(MultiBlock multiBlock) {
        if (multiBlock == null || multiBlock.isDisposed()) return false;

        synchronized (lock) {
            Map<BlockPos, MultiBlock> worldMap = multiBlockRegistry.get(multiBlock.getWorld());
            if (worldMap == null) return false;

            MultiBlock registered = worldMap.get(multiBlock.getMasterPos());
            return registered == multiBlock && !registered.isDisposed();
        }
    }

    /**
     * 检查位置是否被有效的MultiBlock占用
     */
    public static boolean isPositionOccupied(WorldView world, BlockPos pos) {
        synchronized (lock) {
            Map<BlockPos, MultiBlock> worldMap = multiBlockRegistry.get(world);
            if (worldMap == null) return false;

            MultiBlock multiBlock = worldMap.get(pos);
            return multiBlock != null && !multiBlock.isDisposed();
        }
    }

    /**
     * 获取世界中所有未销毁的MultiBlock
     */
    public static Collection<MultiBlock> getMultiBlocksInWorld(WorldView world) {
        synchronized (lock) {
            Map<BlockPos, MultiBlock> worldMap = multiBlockRegistry.get(world);
            if (worldMap == null) {
                return Collections.emptyList();
            }

            List<MultiBlock> validBlocks = new ArrayList<>();
            for (MultiBlock multiBlock : worldMap.values()) {
                if (!multiBlock.isDisposed()) {
                    validBlocks.add(multiBlock);
                }
            }
            return Collections.unmodifiableCollection(validBlocks);
        }
    }

    /**
     * 获取所有世界的MultiBlock数量统计
     */
    public static Map<WorldView, Integer> getRegistryStats() {
        synchronized (lock) {
            Map<WorldView, Integer> stats = new HashMap<>();
            for (Map.Entry<WorldView, Map<BlockPos, MultiBlock>> entry : multiBlockRegistry.entrySet()) {
                int count = (int) entry.getValue().values().stream()
                        .filter(mb -> !mb.isDisposed())
                        .count();
                if (count > 0) {
                    stats.put(entry.getKey(), count);
                }
            }
            return stats;
        }
    }

    /**
     * 清理指定世界的所有MultiBlock
     */
    public static int clearWorld(WorldView world) {
        synchronized (lock) {
            Map<BlockPos, MultiBlock> worldMap = multiBlockRegistry.remove(world);
            if (worldMap != null) {
                // 标记所有MultiBlock为已销毁状态
                for (MultiBlock multiBlock : worldMap.values()) {
                    try {
                        multiBlock.dispose();
                    } catch (Exception e) {
                        LOGGER.error("Error disposing MultiBlock at {}", multiBlock.getMasterPos(), e);
                    }
                }
                int count = worldMap.size();
                LOGGER.info("Cleared {} MultiBlocks from world", count);
                return count;
            }
            return 0;
        }
    }

    /**
     * 清理所有世界的MultiBlock（用于服务器关闭等情况）
     */
    public static void clearAll() {
        synchronized (lock) {
            int totalCount = 0;
            for (Map<BlockPos, MultiBlock> worldMap : multiBlockRegistry.values()) {
                for (MultiBlock multiBlock : worldMap.values()) {
                    try {
                        multiBlock.dispose();
                    } catch (Exception e) {
                        LOGGER.error("Error disposing MultiBlock at {}", multiBlock.getMasterPos(), e);
                    }
                }
                totalCount += worldMap.size();
            }
            multiBlockRegistry.clear();
            LOGGER.info("Cleared all {} MultiBlocks from registry", totalCount);
        }
    }

    /**
     * 执行垃圾回收检查，清理已销毁的MultiBlock引用
     */
    public static void performCleanup() {
        synchronized (lock) {
            Iterator<Map.Entry<WorldView, Map<BlockPos, MultiBlock>>> worldIterator = multiBlockRegistry.entrySet().iterator();
            int removedCount = 0;

            while (worldIterator.hasNext()) {
                Map.Entry<WorldView, Map<BlockPos, MultiBlock>> worldEntry = worldIterator.next();
                Map<BlockPos, MultiBlock> worldMap = worldEntry.getValue();

                Iterator<Map.Entry<BlockPos, MultiBlock>> blockIterator = worldMap.entrySet().iterator();
                while (blockIterator.hasNext()) {
                    Map.Entry<BlockPos, MultiBlock> blockEntry = blockIterator.next();
                    MultiBlock multiBlock = blockEntry.getValue();

                    // 清理已销毁的MultiBlock
                    if (multiBlock.isDisposed()) {
                        blockIterator.remove();
                        removedCount++;
                        LOGGER.debug("Cleaned up disposed MultiBlock at {}", blockEntry.getKey());
                    }
                }

                // 如果这个世界没有有效的MultiBlock了，移除世界条目
                if (worldMap.isEmpty()) {
                    worldIterator.remove();
                    LOGGER.debug("Removed empty world mapping");
                }
            }

            if (removedCount > 0) {
                LOGGER.info("Cleanup removed {} disposed MultiBlocks", removedCount);
            }
        }
    }

    /**
     * 使用try-with-resources模式创建临时MultiBlock
     */
    public static void withMultiBlock(WorldView world, Block baseBlock, MultiBlock.PatternRange range,
                                      Consumer<MultiBlock> action) {
        try (MultiBlock multiBlock = MultiBlock.builder()
                .world(world)
                .baseBlock(baseBlock)
                .range(range)
                .build()) {
            action.accept(multiBlock);
        } catch (Exception e) {
            LOGGER.error("Error in withMultiBlock", e);
        }
    }
}