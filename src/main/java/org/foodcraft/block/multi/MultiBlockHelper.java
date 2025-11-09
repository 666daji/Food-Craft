package org.foodcraft.block.multi;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.FoodCraft;
import org.slf4j.Logger;

import java.util.*;

/**
 * 多方块操作助手 - 处理方块放置和破坏时的多方块逻辑
 *
 * <p>该类作为多方块系统的协调器，负责处理方块生命周期事件并维护多方块结构的完整性。
 * 它提供了自动合并、拆分和引用更新等核心功能。</p>
 *
 * <h2>核心职责</h2>
 * <ul>
 *   <li><strong>事件处理</strong> - 处理方块放置、破坏和邻居更新事件</li>
 *   <li><strong>自动合并</strong> - 执行多轮合并，最大化多方块结构</li>
 *   <li><strong>引用更新</strong> - 更新实现{@link MultiBlockEntity}的方块实体中的多方块引用</li>
 *   <li><strong>完整性维护</strong> - 确保多方块结构在变化时保持正确状态</li>
 *   <li><strong>调试工具</strong> - 提供强制更新和修复功能</li>
 * </ul>
 *
 * @see MultiBlockEntity
 * @see MultiBlock
 * @see MultiBlockManager
 */
public class MultiBlockHelper {
    private static final Logger LOGGER = FoodCraft.LOGGER;

    /** 最大合并轮数，防止无限循环 */
    private static final int MAX_MERGE_ROUNDS = 10;

    /**
     * 处理方块放置事件，创建新的多方块结构并尝试与相邻结构合并。
     *
     * @param world      世界实例
     * @param pos        方块位置
     * @param coreBlock  核心方块类型
     *
     * @see MultiBlockEntity
     * @see #performMultiRoundMerging(MultiBlock)
     * @apiNote 需要在使用了多方块系统的对应方块类中重写
     * {@link net.minecraft.block.AbstractBlock#onBlockAdded(BlockState, World, BlockPos, BlockState, boolean)}
     * 方法主动调用此方法。
     */
    public static void onBlockPlaced(World world, BlockPos pos, Block coreBlock) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(pos, "Position cannot be null");
        Objects.requireNonNull(coreBlock, "Core block cannot be null");

        LOGGER.debug("Core block placed at {}", pos);

        // 检查该位置是否已经有方块堆
        MultiBlock existing = MultiBlockManager.findMultiBlock(world, pos);
        if (existing != null && !existing.isDisposed()) {
            // 位置已有方块堆，检查完整性
            LOGGER.debug("Found existing MultiBlock at {}, checking integrity", pos);
            List<MultiBlock> newBlocks = existing.checkAndSplitIntegrity();
            updateBlockEntityReferences(world, newBlocks);
            LOGGER.info("After integrity check, created {} new MultiBlocks", newBlocks.size());
            return;
        }

        // 创建新的单方块堆
        MultiBlock newMultiBlock = createSingleBlockMultiBlock(world, pos, coreBlock);
        if (newMultiBlock == null) {
            LOGGER.error("Failed to create single block MultiBlock at {}", pos);
            return;
        }

        // 更新当前方块的引用
        updateBlockEntityReference(world, pos, newMultiBlock);

