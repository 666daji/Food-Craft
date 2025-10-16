package org.foodcraft.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.dfood.block.FoodBlock;
import org.foodcraft.FoodCraft;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class FoodCraftUtils {
    private static final Logger LOGGER = FoodCraft.LOGGER;

    /**
     * 创建食物方块状态
     */
    public static BlockState createFoodBlockState(BlockState state, int foodCount, Direction facing) {
        if (facing == Direction.UP || facing == Direction.DOWN){
            LOGGER.warn("FoodBlock direction can only be horizontal");
            facing = Direction.EAST;
        }

        if (state.getBlock() instanceof FoodBlock foodBlock) {
            return state
                    .with(FoodBlock.FACING, facing)
                    .with(foodBlock.NUMBER_OF_FOOD, foodCount);
        }
        return Blocks.AIR.getDefaultState();
    }

    /**
     * 检查一个Block实例是否注册了指定的属性
     * @param block 要检查的Block实例
     * @param property 要检查的属性
     * @return 如果注册了该属性则返回true，否则返回false
     */
    public static boolean hasProperty(Block block, Property<?> property) {
        if (block == null || property == null) {
            return false;
        }

        StateManager<Block, BlockState> stateManager = block.getStateManager();
        Property<?> foundProperty = stateManager.getProperty(property.getName());
        return foundProperty != null && foundProperty.equals(property);
    }

    public static Set<BlockPos> findTargetPositionsFromPattern(BlockPattern.Result result, int patternType, Predicate<CachedBlockPosition> isTargetPositionPredicate) {
        Set<BlockPos> TargetPos = new HashSet<>();
        for (int depth = 0; depth < result.getDepth(); depth++) {
            for (int height = 0; height < result.getHeight(); height++) {
                for (int width = 0; width < result.getWidth(); width++) {
                    // 获取该位置的缓存方块
                    CachedBlockPosition cachedPos = result.translate(width, height, depth);

                    // 检查该位置是否匹配'~'字符的谓词条件
                    if (isTargetPositionPredicate.test(cachedPos)) {
                        // 找到匹配的位置，返回对应的世界坐标
                        TargetPos.add(cachedPos.getBlockPos());
                    }
                }
            }
        }
        return TargetPos;
    }

    /**
     * 序列化BlockPos为NbtCompound
     */
    public static NbtCompound serializeBlockPos(BlockPos pos) {
        NbtCompound tag = new NbtCompound();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        return tag;
    }

    /**
     * 从NbtCompound反序列化BlockPos
     */
    public static BlockPos deserializeBlockPos(NbtCompound tag) {
        if (tag.contains("x") && tag.contains("y") && tag.contains("z")) {
            return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        }
        return null;
    }
}
