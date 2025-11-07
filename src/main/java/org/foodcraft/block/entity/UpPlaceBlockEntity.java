package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.dfood.item.DoubleBlockItem;
import org.foodcraft.block.UpPlaceBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 放置物品方块实体基类，用于管理方块上的物品放置和取出功能。
 * <p>
 * 该实体实现了{@link Inventory}接口，支持物品的存储和管理，并提供了一套完整的物品放置和取出机制。
 * 子类需要实现特定的物品验证、形状计算和交互逻辑。
 * </p>
 *
 * @see UpPlaceBlock
 */
public abstract class UpPlaceBlockEntity extends BlockEntity implements Inventory {
    /**
     * 物品栏列表，存储方块上放置的所有物品
     */
    protected DefaultedList<ItemStack> inventory;

    /**
     * 临时存储取出的物品列表
     * <p>
     * 在执行{@link #tryFetchItem(PlayerEntity)}方法后，成功取出的物品会暂存在此列表中。
     * 这些物品通常会在{@link #onFetch(BlockState, World, BlockPos, PlayerEntity, Hand, BlockHitResult, List)}方法中
     * 被立即消耗或转移，因此该列表大部分时间都为空。
     * </p>
     */
    protected List<ItemStack> fetchStacks;

    public UpPlaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize) {
        super(type, pos, state);
        this.inventory = DefaultedList.ofSize(inventorySize, ItemStack.EMPTY);
        this.fetchStacks = new CopyOnWriteArrayList<>();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, this.inventory);
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        validateSlotIndex(slot);
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        validateSlotIndex(slot);
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        validateSlotIndex(slot);
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        validateSlotIndex(slot);

        this.inventory.set(slot, stack);
        limitStackSizeIfNeeded(stack);
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    /**
     * 限制物品堆叠大小不超过最大值
     *
     * @param stack 需要限制堆叠大小的物品堆栈
     */
    protected void limitStackSizeIfNeeded(ItemStack stack) {
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
    }

    /**
     * 验证槽位索引是否在有效范围内
     *
     * @param slot 要验证的槽位索引
     * @throws IllegalArgumentException 如果槽位索引超出有效范围
     */
    public void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= this.size()) {
            throw new IllegalArgumentException("Slot " + slot + " not in valid range - [0," + this.size() + ")");
        }
    }

    /**
     * 获取容器中物品的碰撞形状
     * <p>
     * 该方法用于计算物品在方块世界中的视觉表现和碰撞体积。
     * </p>
     *
     * @param state 当前方块状态
     * @param world 方块所在的世界
     * @param pos 方块位置
     * @param context 形状计算上下文
     * @return 物品的碰撞形状
     */
    public abstract VoxelShape getContentShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context);

    /**
     * 验证物品是否可以放入该方块实体的物品栏
     *
     * @param stack 待验证的物品堆栈
     * @return 如果可以放入返回true，否则返回false
     */
    public abstract boolean isValidItem(ItemStack stack);

    /**
     * 尝试向容器中添加物品
     * <p>
     * 默认每次添加会消耗数量为1的物品。如果需要不同的消耗数量，
     * 一般需要重写{@link #onPlace(BlockState, World, BlockPos, PlayerEntity, Hand, BlockHitResult, ItemStack)}方法，
     * 来扣除玩家不同数量的物品
     * </p>
     *
     * @param stack 要添加的物品堆栈
     * @return 操作结果，成功返回{@link ActionResult#SUCCESS}，失败返回{@link ActionResult#FAIL}
     */
    public abstract ActionResult tryAddItem(ItemStack stack);

    /**
     * 尝试从容器中取出物品
     * <p>
     * <strong>重要：</strong>子类在重写此方法时，必须在返回成功结果之前设置{@link #fetchStacks}字段，
     * 将成功取出的物品添加到该列表中。
     * </p>
     *
     * @param player 执行取出操作的玩家
     * @return 操作结果，成功返回{@link ActionResult#SUCCESS}，失败返回{@link ActionResult#FAIL}
     */
    public abstract ActionResult tryFetchItem(PlayerEntity player);

    /**
     * 当物品成功取出时调用的回调方法
     * <p>
     * 默认实现会播放取出音效。子类可以重写此方法来添加自定义逻辑，
     * 如播放粒子效果、执行特殊操作等。如果重写此方法，请确保根据需要
     * 调用父类方法以保持默认的音效行为。
     * </p>
     *
     * @param state 当前方块状态
     * @param world 方块所在的世界
     * @param pos 方块位置
     * @param player 执行取出操作的玩家
     * @param hand 玩家使用的手
     * @param hit 方块击中结果
     * @param fetchStacks 此次取出操作获得的所有物品堆栈
     */
    public void onFetch(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, List<ItemStack> fetchStacks) {
        playSound(world, pos, fetchStacks.get(0), false);
    }

    /**
     * 当物品成功放置时调用的回调方法
     * <p>
     * 默认实现会播放放置音效并消耗玩家手中1个物品（创造模式除外）。
     * 子类可以重写此方法来添加自定义逻辑。如果重写此方法，请确保根据需要
     * 调用父类方法以保持默认的音效和物品消耗行为。
     * </p>
     *
     * @param state 当前方块状态
     * @param world 方块所在的世界
     * @param pos 方块位置
     * @param player 执行放置操作的玩家
     * @param hand 玩家使用的手
     * @param hit 方块击中结果
     * @param placeStack 放置的物品堆栈
     */
    public void onPlace(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, ItemStack placeStack) {
        playSound(world, pos, placeStack, true);

        if (!player.isCreative()) {
            placeStack.decrement(1);
        }
    }

    /**
     * 获取当前暂存的取出物品列表，并清空内部存储
     * <p>
     * 这是一个消费型获取方法，调用后会清空内部的{@link #fetchStacks}列表。
     * 确保每次调用后列表都会被重置，为下一次取出操作做准备。
     * </p>
     *
     * @param fetchPlayer 执行取出操作的玩家
     * @param hand 玩家使用的手
     * @param hit 击中结果
     * @return 当前暂存的取出物品列表，如果为空则返回空列表（不会返回null）
     */
    public List<ItemStack> getFetchStacks(PlayerEntity fetchPlayer, Hand hand, HitResult hit) {
        if (fetchStacks.isEmpty()) {
            return Collections.emptyList();
        }

        // 创建副本并清空原列表
        List<ItemStack> result = new ArrayList<>(fetchStacks);
        fetchStacks = new CopyOnWriteArrayList<>();
        return result;
    }

    /**
     * 根据物品堆栈获取对应的声音事件
     * <p>
     * 如果物品是{@link BlockItem}，则返回对应方块的放置音效；
     * 否则返回默认的放置音效。
     * </p>
     *
     * @param itemStack 物品堆栈
     * @return 对应的放置音效，如果没有合适的音效则返回null
     */
    private SoundEvent getSoundForItem(ItemStack itemStack, boolean isPlaceSound) {
        BlockSoundGroup soundGroup = getSoundGroupFromItem(itemStack);

        if (soundGroup != null) {
            return isPlaceSound ?
                    soundGroup.getPlaceSound() : soundGroup.getBreakSound();
        }

        // 默认音效
        UpPlaceBlock.UpSounds defaultSounds = UpPlaceBlock.UpSounds.DEFAULT;
        return isPlaceSound ?
                defaultSounds.placeSound() : defaultSounds.fetchSound();
    }

    private BlockSoundGroup getSoundGroupFromItem(ItemStack itemStack) {
        Item item = itemStack.getItem();

        if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock()
                    .getSoundGroup(blockItem.getBlock().getDefaultState());
        }

        if (item instanceof DoubleBlockItem doubleBlockItem) {
            return doubleBlockItem.getSecondBlock()
                    .getSoundGroup(doubleBlockItem.getSecondBlock().getDefaultState());
        }

        return null;
    }

    /**
     * 默认的播放物品取回和放出的声音，子类在重写onPlace和onFetch方法时可以选择性地调用
     * @param world 当前的世界
     * @param pos 播放声音的位置
     * @param isPlaceSound 是否是放置的声音
     */
    protected void playSound(World world, BlockPos pos, ItemStack stack, boolean isPlaceSound){
        if (getCachedState().getBlock() instanceof UpPlaceBlock upPlaceBlock) {
            if (upPlaceBlock.upSounds.isDefault()) {
                world.playSound(
                        null,
                        pos,
                        getSoundForItem(stack, isPlaceSound),
                        SoundCategory.BLOCKS,
                        1.0F,
                        1.0F
                );
            } else {
                upPlaceBlock.upSounds.playSound(world, pos, isPlaceSound);
            }
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    /**
     * 获取容器中的第一个物品的类型
     *
     * @return 容器中第一个物品的类型，如果容器为空则返回null
     */
    public Item getContentItem() {
        ItemStack contentStack = this.getStack(0);
        return contentStack.isEmpty() ? null : contentStack.getItem();
    }

    /**
     * 检查容器是否已满
     * <p>
     * 容器已满的条件是所有槽位都有物品且每个物品都达到了最大堆叠数量。
     * </p>
     *
     * @return 如果容器已满返回true，否则返回false
     */
    public boolean isFull() {
        for (int i = 0; i < this.size(); i++) {
            ItemStack stack = this.getStack(i);
            if (stack.isEmpty() || stack.getCount() < this.getMaxCountPerStack()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查操作结果是否表示操作被接受（成功执行）
     *
     * @param result 要检查的操作结果
     * @return 如果操作被接受返回true，否则返回false
     */
    public boolean isAccepted(ActionResult result) {
        return result.isAccepted();
    }

    /**
     * 标记方块实体数据已更改，并同步到客户端
     * <p>
     * 这是一个便捷方法，结合了{@link #markDirty()}和{@link #sync()}的调用。
     * </p>
     */
    public void markDirtyAndSync() {
        this.markDirty();
        this.sync();
    }

    /**
     * 同步方块实体数据到客户端
     * <p>
     * 当方块实体数据发生变化时调用此方法，确保客户端能够及时更新显示。
     * </p>
     */
    public void sync() {
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }
}