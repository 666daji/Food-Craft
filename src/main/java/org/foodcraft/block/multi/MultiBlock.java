package org.foodcraft.block.multi;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.foodcraft.FoodCraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class MultiBlock implements AutoCloseable {
    private static final Logger LOGGER = FoodCraft.LOGGER;

    // 最大方块堆大小限制
    public static final int MAX_SIZE = 10;

    private final WorldView world;
    private final Block baseBlock;
    private final PatternRange range;
    private final BlockPos masterPos;
    private boolean disposed = false;

    // 私有构造函数，通过建造者创建实例
    private MultiBlock(WorldView world, Block baseBlock, PatternRange range) {
        this.world = Objects.requireNonNull(world, "World cannot be null");
        this.baseBlock = Objects.requireNonNull(baseBlock, "Base block cannot be null");
        this.range = Objects.requireNonNull(range, "Range cannot be null");
        this.masterPos = range.getStart();

        // 检查大小限制
        if (range.getWidth() > MAX_SIZE || range.getHeight() > MAX_SIZE || range.getDepth() > MAX_SIZE) {
            throw new IllegalArgumentException(
                    String.format("MultiBlock size %dx%dx%d exceeds maximum allowed size %dx%dx%d",
                            range.getWidth(), range.getHeight(), range.getDepth(),
                            MAX_SIZE, MAX_SIZE, MAX_SIZE)
            );
        }

        // 注册到管理器
        if (!MultiBlockManager.registerMultiBlock(this)) {
            throw new IllegalStateException("Failed to register MultiBlock at position " + masterPos +
                    ". Position already occupied by another MultiBlock.");
        }

        LOGGER.debug("Created new MultiBlock at {} with base block {} and size {}x{}x{}",
                masterPos, baseBlock, range.getWidth(), range.getHeight(), range.getDepth());
    }

    public static class Builder {
        private WorldView world;
        private Block baseBlock;
        private PatternRange range;

        public Builder world(WorldView world) {
            this.world = world;
            return this;
        }

        public Builder baseBlock(Block baseBlock) {
            this.baseBlock = baseBlock;
            return this;
        }

        public Builder range(PatternRange range) {
            this.range = range;
            return this;
        }

        public Builder range(BlockPos start, int width, int height, int depth) {
            // 在构建时检查大小限制
            if (width > MAX_SIZE || height > MAX_SIZE || depth > MAX_SIZE) {
                throw new IllegalArgumentException(
                        String.format("Requested size %dx%dx%d exceeds maximum allowed size %dx%dx%d",
                                width, height, depth, MAX_SIZE, MAX_SIZE, MAX_SIZE)
                );
            }
            this.range = new PatternRange(start, width, height, depth);
            return this;
        }

        public MultiBlock build() {
            return new MultiBlock(world, baseBlock, range);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 检查方块堆的完整性，如果不完整则自动拆分
     * @return 拆分后的新方块堆列表（如果未拆分则返回空列表）
     */
    public List<MultiBlock> checkAndSplitIntegrity() {
        if (disposed) {
            LOGGER.warn("Attempted to check integrity of disposed MultiBlock at {}", masterPos);
            return Collections.emptyList();
        }

        List<BlockPos> validBlocks = findValidBlocks();

        // 如果所有方块都有效，返回空列表（不需要拆分）
        if (validBlocks.size() == getVolume()) {
            LOGGER.debug("MultiBlock at {} is intact, no need to split", masterPos);
            return Collections.emptyList();
        }

        LOGGER.info("MultiBlock at {} is incomplete. Valid blocks: {}/{}. Splitting...",
                masterPos, validBlocks.size(), getVolume());

        // 拆分方块堆
        List<MultiBlock> newMultiBlocks = splitMultiBlock(validBlocks);

        // 注销当前方块堆
        this.dispose();

        LOGGER.info("Split MultiBlock at {} into {} new MultiBlocks",
                masterPos, newMultiBlocks.size());

        return newMultiBlocks;
    }

    /**
     * 找到所有有效的方块位置
     */
    private List<BlockPos> findValidBlocks() {
        List<BlockPos> validBlocks = new ArrayList<>();
        BlockPos start = range.getStart();
        BlockPos end = getEndPos();

        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int y = start.getY(); y <= end.getY(); y++) {
                for (int z = start.getZ(); z <= end.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    if (block == baseBlock) {
                        validBlocks.add(pos);
                    }
                }
            }
        }

        return validBlocks;
    }

    /**
     * 将不完整的方块堆拆分成多个完整的小方块堆
     */
    private List<MultiBlock> splitMultiBlock(List<BlockPos> validBlocks) {
        if (validBlocks.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用三维连通组件算法找到连续的方块区域
        List<List<BlockPos>> connectedComponents = findConnectedComponents(validBlocks);

        List<MultiBlock> result = new ArrayList<>();

        for (List<BlockPos> component : connectedComponents) {
            if (!component.isEmpty()) {
                MultiBlock newMultiBlock = createMultiBlockFromComponent(component);
                if (newMultiBlock != null) {
                    result.add(newMultiBlock);
                }
            }
        }

        return result;
    }

    /**
     * 使用广度优先搜索找到连通的方块组件
     */
    private List<List<BlockPos>> findConnectedComponents(List<BlockPos> validBlocks) {
        Set<BlockPos> visited = new HashSet<>();
        List<List<BlockPos>> components = new ArrayList<>();

        // 将有效方块转换为集合以便快速查找
        Set<BlockPos> validSet = new HashSet<>(validBlocks);

        // 定义六个方向：上下左右前后
        int[][] directions = {
                {1, 0, 0}, {-1, 0, 0},  // 东西
                {0, 1, 0}, {0, -1, 0},  // 上下
                {0, 0, 1}, {0, 0, -1}   // 南北
        };

        for (BlockPos block : validBlocks) {
            if (!visited.contains(block)) {
                List<BlockPos> component = new ArrayList<>();
                Queue<BlockPos> queue = new LinkedList<>();

                queue.add(block);
                visited.add(block);
                component.add(block);

                while (!queue.isEmpty()) {
                    BlockPos current = queue.poll();

                    for (int[] dir : directions) {
                        BlockPos neighbor = new BlockPos(
                                current.getX() + dir[0],
                                current.getY() + dir[1],
                                current.getZ() + dir[2]
                        );

                        if (validSet.contains(neighbor) && !visited.contains(neighbor)) {
                            visited.add(neighbor);
                            component.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }

                components.add(component);
            }
        }

        return components;
    }

    /**
     * 从连通的方块组件创建新的方块堆
     */
    private MultiBlock createMultiBlockFromComponent(List<BlockPos> component) {
        if (component.isEmpty()) {
            return null;
        }

        // 找到组件的最小和最大坐标
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : component) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        // 检查矩形区域内的所有方块是否都是有效的
        // 如果不是，我们需要进一步拆分或者只创建包含实际方块的矩形
        if (!isRectangularRegionValid(component, minX, minY, minZ, maxX, maxY, maxZ)) {
            // 如果不是完整的矩形，创建包含所有方块的最小矩形
            return createMinimalMultiBlock(component);
        }

        // 创建完整的矩形方块堆
        BlockPos newStart = new BlockPos(minX, minY, minZ);
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;

        try {
            return MultiBlock.builder()
                    .world(world)
                    .baseBlock(baseBlock)
                    .range(newStart, width, height, depth)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Failed to create MultiBlock from component: {}", e.getMessage());
            return createMinimalMultiBlock(component);
        }
    }

    /**
     * 检查矩形区域是否完全由有效方块填充
     */
    private boolean isRectangularRegionValid(List<BlockPos> component, int minX, int minY, int minZ,
                                             int maxX, int maxY, int maxZ) {
        // 将组件转换为集合以便快速查找
        Set<BlockPos> componentSet = new HashSet<>(component);

        // 检查矩形区域内的每个位置是否都在组件中
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!componentSet.contains(pos)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * 创建包含所有方块的最小矩形方块堆
     * 这会创建一个可能包含空洞的矩形，但保证所有有效方块都在其中
     */
    private MultiBlock createMinimalMultiBlock(List<BlockPos> component) {
        if (component.isEmpty()) {
            return null;
        }

        // 找到组件的最小和最大坐标
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : component) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        BlockPos newStart = new BlockPos(minX, minY, minZ);
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;

        try {
            MultiBlock multiBlock = MultiBlock.builder()
                    .world(world)
                    .baseBlock(baseBlock)
                    .range(newStart, width, height, depth)
                    .build();

            // 记录警告，因为这个方块堆可能包含空洞
            LOGGER.warn("Created non-solid MultiBlock at {} with size {}x{}x{} containing {} blocks",
                    newStart, width, height, depth, component.size());

            return multiBlock;
        } catch (Exception e) {
            LOGGER.error("Failed to create minimal MultiBlock: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 手动拆分方块堆为两个部分（沿着指定的平面）
     * @param axis 拆分轴：'x', 'y', 或 'z'
     * @param splitPosition 在指定轴上的拆分位置（相对坐标）
     * @return 拆分后的两个方块堆，如果拆分失败返回空列表
     */
    public List<MultiBlock> splitAlongPlane(char axis, int splitPosition) {
        if (disposed) {
            LOGGER.warn("Attempted to split disposed MultiBlock at {}", masterPos);
            return Collections.emptyList();
        }

        if (splitPosition <= 0 || splitPosition >= getAxisSize(axis)) {
            LOGGER.error("Invalid split position {} for axis {}", splitPosition, axis);
            return Collections.emptyList();
        }

        try {
            BlockPos start = range.getStart();
            int width = range.getWidth();
            int height = range.getHeight();
            int depth = range.getDepth();

            MultiBlock firstPart, secondPart;

            switch (axis) {
                case 'x':
                    firstPart = createSubMultiBlock(start, splitPosition, height, depth);
                    BlockPos secondStart = new BlockPos(start.getX() + splitPosition, start.getY(), start.getZ());
                    secondPart = createSubMultiBlock(secondStart, width - splitPosition, height, depth);
                    break;

                case 'y':
                    firstPart = createSubMultiBlock(start, width, splitPosition, depth);
                    BlockPos secondStartY = new BlockPos(start.getX(), start.getY() + splitPosition, start.getZ());
                    secondPart = createSubMultiBlock(secondStartY, width, height - splitPosition, depth);
                    break;

                case 'z':
                    firstPart = createSubMultiBlock(start, width, height, splitPosition);
                    BlockPos secondStartZ = new BlockPos(start.getX(), start.getY(), start.getZ() + splitPosition);
                    secondPart = createSubMultiBlock(secondStartZ, width, height, depth - splitPosition);
                    break;

                default:
                    LOGGER.error("Invalid axis: {}", axis);
                    return Collections.emptyList();
            }

            if (firstPart != null && secondPart != null) {
                // 注销当前方块堆
                this.dispose();

                LOGGER.info("Split MultiBlock at {} along {} axis at position {}",
                        masterPos, axis, splitPosition);

                return Arrays.asList(firstPart, secondPart);
            }
        } catch (Exception e) {
            LOGGER.error("Error splitting MultiBlock: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * 获取指定轴的大小
     */
    private int getAxisSize(char axis) {
        return switch (axis) {
            case 'x' -> range.getWidth();
            case 'y' -> range.getHeight();
            case 'z' -> range.getDepth();
            default -> 0;
        };
    }

    /**
     * 创建子方块堆
     */
    private MultiBlock createSubMultiBlock(BlockPos start, int width, int height, int depth) {
        if (width <= 0 || height <= 0 || depth <= 0) {
            return null;
        }

        try {
            return MultiBlock.builder()
                    .world(world)
                    .baseBlock(baseBlock)
                    .range(start, width, height, depth)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Failed to create sub MultiBlock: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查方块堆的完整性（所有方块是否都是同种方块）
     */
    public boolean checkIntegrity() {
        if (disposed) {
            LOGGER.warn("Attempted to check integrity of disposed MultiBlock at {}", masterPos);
            return false;
        }

        BlockPos start = range.getStart();
        BlockPos end = getEndPos();

        LOGGER.debug("Checking integrity of MultiBlock at {} to {}", start, end);

        int invalidCount = 0;
        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int y = start.getY(); y <= end.getY(); y++) {
                for (int z = start.getZ(); z <= end.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    if (block != baseBlock) {
                        invalidCount++;
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Found invalid block at {}: expected {}, found {}",
                                    pos, baseBlock, block);
                        }
                    }
                }
            }
        }

        if (invalidCount > 0) {
            LOGGER.warn("MultiBlock at {} has {} invalid blocks out of {}",
                    masterPos, invalidCount, getVolume());
            return false;
        }

        LOGGER.debug("MultiBlock at {} integrity check passed", masterPos);
        return true;
    }

    /**
     * 根据相对位置计算世界坐标
     */
    public BlockPos getWorldPos(int relativeX, int relativeY, int relativeZ) {
        if (disposed) {
            throw new IllegalStateException("MultiBlock at " + masterPos + " has been disposed");
        }

        if (relativeX < 0 || relativeX >= range.getWidth() ||
                relativeY < 0 || relativeY >= range.getHeight() ||
                relativeZ < 0 || relativeZ >= range.getDepth()) {
            String errorMsg = String.format("Relative coordinates (%d, %d, %d) out of range. Valid range: [0-%d, 0-%d, 0-%d]",
                    relativeX, relativeY, relativeZ,
                    range.getWidth() - 1, range.getHeight() - 1, range.getDepth() - 1);
            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        return new BlockPos(
                range.getStart().getX() + relativeX,
                range.getStart().getY() + relativeY,
                range.getStart().getZ() + relativeZ
        );
    }

    /**
     * 获取相对坐标对应的世界坐标
     */
    public BlockPos getWorldPos(BlockPos relativePos) {
        return getWorldPos(relativePos.getX(), relativePos.getY(), relativePos.getZ());
    }

    /**
     * 实例方法：将当前方块堆与另一个方块堆拼接
     */
    public MultiBlock combineWith(@NotNull MultiBlock other) {
        if (disposed) {
            throw new IllegalStateException("This MultiBlock at " + masterPos + " has been disposed");
        }
        if (other.disposed) {
            throw new IllegalStateException("Other MultiBlock at " + other.masterPos + " has been disposed");
        }

        LOGGER.debug("Attempting to combine MultiBlock at {} with MultiBlock at {}",
                this.masterPos, other.masterPos);
        return combine(this, other);
    }

    /**
     * 拼接两个方块堆
     */
    public static MultiBlock combine(@NotNull MultiBlock first, @NotNull MultiBlock second) {
        if (first.disposed || second.disposed) {
            throw new IllegalStateException("Cannot combine disposed MultiBlocks");
        }

        // 检查基础条件
        if (first.world != second.world) {
            String errorMsg = "MultiBlocks must be in the same world";
            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        if (first.baseBlock != second.baseBlock) {
            String errorMsg = "MultiBlocks must have the same base block: " +
                    first.baseBlock + " vs " + second.baseBlock;
            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        PatternRange firstRange = first.range;
        PatternRange secondRange = second.range;

        // 检查合并后的大小是否超过限制
        BlockPos newStart = new BlockPos(
                Math.min(firstRange.getStart().getX(), secondRange.getStart().getX()),
                Math.min(firstRange.getStart().getY(), secondRange.getStart().getY()),
                Math.min(firstRange.getStart().getZ(), secondRange.getStart().getZ())
        );

        BlockPos firstEnd = first.getEndPos();
        BlockPos secondEnd = second.getEndPos();

        BlockPos newEnd = new BlockPos(
                Math.max(firstEnd.getX(), secondEnd.getX()),
                Math.max(firstEnd.getY(), secondEnd.getY()),
                Math.max(firstEnd.getZ(), secondEnd.getZ())
        );

        int newWidth = newEnd.getX() - newStart.getX() + 1;
        int newHeight = newEnd.getY() - newStart.getY() + 1;
        int newDepth = newEnd.getZ() - newStart.getZ() + 1;

        if (newWidth > MAX_SIZE || newHeight > MAX_SIZE || newDepth > MAX_SIZE) {
            String errorMsg = String.format("Combined MultiBlock size %dx%dx%d would exceed maximum allowed size %dx%dx%d",
                    newWidth, newHeight, newDepth, MAX_SIZE, MAX_SIZE, MAX_SIZE);
            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // 检查是否相邻或重叠，并确定合并方向
        MergeDirection direction = findMergeDirection(firstRange, secondRange);
        if (direction == null) {
            String errorMsg = String.format("MultiBlocks are not adjacent or overlapping in a valid way. " +
                    "First: %s, Second: %s", firstRange, secondRange);
            LOGGER.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        LOGGER.debug("Merging MultiBlocks in direction: {}", direction);

        // 计算合并后的新范围
        PatternRange newRange = new PatternRange(newStart, newWidth, newHeight, newDepth);

        // 创建新的合并后的MultiBlock
        MultiBlock combined = new MultiBlock(first.world, first.baseBlock, newRange);

        // 注销原来的两个MultiBlock
        first.dispose();
        second.dispose();

        LOGGER.info("Successfully combined MultiBlocks at {} and {} into new MultiBlock at {}",
                first.masterPos, second.masterPos, combined.masterPos);

        return combined;
    }

    /**
     * 安全地销毁这个MultiBlock实例（实现AutoCloseable接口）
     */
    @Override
    public void close() {
        dispose();
    }

    /**
     * 安全地销毁这个MultiBlock实例
     */
    public void dispose() {
        if (!disposed) {
            LOGGER.debug("Disposing MultiBlock at {}", masterPos);
            MultiBlockManager.unregisterMultiBlock(this);
            disposed = true;
        }
    }

    /**
     * 检查是否已被销毁
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * 获取方块堆的结束坐标（东南偏上位置）
     */
    public BlockPos getEndPos() {
        if (disposed) {
            throw new IllegalStateException("MultiBlock at " + masterPos + " has been disposed");
        }
        return range.getEnd();
    }

    /**
     * 获取方块堆的体积（方块数量）
     */
    public int getVolume() {
        return disposed ? 0 : range.getVolume();
    }

    // Getter方法
    public WorldView getWorld() {
        if (disposed) {
            throw new IllegalStateException("MultiBlock at " + masterPos + " has been disposed");
        }
        return world;
    }

    public Block getBaseBlock() {
        if (disposed) {
            throw new IllegalStateException("MultiBlock at " + masterPos + " has been disposed");
        }
        return baseBlock;
    }

    public PatternRange getRange() {
        if (disposed) {
            throw new IllegalStateException("MultiBlock at " + masterPos + " has been disposed");
        }
        return range;
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    /**
     * 为当前方块堆中的特定相对位置创建引用
     */
    public MultiBlockReference createReference(BlockPos relativePos) {
        return MultiBlockReference.fromRelativePos(this, relativePos);
    }

    /**
     * 为当前方块堆中的特定相对位置创建引用
     */
    public MultiBlockReference createReference(int relativeX, int relativeY, int relativeZ) {
        return createReference(new BlockPos(relativeX, relativeY, relativeZ));
    }

    /**
     * 为当前方块堆中的世界位置创建引用
     */
    @Nullable
    public MultiBlockReference createReferenceFromWorldPos(BlockPos worldPos) {
        return MultiBlockReference.fromWorldPos(this, worldPos);
    }

    /**
     * 检查指定的世界位置是否在当前方块堆范围内
     */
    public boolean containsWorldPos(BlockPos worldPos) {
        return getRange().contains(worldPos);
    }

    /**
     * 获取世界位置对应的相对位置
     */
    @Nullable
    public BlockPos getRelativePosFromWorld(BlockPos worldPos) {
        if (!containsWorldPos(worldPos)) {
            return null;
        }

        return new BlockPos(
                worldPos.getX() - getMasterPos().getX(),
                worldPos.getY() - getMasterPos().getY(),
                worldPos.getZ() - getMasterPos().getZ()
        );
    }

    // 内部辅助方法
    private static MergeDirection findMergeDirection(PatternRange first, PatternRange second) {
        BlockPos firstEnd = first.getEnd();
        BlockPos secondEnd = second.getEnd();

        // 检查X轴方向合并（东西方向）
        if (first.getStart().getY() == second.getStart().getY() && firstEnd.getY() == secondEnd.getY() &&
                first.getStart().getZ() == second.getStart().getZ() && firstEnd.getZ() == secondEnd.getZ()) {

            if (firstEnd.getX() + 1 == second.getStart().getX()) {
                return MergeDirection.EAST_WEST;
            } else if (secondEnd.getX() + 1 == first.getStart().getX()) {
                return MergeDirection.WEST_EAST;
            } else if (first.getStart().getX() <= secondEnd.getX() && firstEnd.getX() >= second.getStart().getX()) {
                return MergeDirection.OVERLAP_X;
            }
        }

        // 检查Y轴方向合并（上下方向）
        if (first.getStart().getX() == second.getStart().getX() && firstEnd.getX() == secondEnd.getX() &&
                first.getStart().getZ() == second.getStart().getZ() && firstEnd.getZ() == secondEnd.getZ()) {

            if (firstEnd.getY() + 1 == second.getStart().getY()) {
                return MergeDirection.UP_DOWN;
            } else if (secondEnd.getY() + 1 == first.getStart().getY()) {
                return MergeDirection.DOWN_UP;
            } else if (first.getStart().getY() <= secondEnd.getY() && firstEnd.getY() >= second.getStart().getY()) {
                return MergeDirection.OVERLAP_Y;
            }
        }

        // 检查Z轴方向合并（南北方向）
        if (first.getStart().getX() == second.getStart().getX() && firstEnd.getX() == secondEnd.getX() &&
                first.getStart().getY() == second.getStart().getY() && firstEnd.getY() == secondEnd.getY()) {

            if (firstEnd.getZ() + 1 == second.getStart().getZ()) {
                return MergeDirection.SOUTH_NORTH;
            } else if (secondEnd.getZ() + 1 == first.getStart().getZ()) {
                return MergeDirection.NORTH_SOUTH;
            } else if (first.getStart().getZ() <= secondEnd.getZ() && firstEnd.getZ() >= second.getStart().getZ()) {
                return MergeDirection.OVERLAP_Z;
            }
        }

        return null;
    }

    public static final class PatternRange {
        private final BlockPos start;
        private final int width;
        private final int height;
        private final int depth;
        private final BlockPos end;
        private final int volume;

        public PatternRange(BlockPos start, int width, int height, int depth) {
            if (width <= 0 || height <= 0 || depth <= 0) {
                throw new IllegalArgumentException("Dimensions must be positive: width=" + width +
                        ", height=" + height + ", depth=" + depth);
            }

            this.start = start;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.end = new BlockPos(
                    start.getX() + width - 1,
                    start.getY() + height - 1,
                    start.getZ() + depth - 1
            );
            this.volume = width * height * depth;
        }

        public PatternRange(BlockPos start, int width, int height) {
            this(start, width, height, 1);
        }

        public PatternRange(BlockPos start) {
            this(start, 1, 1);
        }

        public boolean isSquare() {
            return width == height;
        }

        public boolean isCubes() {
            return this.isSquare() && width == depth;
        }

        public boolean contains(BlockPos worldPos) {
            return worldPos.getX() >= start.getX() && worldPos.getX() <= end.getX() &&
                    worldPos.getY() >= start.getY() && worldPos.getY() <= end.getY() &&
                    worldPos.getZ() >= start.getZ() && worldPos.getZ() <= end.getZ();
        }

        // Getter 方法
        public BlockPos getStart() { return start; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getDepth() { return depth; }
        public BlockPos getEnd() { return end; }
        public int getVolume() { return volume; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PatternRange that = (PatternRange) obj;
            return width == that.width && height == that.height && depth == that.depth &&
                    Objects.equals(start, that.start);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, width, height, depth);
        }

        @Override
        public String toString() {
            return String.format("PatternRange{start=%s, width=%d, height=%d, depth=%d}",
                    start, width, height, depth);
        }
    }

    /**
     * 合并方向枚举
     */
    private enum MergeDirection {
        EAST_WEST,    // 第一个在东，第二个在西
        WEST_EAST,    // 第一个在西，第二个在东
        UP_DOWN,      // 第一个在上，第二个在下
        DOWN_UP,      // 第一个在下，第二个在上
        SOUTH_NORTH,  // 第一个在南，第二个在北
        NORTH_SOUTH,  // 第一个在北，第二个在南
        OVERLAP_X,    // X轴重叠
        OVERLAP_Y,    // Y轴重叠
        OVERLAP_Z     // Z轴重叠
    }
}