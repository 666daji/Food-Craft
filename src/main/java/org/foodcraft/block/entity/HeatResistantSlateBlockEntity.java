package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.block.HeatResistantSlateBlock;
import org.foodcraft.block.multi.MultiBlock;
import org.foodcraft.block.multi.MultiBlockReference;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.jetbrains.annotations.Nullable;

public class HeatResistantSlateBlockEntity extends BlockEntity {
    private static final int MIN_CHECK_INTERVAL = 10;

    private BlockPattern stove1x1;
    private BlockPattern stove1x2;
    private BlockPattern stove2x2;
    private BlockPattern stove2x3;

    private MultiBlockReference multiBlockRef;
    private BlockPattern.Result currentStoveResult;
    private int stoveStructureType = -1;
    private boolean isValidStove = false;

    public int age;
    private int lastCheckTime = 0;

    public HeatResistantSlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.HEAT_RESISTANT_SLATE, pos, state);
        this.initStovePattern();
    }

    public static void tick(World world, BlockPos pos, BlockState state, HeatResistantSlateBlockEntity blockEntity) {
        // 每tick增加方块寿命
        blockEntity.age++;
        if (blockEntity.age == Integer.MAX_VALUE) {
            blockEntity.age = 0;
        }

        // 尝试检查结构
        boolean interval = blockEntity.age - blockEntity.lastCheckTime < MIN_CHECK_INTERVAL;
        if (!interval) {
            blockEntity.checkPattern(world, pos, state);
            blockEntity.lastCheckTime = blockEntity.age;
        }
    }

    private boolean checkPattern(World world, BlockPos pos, BlockState state) {
        // 重置检查结果
        this.currentStoveResult = null;
        this.stoveStructureType = -1;
        this.isValidStove = false;

        // 如果没有多方块引用或不是主方块，不进行检查
        if (multiBlockRef == null || multiBlockRef.isDisposed() || !multiBlockRef.isMasterBlock()) {
            return false;
        }

        // 检查多方块结构是否有效
        if (!multiBlockRef.checkIntegrity()) {
            return false;
        }

        // 检查是否符合炉子结构要求
        if (!isValidStoveStructure()) {
            return false;
        }

        // 根据多方块尺寸获取对应的炉子图案类型
        int patternType = getStoveStructureType();
        if (patternType == -1) {
            return false;
        }

        // 获取对应的BlockPattern
        BlockPattern pattern = getStovePattern(patternType);
        if (pattern == null) {
            return false;
        }

        // 在周围搜索匹配的炉子结构
        // 使用主方块位置作为搜索起点
        BlockPos searchPos = multiBlockRef.getMasterWorldPos();
        BlockPattern.Result result = pattern.searchAround(world, searchPos);

        if (result != null) {
            this.currentStoveResult = result;
            this.stoveStructureType = patternType;
            this.isValidStove = true;

            // 结构匹配成功，可以执行炉子逻辑
            onStoveStructureValid(world, pos, result, patternType);
            return true;
        } else {
            // 结构不匹配
            onStoveStructureInvalid(world, pos);
            return false;
        }
    }

    /**
     * 当炉子结构有效时调用
     */
    private void onStoveStructureValid(World world, BlockPos pos, BlockPattern.Result result, int patternType) {
        // 在这里实现炉子激活的逻辑
        // 例如：改变方块状态、开始工作、播放音效等

        // 示例：记录激活信息
        // FoodCraft.LOGGER.info("Stove structure activated at {} with pattern type {}", pos, patternType);
    }

    /**
     * 当炉子结构无效时调用
     */
    private void onStoveStructureInvalid(World world, BlockPos pos) {
        // 在这里实现炉子停用的逻辑
        // 例如：重置方块状态、停止工作等

        // 示例：记录停用信息
        // FoodCraft.LOGGER.debug("Stove structure deactivated at {}", pos);
    }

    /**
     * 检查方块堆是否符合炉子结构要求
     */
    public boolean isValidStoveStructure() {
        if (multiBlockRef == null || multiBlockRef.isDisposed()) {
            return false;
        }

        MultiBlock.PatternRange range = multiBlockRef.getMultiBlock().getRange();
        int width = range.getWidth();
        int height = range.getHeight();
        int depth = range.getDepth();

        // 炉子结构要求：水平方向，高度为1
        if (height != 1) {
            return false;
        }

        // 检查是否为有效尺寸：1x1, 1x2, 2x2, 2x3
        return (width == 1 && depth == 1) ||  // 1x1
                (width == 1 && depth == 2) ||  // 1x2
                (width == 2 && depth == 2) ||  // 2x2
                (width == 2 && depth == 3);    // 2x3
    }

    /**
     * 获取炉子结构的类型索引
     */
    public int getStoveStructureType() {
        if (!isValidStoveStructure()) {
            return -1;
        }

        MultiBlock.PatternRange range = multiBlockRef.getMultiBlock().getRange();
        int width = range.getWidth();
        int depth = range.getDepth();

        if (width == 1 && depth == 1) return 1;  // 1x1
        if (width == 1 && depth == 2) return 2;  // 1x2
        if (width == 2 && depth == 2) return 3;  // 2x2
        if (width == 2 && depth == 3) return 4;  // 2x3

        return -1;
    }

    /**
     * 设置多方块引用
     */
    public void setMultiBlockReference(@Nullable MultiBlockReference ref) {
        // 先清理旧的引用
        if (this.multiBlockRef != null) {
            this.multiBlockRef.dispose();
        }

        this.multiBlockRef = ref;

        // 重置结构检查状态
        this.currentStoveResult = null;
        this.stoveStructureType = -1;
        this.isValidStove = false;
    }

    /**
     * 获取多方块引用
     */
    @Nullable
    public MultiBlockReference getMultiBlockReference() {
        return multiBlockRef;
    }

    /**
     * 检查是否为多方块结构的主方块
     */
    public boolean isMasterBlock() {
        return multiBlockRef != null && multiBlockRef.isMasterBlock();
    }

    /**
     * 获取当前炉子结构检查结果
     */
    @Nullable
    public BlockPattern.Result getCurrentStoveResult() {
        return currentStoveResult;
    }

    /**
     * 获取当前炉子结构类型
     */
    public int getCurrentStoveStructureType() {
        return stoveStructureType;
    }

    /**
     * 检查当前是否有效的炉子结构
     */
    public boolean isStoveValid() {
        return isValidStove;
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        // 清理引用
        if (multiBlockRef != null) {
            multiBlockRef.dispose();
            multiBlockRef = null;
        }
    }

    /**
     * 获取对应的炉子图案
     * @param index 图案索引
     * @return 对应的炉子图案
     */
    public BlockPattern getStovePattern(int index){
        switch (index) {
            case 1 -> {
                return stove1x1;
            }
            case 2 -> {
                return stove1x2;
            }
            case 3 -> {
                return stove2x2;
            }
            case 4 -> {
                return stove2x3;
            }
        }
        return null;
    }

    private void initStovePattern() {
        this.stove1x1 = BlockPatternBuilder.start()
                .aisle("###", "###")
                .aisle("#^#","#~#")
                .aisle("#-#", "#|#")
                .where('^', cachedBlockPosition -> cachedBlockPosition.getBlockState().isAir())
                .where('#', cachedBlockPosition -> !cachedBlockPosition.getBlockState().isAir())
                .where('|', cachedBlockPosition -> cachedBlockPosition.getBlockState().getBlock() instanceof HeatResistantSlateBlock)
                .build();
        this.stove1x2 = BlockPatternBuilder.start()
                .aisle("###", "###")
                .aisle("#^#","#~#")
                .aisle("#-#", "#|#")
                .aisle("#-#", "#|#")
                .where('^', cachedBlockPosition -> cachedBlockPosition.getBlockState().isAir())
                .where('#', cachedBlockPosition -> !cachedBlockPosition.getBlockState().isAir())
                .where('|', cachedBlockPosition -> cachedBlockPosition.getBlockState().getBlock() instanceof HeatResistantSlateBlock)
                .build();
        this.stove2x2 = BlockPatternBuilder.start()
                .aisle("####", "####")
                .aisle("#^^#","#~~#")
                .aisle("#--#", "#||#")
                .aisle("#--#", "#||#")
                .where('^', cachedBlockPosition -> cachedBlockPosition.getBlockState().isAir())
                .where('#', cachedBlockPosition -> !cachedBlockPosition.getBlockState().isAir())
                .where('|', cachedBlockPosition -> cachedBlockPosition.getBlockState().getBlock() instanceof HeatResistantSlateBlock)
                .build();
        this.stove2x3 = BlockPatternBuilder.start()
                .aisle("####", "####")
                .aisle("#^^#","#~~#")
                .aisle("#--#", "#||#")
                .aisle("#--#", "#||#")
                .aisle("#--#", "#||#")
                .where('^', cachedBlockPosition -> cachedBlockPosition.getBlockState().isAir())
                .where('#', cachedBlockPosition -> !cachedBlockPosition.getBlockState().isAir())
                .where('|', cachedBlockPosition -> cachedBlockPosition.getBlockState().getBlock() instanceof HeatResistantSlateBlock)
                .build();
    }
}