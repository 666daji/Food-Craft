package org.foodcraft.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.block.entity.UpPlaceBlockEntity;

import java.util.List;

/**
 * 可放置物品的方块基类，提供统一的物品放置和取出交互机制。
 * <p>
 * 该方块通过与实现{@link Inventory}接口的方块实体配合，允许玩家在方块上放置和取出物品。
 * 支持自定义放置和取出条件、音效以及物品的视觉表现。
 * </p>
 *
 * @see UpPlaceBlockEntity
 */
public abstract class UpPlaceBlock extends BlockWithEntity {
    /**
     * 方块交互音效配置
     */
    public final UpSounds upSounds;

    public UpPlaceBlock(Settings settings, UpSounds upSounds) {
        super(settings);
        this.upSounds = upSounds;
    }

    public UpPlaceBlock(Settings settings) {
        this(settings, UpSounds.DEFAULT);
    }

    /**
     * 当方块被替换或破坏时，将方块实体中的库存物品掉落出来
     * <p>
     * 确保方块被破坏时不会丢失其中的物品。
     * </p>
     */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory inventory) {
                ItemScatterer.spawn(world, pos, inventory);
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    /**
     * 获取方块的轮廓形状，合并基础形状与容器中物品的形状
     * <p>
     * 当方块实体中有物品时，轮廓形状会是基础形状和物品形状的并集，
     * 这样可以正确显示物品在方块上的视觉表现。
     * </p>
     */
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof UpPlaceBlockEntity blockEntity && !blockEntity.isEmpty()) {
            return VoxelShapes.union(
                    getBaseShape(state, world, pos, context),
                    blockEntity.getContentShape(state, world, pos, context)
            );
        }
        return getBaseShape(state, world, pos, context);
    }

    /**
     * 获取方块的基准轮廓形状
     * <p>
     * 子类必须实现此方法来定义方块本身的基本碰撞体积。
     * </p>
     *
     * @param state 当前方块状态
     * @param world 方块所在的世界
     * @param pos 方块位置
     * @param context 形状计算上下文
     * @return 方块的基准轮廓形状
     */
    public abstract VoxelShape getBaseShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context);

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof UpPlaceBlockEntity upPlaceBlockEntity) {
            // 尝试取出物品
            if (canFetched(upPlaceBlockEntity, handStack)) {
                ActionResult fetchResult = upPlaceBlockEntity.tryFetchItem(player);
                if (upPlaceBlockEntity.isAccepted(fetchResult)) {
                    // 获取此次取出操作成功取出的所有物品
                    List<ItemStack> fetchStack = upPlaceBlockEntity.getFetchStacks(player, hand, hit);
                    upPlaceBlockEntity.onFetch(state, world, pos, player, hand, hit, fetchStack);
                    return fetchResult;
                }
            }

            // 尝试放置物品
            if (canPlace(upPlaceBlockEntity, handStack)) {
                ActionResult placeResult = upPlaceBlockEntity.tryAddItem(handStack);
                if (upPlaceBlockEntity.isAccepted(placeResult)) {
                    upPlaceBlockEntity.onPlace(state, world, pos, player, hand, hit, handStack);
                    return placeResult;
                }
            }
        }

        return ActionResult.FAIL;
    }

    /**
     * 检查当前条件下是否可以执行取出操作
     * <p>
     * 子类必须实现此方法来确定取出操作的触发条件，
     * 例如：手中是否持有特定工具、方块中是否有物品等。
     * </p>
     *
     * @param blockEntity 目标方块实体
     * @param handStack 玩家手中的物品堆栈
     * @return 如果可以取出物品返回true，否则返回false
     */
    public abstract boolean canFetched(UpPlaceBlockEntity blockEntity, ItemStack handStack);

    /**
     * 检查当前条件下是否可以执行放置操作
     * <p>
     * 子类必须实现此方法来确定放置操作的触发条件，
     * 例如：手中物品是否有效、方块是否有空位等。
     * </p>
     *
     * @param blockEntity 目标方块实体
     * @param handStack 玩家手中的物品堆栈
     * @return 如果可以放置物品返回true，否则返回false
     */
    public abstract boolean canPlace(UpPlaceBlockEntity blockEntity, ItemStack handStack);

    /**
     * 定义物品在{@link UpPlaceBlock}上放置和取出时播放的音效
     *
     * @param placeSound 放置物品时播放的音效
     * @param fetchSound 取出物品时播放的音效
     */
    public record UpSounds(SoundEvent placeSound, SoundEvent fetchSound) {
        /**
         * 空音效定义，表示不播放任何音效
         */
        public static final UpSounds EMPTY = new UpSounds(null, null);

        /**
         * 默认的音效配置
         */
        public static final UpSounds DEFAULT = new UpSounds(SoundEvents.BLOCK_STONE_PLACE, SoundEvents.BLOCK_STONE_BREAK);

        /**
         * 在指定位置播放放置音效
         * <p>
         * 音效会在整个世界中播放，所有玩家都能听到。
         * 如果placeSound为null，则不会播放任何音效。
         * </p>
         *
         * @param world 播放音效的世界
         * @param pos 播放音效的位置
         */
        public void playPlaceSound(World world, BlockPos pos) {
            if (placeSound != null && !world.isClient) {
                world.playSound(
                        null,
                        pos,
                        placeSound,
                        SoundCategory.BLOCKS,
                        1.0F,
                        1.0F
                );
            }
        }

        /**
         * 在指定位置播放取出音效
         * <p>
         * 音效会在整个世界中播放，所有玩家都能听到。
         * 如果fetchSound为null，则不会播放任何音效。
         * </p>
         *
         * @param world 播放音效的世界
         * @param pos 播放音效的位置
         */
        public void playFetchSound(World world, BlockPos pos) {
            if (fetchSound != null && !world.isClient) {
                world.playSound(
                        null,
                        pos,
                        fetchSound,
                        SoundCategory.BLOCKS,
                        1.0F,
                        1.0F
                );
            }
        }

        public void playSound(World world, BlockPos pos, boolean isPlaceSound) {
            if (isPlaceSound) {
                playPlaceSound(world, pos);
            } else {
                playFetchSound(world, pos);
            }
        }

        public boolean isDefault() {
            return this == DEFAULT;
        }
    }
}