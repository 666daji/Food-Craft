package org.foodcraft.block.multi;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.entity.HeatResistantSlateBlockEntity;
import org.slf4j.Logger;

import java.util.*;

/**
 * 提供方块堆操作的辅助类，专门用于处理核心方块的放置和破坏逻辑
 */
public class MultiBlockHelper {
    private static final Logger LOGGER = FoodCraft.LOGGER;

    /**
     * 处理核心方块放置时的逻辑
     */
    public static void onCoreBlockPlaced(World world, BlockPos pos, Block coreBlock) {
        LOGGER.debug("Core block placed at {}", pos);

        // 1. 首先检查该位置是否已经有方块堆
        MultiBlock existing = MultiBlockManager.findMultiBlock(world, pos);
        if (existing != null && !existing.isDisposed()) {
            // 位置已有方块堆，检查完整性
            LOGGER.debug("Found existing MultiBlock at {}, checking integrity", pos);
            List<MultiBlock> newBlocks = existing.checkAndSplitIntegrity();
            updateBlockEntityReferences(world, newBlocks);
            LOGGER.info("After integrity check, created {} new MultiBlocks", newBlocks.size());
            return;
        }

        // 2. 创建新的单方块堆
        MultiBlock newMultiBlock = createSingleBlockMultiBlock(world, pos, coreBlock);
        if (newMultiBlock == null) {
            LOGGER.error("Failed to create single block MultiBlock at {}", pos);
            return;
        }

        // 3. 更新当前方块的引用
        updateBlockEntityReference(world, pos, newMultiBlock);

        // 4. 尝试与相邻的方块堆合并（多轮合并直到无法再合并）
        performMultiRoundMerging(newMultiBlock);
    }

    /**
     * 处理核心方块破坏时的逻辑
     */
    public static void onCoreBlockBroken(World world, BlockPos pos, Block coreBlock) {
        LOGGER.debug("Core block broken at {}", pos);

        // 1. 找到包含该位置的方块堆
        MultiBlock multiBlock = MultiBlockManager.findMultiBlock(world, pos);
        if (multiBlock == null || multiBlock.isDisposed()) {
            LOGGER.debug("No MultiBlock found at {}", pos);
            return;
        }

        // 2. 检查并拆分方块堆
        LOGGER.info("Breaking block at {} in MultiBlock {}, checking integrity",
                pos, multiBlock.getMasterPos());
        List<MultiBlock> newBlocks = multiBlock.checkAndSplitIntegrity();

        // 3. 更新所有新方块堆的引用
        updateBlockEntityReferences(world, newBlocks);

        LOGGER.info("After breaking, created {} new MultiBlocks", newBlocks.size());

        // 4. 尝试合并新创建的方块堆（多轮合并）
        for (MultiBlock newBlock : newBlocks) {
            performMultiRoundMerging(newBlock);
        }
    }

    /**
     * 处理相邻方块更新时的逻辑
     */
    public static void onNeighborUpdate(World world, BlockPos pos, Block coreBlock) {
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
     * 执行多轮合并，直到无法再合并为止
     */
    public static void performMultiRoundMerging(MultiBlock initialBlock) {
        if (initialBlock.isDisposed()) {
            return;
        }

        MultiBlock current = initialBlock;
        boolean mergedInThisRound;
        int roundCount = 0;
        final int MAX_ROUNDS = 10; // 防止无限循环

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

                    // 合并成功，更新引用
                    updateBlockEntityReferences((World) merged.getWorld(), List.of(merged));

                    current = merged;
                    mergedInThisRound = true;
                    LOGGER.info("Successfully merged MultiBlocks into new MultiBlock at {}",
                            merged.getMasterPos());

                    // 合并后重新查找邻居，因为情况可能发生了变化
                    break;

                } catch (IllegalArgumentException e) {
                    LOGGER.debug("Cannot merge MultiBlocks: {}", e.getMessage());
                    // 继续尝试与其他邻居合并
                }
            }

            if (roundCount >= MAX_ROUNDS) {
                LOGGER.warn("Reached maximum merge rounds for MultiBlock at {}",
                        current.getMasterPos());
                break;
            }

        } while (mergedInThisRound);

        LOGGER.debug("Completed merging after {} rounds for MultiBlock at {}",
                roundCount, current.getMasterPos());
    }

    /**
     * 更新多个方块堆的所有方块实体引用
     */
    public static void updateBlockEntityReferences(World world, List<MultiBlock> multiBlocks) {
        for (MultiBlock multiBlock : multiBlocks) {
            if (multiBlock.isDisposed()) {
                continue;
            }

            updateBlockEntityReferencesForMultiBlock(world, multiBlock);
        }
    }

    /**
     * 更新单个方块堆的所有方块实体引用
     */
    public static void updateBlockEntityReferencesForMultiBlock(World world, MultiBlock multiBlock) {
        MultiBlock.PatternRange range = multiBlock.getRange();
        BlockPos start = range.getStart();
        BlockPos end = multiBlock.getEndPos();

        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int y = start.getY(); y <= end.getY(); y++) {
                for (int z = start.getZ(); z <= end.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    updateBlockEntityReference(world, pos, multiBlock);
                }
            }
        }

        LOGGER.debug("Updated all block entity references for MultiBlock at {}",
                multiBlock.getMasterPos());
    }

    /**
     * 更新单个方块实体的引用
     */
    public static void updateBlockEntityReference(World world, BlockPos pos, MultiBlock multiBlock) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof HeatResistantSlateBlockEntity slateEntity) {

            // 创建新的引用
            MultiBlockReference newRef = MultiBlockReference.fromWorldPos(multiBlock, pos);
            if (newRef != null) {
                slateEntity.setMultiBlockReference(newRef);
                LOGGER.debug("Updated MultiBlock reference for block entity at {}", pos);
            } else {
                LOGGER.warn("Failed to create MultiBlockReference for block entity at {}", pos);
            }
        }
    }

    /**
     * 创建单方块堆
     */
    private static MultiBlock createSingleBlockMultiBlock(World world, BlockPos pos, Block coreBlock) {
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
     * 找到相邻的方块堆
     */
    private static List<MultiBlock> findAdjacentMultiBlocks(MultiBlock multiBlock) {
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
     * 检查特定方向的相邻方块堆
     */
    private static void checkDirection(List<MultiBlock> neighbors, World world, MultiBlock multiBlock,
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

    /**
     * 强制更新特定位置的所有方块堆引用
     * 用于调试或手动修复
     */
    public static void forceUpdateReferencesInArea(World world, BlockPos center, int radius) {
        LOGGER.info("Forcing update of MultiBlock references in area around {}", center);

        // 获取区域内的所有核心方块
        List<BlockPos> coreBlocks = new ArrayList<>();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity instanceof HeatResistantSlateBlockEntity) {
                        coreBlocks.add(pos);
                    }
                }
            }
        }

        LOGGER.debug("Found {} core blocks in area", coreBlocks.size());

        // 为每个核心方块重新创建引用
        for (BlockPos pos : coreBlocks) {
            MultiBlock multiBlock = MultiBlockManager.findMultiBlock(world, pos);
            if (multiBlock != null && !multiBlock.isDisposed()) {
                updateBlockEntityReference(world, pos, multiBlock);
            } else {
                // 如果没有找到方块堆，清除引用
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof HeatResistantSlateBlockEntity) {
                    ((HeatResistantSlateBlockEntity) blockEntity).setMultiBlockReference(null);
                    LOGGER.debug("Cleared MultiBlock reference at {}", pos);
                }
            }
        }

        LOGGER.info("Completed forced update of MultiBlock references");
    }
}