        // 尝试与相邻的方块堆合并
        performMultiRoundMerging(newMultiBlock);
    }

    /**
     * 处理方块破坏事件，拆分受影响的多方块结构并尝试重新合并。
     *
     * @param world      世界实例
     * @param pos        被破坏的方块位置
     * @param coreBlock  核心方块类型
     * @apiNote 需要在使用了多方块系统的对应方块类中重写
     * {@link net.minecraft.block.AbstractBlock#onStateReplaced(BlockState, World, BlockPos, BlockState, boolean)}
     * 方法主动调用此方法。
     */
    public static void onBlockBroken(World world, BlockPos pos, Block coreBlock) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(pos, "Position cannot be null");
        Objects.requireNonNull(coreBlock, "Core block cannot be null");

        LOGGER.debug("Core block broken at {}", pos);

        // 找到包含该位置的方块堆
        MultiBlock multiBlock = MultiBlockManager.findMultiBlock(world, pos);
        if (multiBlock == null || multiBlock.isDisposed()) {
            LOGGER.debug("No MultiBlock found at {}", pos);
            return;
        }

        // 检查并拆分方块堆
        LOGGER.info("Breaking block at {} in MultiBlock {}, checking integrity",
                pos, multiBlock.getMasterPos());
        List<MultiBlock> newBlocks = multiBlock.checkAndSplitIntegrity();

        // 更新所有新方块堆的引用
        updateBlockEntityReferences(world, newBlocks);

        LOGGER.info("After breaking, created {} new MultiBlocks", newBlocks.size());

        // 尝试合并新创建的方块堆（多轮合并）
        for (MultiBlock newBlock : newBlocks) {
            performMultiRoundMerging(newBlock);
        }
    }

    /**
     * 处理相邻方块更新事件，验证多方块结构的完整性。
     *
     * @param world      世界实例
     * @param pos        受影响的核心方块位置
     * @param coreBlock  核心方块类型
     * @apiNote 需要在使用了多方块系统的对应方块类中重写
     * {@link net.minecraft.block.AbstractBlock#neighborUpdate(BlockState, World, BlockPos, Block, BlockPos, boolean)}
     * 方法主动调用此方法。
     */
    public static void onNeighborUpdate(World world, BlockPos pos, Block coreBlock) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(pos, "Position cannot be null");
        Objects.requireNonNull(coreBlock, "Core block cannot be null");

        LOGGER.debug("Neighbor update for core block at {}", pos);

        // 找到包含该位置的方块堆
        MultiBlock multiBlock = MultiBlockManager.findMultiBlock(world, pos);
        if (multiBlock == null || multiBlock.isDisposed()) {
            LOGGER.debug("No MultiBlock found at {} during neighbor update", pos);
            return;
        }

        // 检查方块堆的完整性
        if (!multiBlock.checkIntegrity()) {
            LOGGER.info("MultiBlock at {} lost integrity due to neighbor update, splitting...",
                    multiBlock.getMasterPos());
            List<MultiBlock> newBlocks = multiBlock.checkAndSplitIntegrity();
            updateBlockEntityReferences(world, newBlocks);

            // 尝试合并新创建的方块堆
            for (MultiBlock newBlock : newBlocks) {
                performMultiRoundMerging(newBlock);
            }
        }
    }

    /**
     * 执行多轮合并操作，直到无法再合并为止。
     *
     * @param initialBlock 初始的多方块结构
     */
    public static void performMultiRoundMerging(MultiBlock initialBlock) {
        Objects.requireNonNull(initialBlock, "Initial block cannot be null");

        if (initialBlock.isDisposed()) {
            return;
        }

        MultiBlock current = initialBlock;
        boolean mergedInThisRound;
        int roundCount = 0;

        do {
            mergedInThisRound = false;
            roundCount++;

            List<MultiBlock> neighbors = findAdjacentMultiBlocks(current);
            LOGGER.debug("Round {}: Found {} adjacent MultiBlocks to merge with",
                    roundCount, neighbors.size());

            for (MultiBlock neighbor : neighbors) {
                if (neighbor.isDisposed() || neighbor == current) {
                    continue;
                }

                try {
                    LOGGER.debug("Attempting to merge MultiBlock at {} with neighbor at {}",
                            current.getMasterPos(), neighbor.getMasterPos());
                    MultiBlock merged = MultiBlock.combine(current, neighbor);

                    if (merged != null) {
                        // 合并成功，更新引用
                        updateBlockEntityReferences((World) merged.getWorld(), List.of(merged));

                        current = merged;
                        mergedInThisRound = true;
                        LOGGER.info("Successfully merged MultiBlocks into new MultiBlock at {}",
                                merged.getMasterPos());

                        // 合并后重新查找邻居，因为情况可能发生了变化
                        break;
                    } else {
                        LOGGER.debug("Merge operation returned null, merge failed");
                    }

                } catch (IllegalArgumentException e) {
                    LOGGER.debug("Cannot merge MultiBlocks: {}", e.getMessage());
                    // 继续尝试与其他邻居合并
                } catch (Exception e) {
                    LOGGER.error("Unexpected error during merge: {}", e.getMessage());
                    // 发生意外错误，停止合并
                    break;
                }
            }

            if (roundCount >= MAX_MERGE_ROUNDS) {
                LOGGER.warn("Reached maximum merge rounds for MultiBlock at {}",
                        current.getMasterPos());
                break;
            }

        } while (mergedInThisRound);

        LOGGER.debug("Completed merging after {} rounds for MultiBlock at {}",
                roundCount, current.getMasterPos());
    }

    /**
     * 批量更新多个多方块结构中所有方块实体的引用。
     *
     * @param world        世界实例
     * @param multiBlocks  需要更新引用的多方块结构列表
     */
    public static void updateBlockEntityReferences(World world, List<MultiBlock> multiBlocks) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(multiBlocks, "MultiBlocks list cannot be null");

        for (MultiBlock multiBlock : multiBlocks) {
            if (multiBlock.isDisposed()) {
                continue;
            }

            updateBlockEntityReferencesForMultiBlock(world, multiBlock);
        }
    }

    /**
     * 更新单个多方块结构中所有方块实体的引用。
     *
     * @param world       世界实例
     * @param multiBlock  需要更新引用的多方块结构
     */
    public static void updateBlockEntityReferencesForMultiBlock(World world, MultiBlock multiBlock) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(multiBlock, "MultiBlock cannot be null");

        if (multiBlock.isDisposed()) {
            throw new IllegalStateException("Cannot update references for disposed MultiBlock");
        }

        MultiBlock.PatternRange range = multiBlock.getRange();
        BlockPos start = range.getStart();
        BlockPos end = multiBlock.getEndPos();

        int updatedCount = 0;
        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int y = start.getY(); y <= end.getY(); y++) {
                for (int z = start.getZ(); z <= end.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (updateBlockEntityReference(world, pos, multiBlock)) {
                        updatedCount++;
                    }
                }
            }
        }

        LOGGER.debug("Updated {} block entity references for MultiBlock at {}",
                updatedCount, multiBlock.getMasterPos());
    }

    /**
     * 更新指定位置的方块实体引用。
     *
     * @param world       世界实例
     * @param pos         方块位置
     * @param multiBlock  关联的多方块结构
     * @return 如果成功更新了引用返回true，否则返回false
     */
    public static boolean updateBlockEntityReference(World world, BlockPos pos, MultiBlock multiBlock) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(pos, "Position cannot be null");
        Objects.requireNonNull(multiBlock, "MultiBlock cannot be null");

        // 只在服务端处理
        if (world.isClient) {
            return false;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MultiBlockEntity multiBlockEntity) {
            // 保存旧引用用于通知
             MultiBlockReference oldReference = multiBlockEntity.getMultiBlockReference();

            // 创建新的引用
            ServerMultiBlockReference newRef = ServerMultiBlockReference.fromWorldPos(multiBlock, pos);
            if (newRef != null) {
                multiBlockEntity.setMultiBlockReference(newRef);
                LOGGER.debug("Updated MultiBlock reference for MultiBlockEntity at {}", pos);

                // 通知引用变化
                multiBlockEntity.onMultiBlockChanged(oldReference, newRef);
                return true;
            } else {
                LOGGER.warn("Failed to create MultiBlockReference for MultiBlockEntity at {}", pos);
            }
        }
        return false;
    }

    /**
     * 强制更新指定区域内所有方块实体的多方块引用。
     *
     * @param world   世界实例
     * @param center  区域中心位置
     * @param radius  扫描半径（曼哈顿距离）
     */
    public static void forceUpdateReferencesInArea(World world, BlockPos center, int radius) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(center, "Center position cannot be null");

        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }

        LOGGER.info("Forcing update of MultiBlock references in area around {} with radius {}", center, radius);

        // 获取区域内的所有实现了IMultiBlockEntity的方块实体
        List<BlockPos> multiBlockEntities = new ArrayList<>();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity instanceof MultiBlockEntity) {
                        multiBlockEntities.add(pos);
                    }
                }
            }
        }

        LOGGER.debug("Found {} MultiBlockEntity instances in area", multiBlockEntities.size());

        // 为每个方块实体重新创建引用
        int updatedCount = 0;
        int clearedCount = 0;

        for (BlockPos pos : multiBlockEntities) {
            MultiBlock multiBlock = MultiBlockManager.findMultiBlock(world, pos);
            if (multiBlock != null && !multiBlock.isDisposed()) {
                if (updateBlockEntityReference(world, pos, multiBlock)) {
                    updatedCount++;
                }
            } else {
                // 如果没有找到方块堆，清除引用
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof MultiBlockEntity multiBlockEntity) {
                    MultiBlockReference oldReference = multiBlockEntity.getMultiBlockReference();
                    multiBlockEntity.setMultiBlockReference(null);
                    multiBlockEntity.onMultiBlockChanged(oldReference, null);
                    clearedCount++;
                    LOGGER.debug("Cleared MultiBlock reference at {}", pos);
                }
            }
        }

        LOGGER.info("Completed forced update: {} references updated, {} references cleared",
                updatedCount, clearedCount);
    }

    /**
     * 清除指定位置的多方块引用。
     * 用于当方块实体需要明确断开与多方块的关联时。
     *
     * @param world 世界实例
     * @param pos   方块位置
     */
    public static void clearMultiBlockReference(World world, BlockPos pos) {
        Objects.requireNonNull(world, "World cannot be null");
        Objects.requireNonNull(pos, "Position cannot be null");

        if (world.isClient) {
            return;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MultiBlockEntity multiBlockEntity) {
            MultiBlockReference oldReference = multiBlockEntity.getMultiBlockReference();
            multiBlockEntity.setMultiBlockReference(null);
            multiBlockEntity.onMultiBlockChanged(oldReference, null);
            LOGGER.debug("Cleared MultiBlock reference at {}", pos);
        }
    }

    /**
     * 创建单方块多方块结构。
     */
    public static MultiBlock createSingleBlockMultiBlock(World world, BlockPos pos, Block coreBlock) {
        try {
            return MultiBlock.builder()
                    .world(world)
                    .baseBlock(coreBlock)
                    .range(pos, 1, 1, 1)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Failed to create single block MultiBlock at {}: {}", pos, e.getMessage());
            return null;
        }
    }

    /**
     * 查找与指定多方块结构相邻的所有其他结构。
     */
    public static List<MultiBlock> findAdjacentMultiBlocks(MultiBlock multiBlock) {
        List<MultiBlock> neighbors = new ArrayList<>();
        World world = (World) multiBlock.getWorld();
        MultiBlock.PatternRange range = multiBlock.getRange();
        BlockPos start = range.getStart();
        BlockPos end = range.getEnd();

        // 检查六个方向的相邻位置
        // 西方向
        BlockPos westFaceStart = start.west();
        BlockPos westFaceEnd = new BlockPos(start.getX() - 1, end.getY(), end.getZ());
        checkDirection(neighbors, world, multiBlock, westFaceStart, westFaceEnd);

        // 东方向
        BlockPos eastFaceStart = end.east();
        BlockPos eastFaceEnd = new BlockPos(end.getX() + 1, end.getY(), end.getZ());
        checkDirection(neighbors, world, multiBlock, eastFaceStart, eastFaceEnd);

        // 下方向
        BlockPos downFaceStart = start.down();
        BlockPos downFaceEnd = new BlockPos(end.getX(), start.getY() - 1, end.getZ());
        checkDirection(neighbors, world, multiBlock, downFaceStart, downFaceEnd);

        // 上方向
        BlockPos upFaceStart = end.up();
        BlockPos upFaceEnd = new BlockPos(end.getX(), end.getY() + 1, end.getZ());
        checkDirection(neighbors, world, multiBlock, upFaceStart, upFaceEnd);

        // 北方向
        BlockPos northFaceStart = start.north();
        BlockPos northFaceEnd = new BlockPos(end.getX(), end.getY(), start.getZ() - 1);
        checkDirection(neighbors, world, multiBlock, northFaceStart, northFaceEnd);

        // 南方向
        BlockPos southFaceStart = end.south();
        BlockPos southFaceEnd = new BlockPos(end.getX(), end.getY(), end.getZ() + 1);
        checkDirection(neighbors, world, multiBlock, southFaceStart, southFaceEnd);

        return neighbors;
    }

    /**
     * 在指定方向上查找相邻的多方块结构。
     */
    public static void checkDirection(List<MultiBlock> neighbors, World world, MultiBlock multiBlock,
                                       BlockPos faceStart, BlockPos faceEnd) {
        // 检查该方向上所有可能的位置
        for (int x = faceStart.getX(); x <= faceEnd.getX(); x++) {
            for (int y = faceStart.getY(); y <= faceEnd.getY(); y++) {
                for (int z = faceStart.getZ(); z <= faceEnd.getZ(); z++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    MultiBlock neighbor = MultiBlockManager.findMultiBlock(world, checkPos);

                    if (neighbor != null && !neighbor.isDisposed() &&
                            neighbor.getBaseBlock() == multiBlock.getBaseBlock() &&
                            !neighbors.contains(neighbor)) {
                        neighbors.add(neighbor);
                    }
                }
            }
        }
    }
}