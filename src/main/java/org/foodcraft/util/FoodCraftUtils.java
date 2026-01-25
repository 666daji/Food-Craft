package org.foodcraft.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.dfood.block.FoodBlock;
import org.dfood.item.DoubleBlockItem;
import org.dfood.util.DFoodUtils;
import org.foodcraft.FoodCraft;
import org.jetbrains.annotations.Nullable;
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
     * 获取物品对应方块的声音事件组
     * @param itemStack 要获取的物品堆栈
     * @return 获取的声音组，如果物品不是一个
     */
    @Nullable
    public static BlockSoundGroup getSoundGroupFromItem(ItemStack itemStack) {
        BlockState state = DFoodUtils.getBlockStateFromItem(itemStack.getItem());
        if (state != null) {
            return state.getBlock().getSoundGroup(state);
        }

        return null;
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

    /**
     * 从BlockPattern.Result中查找符合条件的方块位置
     * @param result BlockPattern匹配结果
     * @param isTargetPositionPredicate 用于判断是否为目标位置的谓词
     * @return 符合条件的方块位置集合
     */
    public static Set<BlockPos> findTargetPositionsFromPattern(BlockPattern.Result result, Predicate<CachedBlockPosition> isTargetPositionPredicate) {
        Set<BlockPos> TargetPos = new HashSet<>();
        for (int depth = 0; depth < result.getDepth(); depth++) {
            for (int height = 0; height < result.getHeight(); height++) {
                for (int width = 0; width < result.getWidth(); width++) {
                    // 获取该位置的缓存方块
                    CachedBlockPosition cachedPos = result.translate(width, height, depth);

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
     * 获取FoodBlock的NUMBER_OF_FOOD属性
     * @param block 要检查的Block实例
     * @return 如果是FoodBlock则返回NUMBER_OF_FOOD属性，否则返回null
     */
    @Nullable
    public static IntProperty getFoodBlockProperty(Block block) {
        if (block instanceof FoodBlock foodBlock) {
            return foodBlock.NUMBER_OF_FOOD;
        }
        return null;
    }

    /**
     * 检查物品堆是否为水瓶。
     * @param stack 要检查的物品堆
     * @return 如果物品堆是水瓶则返回true，否则返回false
     */
    public static boolean isWaterPotion(ItemStack stack) {
        if (stack.getItem() instanceof PotionItem) {
            return stack.isOf(Items.POTION) &&
                    PotionUtil.getPotion(stack) == Potions.WATER;
        }
        return false;
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

    /**
     * 根据物品堆和朝向创建对应的方块状态
     * @param stack 物品堆栈
     * @param facing 方块朝向
     * @return 对应的方块状态 如果返回值为{@linkplain Blocks#AIR}，则说明该物品没有对应的方块状态
     */
    public static BlockState createCountBlockstate(ItemStack stack, Direction facing) {
        Item item = stack.getItem();
        BlockState blockState = DFoodUtils.getBlockStateFromItem(item);

        if (blockState == null) {
            return Blocks.AIR.getDefaultState();
        }

        Block block = blockState.getBlock();

        // 处理FoodBlock（包括DoubleBlockItem中的FoodBlock）
        if (block instanceof FoodBlock) {
            return createFoodBlockState(blockState, stack.getCount(), facing);
        }

        // 处理其他方块，检查是否有水平朝向属性
        if (blockState.contains(Properties.HORIZONTAL_FACING)) {
            return blockState.with(Properties.HORIZONTAL_FACING, facing);
        }

        return blockState;
    }

    /**
     * 按中心点缩放VoxelShape
     * @param shape 待缩放的形状
     * @param scale 缩放比例 只能为正数
     * @return 缩放后的形状
     */
    public static VoxelShape scale(VoxelShape shape, double scale) {
        if (shape.isEmpty()) {
            return VoxelShapes.empty();
        }

        VoxelShape[] result = new VoxelShape[]{VoxelShapes.empty()};

        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            // 计算中心点
            double centerX = (minX + maxX) / 2;
            double centerY = (minY + maxY) / 2;
            double centerZ = (minZ + maxZ) / 2;

            // 相对于中心点进行缩放
            double newMinX = centerX + (minX - centerX) * scale;
            double newMinY = centerY + (minY - centerY) * scale;
            double newMinZ = centerZ + (minZ - centerZ) * scale;
            double newMaxX = centerX + (maxX - centerX) * scale;
            double newMaxY = centerY + (maxY - centerY) * scale;
            double newMaxZ = centerZ + (maxZ - centerZ) * scale;

            VoxelShape scaledBox = VoxelShapes.cuboid(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
            result[0] = VoxelShapes.union(result[0], scaledBox);
        });

        return result[0];
    }

    /**
     * 按原点缩放VoxelShape
     * @param shape 待缩放的形状
     * @param scale 缩放比例
     * @return 缩放后的形状
     */
    public static VoxelShape scaleFromOrigin(VoxelShape shape, double scale) {
        if (shape.isEmpty()) {
            return VoxelShapes.empty();
        }

        VoxelShape[] result = new VoxelShape[]{VoxelShapes.empty()};

        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double newMinX = minX * scale;
            double newMinY = minY * scale;
            double newMinZ = minZ * scale;
            double newMaxX = maxX * scale;
            double newMaxY = maxY * scale;
            double newMaxZ = maxZ * scale;

            VoxelShape scaledBox = VoxelShapes.cuboid(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
            result[0] = VoxelShapes.union(result[0], scaledBox);
        });

        return result[0];
    }
}
