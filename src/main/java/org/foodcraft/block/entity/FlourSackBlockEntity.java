package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.foodcraft.item.FlourItem;
import org.foodcraft.item.FlourSackItem;
import org.foodcraft.registry.ModBlockEntityTypes;

import java.util.Optional;

/**
 * 粉尘袋方块实体，管理堆叠和内容物
 */
public class FlourSackBlockEntity extends BlockEntity {
    public static final String ITEMS_KEY = "Items";
    private static final String SACK_COUNT_KEY = "SackCount";
    public static final int MAX_SACK_STACK = 2;
    private static final int ITEMS_PER_SACK = 64;
    public static final int DEFAULT_FLOUR_COLOR = 0xFFFFFF; // 默认白色

    private DefaultedList<ItemStack> sackContents;
    private int sackCount;

    public FlourSackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.FLOUR_SACK, pos, state);
        this.sackContents = DefaultedList.ofSize(MAX_SACK_STACK, ItemStack.EMPTY);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
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

    /**
     * 从物品NBT读取数据到方块实体
     */
    public void readItemNbt(NbtCompound nbt) {
        if (nbt.contains(ITEMS_KEY, NbtElement.LIST_TYPE)) {
            loadItemsFromNbt(nbt.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE));
        }

        if (nbt.contains(SACK_COUNT_KEY)) {
            this.sackCount = nbt.getInt(SACK_COUNT_KEY);
        }
    }

    /**
     * 写入物品NBT数据
     */
    public void writeItemNbt(NbtCompound nbt) {
        NbtList items = createItemsNbtList();
        nbt.put(ITEMS_KEY, items);
        nbt.putInt(SACK_COUNT_KEY, sackCount);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        readItemNbt(nbt);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        writeItemNbt(nbt);
    }

    public int getSackCount() {
        return sackCount;
    }

    public void setSackCount(int count) {
        int oldCount = this.sackCount;
        this.sackCount = Math.min(Math.max(0, count), MAX_SACK_STACK);

        // 只有在堆叠数实际改变时才同步
        if (oldCount != this.sackCount) {
            markDirtyAndSync(); // 使用同步方法
        }
    }

    /**
     * 检查是否可以添加粉尘袋
     */
    public boolean canAddSack(ItemStack newSack) {
        if (sackCount >= MAX_SACK_STACK) return false;

        Optional<ItemStack> newContent = FlourSackItem.getFirstBundledStack(newSack);
        return areContentsCompatible(newContent);
    }

    /**
     * 添加粉尘袋
     */
    public boolean addSack(ItemStack newSack) {
        if (!canAddSack(newSack)) return false;

        Optional<ItemStack> newContent = FlourSackItem.getFirstBundledStack(newSack);
        newContent.ifPresent(stack -> sackContents.set(sackCount, stack));

        sackCount++;
        markDirtyAndSync();
        return true;
    }

    /**
     * 移除一个粉尘袋
     */
    public ItemStack removeSack() {
        if (sackCount <= 0) return ItemStack.EMPTY;

        sackCount--;
        ItemStack removedContent = sackContents.get(sackCount);
        sackContents.set(sackCount, ItemStack.EMPTY);
        markDirtyAndSync();

        return createSackItem(removedContent);
    }

    /**
     * 获取指定位置的粉尘袋内容物
     */
    public ItemStack getSackContent(int index) {
        if (index >= 0 && index < sackContents.size()) {
            return sackContents.get(index);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 设置指定位置的粉尘袋内容物
     */
    public void setSackContent(int index, ItemStack stack) {
        if (index >= 0 && index < sackContents.size()) {
            ItemStack oldStack = sackContents.get(index);
            sackContents.set(index, stack);

            // 只有在内容物实际改变时才同步
            if (!ItemStack.areEqual(oldStack, stack)) {
                markDirtyAndSync(); // 使用同步方法
            }
        }
    }

    /**
     * 根据索引获取对应堆叠的粉尘袋物品堆栈
     */
    public ItemStack getSackStack(int index) {
        if (!isValidSackIndex(index)) {
            return ItemStack.EMPTY;
        }

        ItemStack content = sackContents.get(index);
        return createSackItem(content);
    }

    /**
     * 根据索引获取对应位置的粉尘颜色
     */
    public int getFlourColor(int index) {
        if (!isValidSackIndex(index)) {
            return DEFAULT_FLOUR_COLOR;
        }

        ItemStack content = sackContents.get(index);
        return getFlourColorFromStack(content);
    }

    /**
     * 获取所有粉尘袋堆叠的颜色数组
     */
    public int[] getAllFlourColors() {
        int[] colors = new int[sackCount];
        for (int i = 0; i < sackCount; i++) {
            colors[i] = getFlourColor(i);
        }
        return colors;
    }

    public DefaultedList<ItemStack> getAllContents() {
        return sackContents;
    }

    /**
     * 获取总占用空间
     */
    public int getTotalOccupancy() {
        int total = 0;
        for (int i = 0; i < sackCount; i++) {
            ItemStack content = sackContents.get(i);
            if (!content.isEmpty()) {
                total += content.getCount();
            }
        }
        return total;
    }

    /**
     * 获取最大容量
     */
    public int getMaxCapacity() {
        return ITEMS_PER_SACK * sackCount;
    }

    /**
     * 检查指定索引是否有效
     */
    public boolean isValidSackIndex(int index) {
        return index >= 0 && index < sackCount;
    }

    /**
     * 强制同步数据到客户端（用于调试或特殊情况下）
     */
    public void forceSync() {
        markDirtyAndSync();
    }

    /**
     * 从NBT加载物品列表
     */
    private void loadItemsFromNbt(NbtList items) {
        this.sackContents = DefaultedList.ofSize(MAX_SACK_STACK, ItemStack.EMPTY);

        for (int i = 0; i < Math.min(items.size(), MAX_SACK_STACK); i++) {
            NbtCompound itemNbt = items.getCompound(i);
            this.sackContents.set(i, ItemStack.fromNbt(itemNbt));
        }
    }

    /**
     * 创建物品NBT列表
     */
    private NbtList createItemsNbtList() {
        NbtList items = new NbtList();
        for (int i = 0; i < this.sackCount; i++) {
            ItemStack stack = this.sackContents.get(i);
            if (!stack.isEmpty()) {
                NbtCompound itemNbt = new NbtCompound();
                stack.writeNbt(itemNbt);
                items.add(itemNbt);
            }
        }
        return items;
    }

    /**
     * 检查内容物兼容性
     */
    private boolean areContentsCompatible(Optional<ItemStack> newContent) {
        for (int i = 0; i < sackCount; i++) {
            ItemStack existingContent = sackContents.get(i);

            if (newContent.isPresent() &&
                    !existingContent.isEmpty() &&
                    !ItemStack.areItemsEqual(newContent.get(), existingContent)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据内容创建粉尘袋物品
     */
    public ItemStack createSackItem(ItemStack content) {
        ItemStack sackItem = new ItemStack(getCachedState().getBlock().asItem());

        if (!content.isEmpty()) {
            NbtCompound nbt = new NbtCompound();
            NbtList items = new NbtList();
            NbtCompound itemNbt = new NbtCompound();

            content.writeNbt(itemNbt);
            items.add(itemNbt);
            nbt.put(ITEMS_KEY, items);
            sackItem.setNbt(nbt);
        }

        return sackItem;
    }

    /**
     * 从物品堆栈获取粉尘颜色
     */
    private int getFlourColorFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return DEFAULT_FLOUR_COLOR;
        }

        if (stack.getItem() instanceof FlourItem flourItem) {
            return flourItem.getColor();
        }

        return DEFAULT_FLOUR_COLOR;
    }
}