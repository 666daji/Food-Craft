package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.block.UpPlaceBlock;

/**
 * @see UpPlaceBlock
 */
public abstract class UpPlaceBlockEntity extends BlockEntity implements Inventory {
    protected DefaultedList<ItemStack> inventory;

    public UpPlaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize) {
        super(type, pos, state);
        this.inventory = DefaultedList.ofSize(inventorySize, ItemStack.EMPTY);
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
     * 限制堆叠大小不超过最大值
     */
    protected void limitStackSizeIfNeeded(ItemStack stack) {
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
    }

    /**
     * 验证槽位索引是否有效
     */
    public void validateSlotIndex(int slot){
        if (slot < 0 || slot >= this.size()) {
            throw new IllegalArgumentException("Slot " + slot + " not in valid range - [0," + this.size() + ")");
        }
    };

    /**
     * 获取容器中物品的形状
     * @return 物品的形状
     */
    public abstract VoxelShape getContentShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context);

    /**
     * 验证物品是否可以放入该方块实体的物品栏
     * @param stack 待验证的物品堆栈
     * @return 是否可以放入
     */
    public abstract boolean isValidItem(ItemStack stack);

    /**
     * 尝试向容器中添加物品,
     * 每次添加仅消耗数量为1的物品，无论stack的数量是多少
     * @param stack 要添加的物品堆栈
     * @return ActionResult 表示操作结果
     */
    public abstract ActionResult tryAddItem(ItemStack stack);

    /**
     * 尝试从容器中取出一个物品
     * @param player 执行取出操作的玩家
     * @return ActionResult 表示操作结果
     */
    public abstract ActionResult tryFetchItem(PlayerEntity player);

    /**
     * 在取出物品时调用
     */
    public void onFetch(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
    }

    /**
     * 在放置物品时调用
     */
    public void onPlace(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    /**
     * 获取容器中的物品类型
     */
    public Item getContentItem() {
        ItemStack contentStack = this.getStack(0);
        return contentStack.isEmpty() ? null : contentStack.getItem();
    }

    /**
     * 检查容器是否已满
     * @return 如果容器已满则返回true，否则返回false
     */
    public boolean isFull() {
        for (int i = 0; i < this.size(); i++) {
            if (this.getStack(i).isEmpty() || this.getStack(i).getCount() < this.getMaxCountPerStack()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查操作是否被接受（成功执行）
     */
    public boolean isAccepted(ActionResult result) {
        return result.isAccepted();
    }

    /**
     * 标记脏状态并同步到客户端
     */
    public void markDirtyAndSync() {
        this.markDirty();
        this.sync();
    }

    /**
     * 同步方块实体数据到客户端
     */
    public void sync() {
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }
}
