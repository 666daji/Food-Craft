package org.foodcraft.block.entity;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.registry.Registries;
import org.foodcraft.block.FlourSackBlock;
import org.foodcraft.block.ShelfBlock;
import org.foodcraft.item.FlourSackItem;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.registry.ModItems;
import org.foodcraft.util.FoodCraftUtils;
import org.foodcraft.mixin.FlowerPotBlockAccessor;

import java.util.*;
import java.util.function.Predicate;

public class ShelfBlockEntity extends UpPlaceBlockEntity {
    private static final int INVENTORY_SIZE = 2;
    private static final int MAX_STACK_SIZE = 1;
    private static final String CONTENT_DATA_KEY = "ContentData"; // 重命名

    private static final byte CONTENT_TYPE_EMPTY = 0;
    private static final byte CONTENT_TYPE_FLOWER = 1;
    private static final byte CONTENT_TYPE_FLOUR = 2;

    private NbtList contentData; // 重命名

    /**
     * 允许放置在架子上的物品
     * @see ShelfBlockEntity#isValidItem
     * */
    public static final Set<Predicate<ItemStack>> CanPlaceItem = new HashSet<>();

    public ShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SHELF, pos, state, INVENTORY_SIZE);
        this.contentData = new NbtList(); // 重命名
        // 初始化contentData，确保与库存大小一致
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            this.contentData.add(createEmptyContentData());
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains(CONTENT_DATA_KEY)) {
            this.contentData = nbt.getList(CONTENT_DATA_KEY, 10);
            // 确保contentData大小正确
            while (this.contentData.size() < this.size()) {
                this.contentData.add(createEmptyContentData());
            }
        } else {
            this.contentData = new NbtList();
            for (int i = 0; i < this.size(); i++) {
                this.contentData.add(createEmptyContentData());
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put(CONTENT_DATA_KEY, this.contentData);
    }

    @Override
    public VoxelShape getContentShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = this.getCachedState().get(Properties.HORIZONTAL_FACING);

        double xOffset;
        double zOffset;

        VoxelShape resultShape = VoxelShapes.empty();

        for (int i = 0; i < this.size(); i++) {
            if (!this.getStack(i).isEmpty()) {
                BlockState itemState = this.getInventoryBlockState(i);
                if (!itemState.isAir()) {
                    VoxelShape itemShape = itemState.getOutlineShape(world, pos, context);
                    double offset = (double) -4 / 16;
                    double offsetSign = i == 0 ? -offset : offset;
                    double offsetSign_x = i == 0 ? offset : -offset;

                    itemShape = scaleShape(itemState, itemShape);

                    zOffset = switch (direction) {
                        case SOUTH-> {
                            xOffset = offsetSign;
                            yield -offset;
                        }
                        case NORTH-> {
                            xOffset = offsetSign_x;
                            yield offset;
                        }
                        case WEST-> {
                            xOffset = offset;
                            yield offsetSign;
                        }
                        case EAST-> {
                            xOffset = -offset;
                            yield offsetSign_x;
                        }
                        default -> {
                            xOffset = 0.0;
                            yield 0.0;
                        }
                    };

                    if (itemShape != null && !itemShape.isEmpty()) {
                        resultShape = VoxelShapes.union(resultShape, itemShape.offset(xOffset, 0, zOffset));
                    }
                }
            }
        }

        return resultShape;
    }

    protected VoxelShape scaleShape(BlockState itemState, VoxelShape itemShape) {
        if (itemState.getBlock() instanceof FlourSackBlock){
            return FoodCraftUtils.scale(itemShape, 0.7).offset(0, (double) 4 / 16, 0);
        }
        return itemShape.offset(0,(double) 5 / 16,0);
    }

    /**
     * 获取当前物品栏中的物品对应的方块状态
     * @return 物品对应的方块状态
     */
    public BlockState getInventoryBlockState(int index) {
        ItemStack stack = this.inventory.get(index);
        Direction facing = this.getCachedState().get(ShelfBlock.FACING);

        // 如果是花盆且已插花，返回对应的花盆方块状态
        if (isFlowerPot(stack) && hasFlower(index)) {
            Block flowerBlock = getFlowerBlock(index);
            if (flowerBlock != null) {
                Map<Block, Block> contentToPotted = FlowerPotBlockAccessor.getContentToPotted();
                Block pottedBlock = contentToPotted.get(flowerBlock);
                if (pottedBlock != null) {
                    return pottedBlock.getDefaultState();
                }
            }
        }

        // 如果是粉尘袋，设置对应的索引状态
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof FlourSackBlock) {
            BlockState flourSackState = blockItem.getBlock().getDefaultState();
            if (flourSackState.contains(FlourSackBlock.SHELF_INDEX)) {
                flourSackState = flourSackState.with(FlourSackBlock.SHELF_INDEX, index);
            }
            return flourSackState;
        }

        return FoodCraftUtils.createCountBlockstate(stack, facing);
    }

    @Override
    public boolean isValidItem(ItemStack stack) {
        for (Predicate<ItemStack> canItem : CanPlaceItem) {
            if (canItem.test(stack)) {
                // 如果是粉尘袋，检查是否有内容物
                if (stack.getItem() instanceof BlockItem blockItem &&
                        blockItem.getBlock() instanceof FlourSackBlock) {
                    return FlourSackItem.getFirstBundledStack(stack).isPresent();
                }
                return true;
            }
        }
        return canInsertFlower(stack);
    }

    /**
     * 检查是否可以插入花到花盆中
     */
    public boolean canInsertFlower(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();
        Map<Block, Block> contentToPotted = FlowerPotBlockAccessor.getContentToPotted();
        return contentToPotted.containsKey(block);
    }

    @Override
    public ActionResult tryAddItem(ItemStack stack) {
        if (stack.isEmpty() || !isValidItem(stack)) {
            return ActionResult.FAIL;
        }

        // 如果是花，尝试插入到空花盆中
        if (canInsertFlower(stack)) {
            return tryInsertFlower(stack);
        }

        // 放置普通物品
        ItemStack newStack = stack.copy();
        newStack.setCount(1);
        int emptySlot = this.foundSlot();
        if (emptySlot != -1) {
            this.setStack(emptySlot, newStack);

            // 如果是粉尘袋，保存内容物数据
            if (newStack.getItem() instanceof BlockItem blockItem &&
                    blockItem.getBlock() instanceof FlourSackBlock) {
                saveFlourContent(emptySlot, newStack);
            }
            // 如果是花盆，初始化花盆数据
            else if (isFlowerPot(newStack)) {
                initFlowerPotData(emptySlot);
            }

            this.markDirtyAndSync();
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    /**
     * 尝试将花插入到空花盆中
     */
    private ActionResult tryInsertFlower(ItemStack flowerStack) {
        if (!(flowerStack.getItem() instanceof BlockItem blockItem)) {
            return ActionResult.FAIL;
        }

        Block flowerBlock = blockItem.getBlock();

        // 寻找第一个空的或可插入的花盆槽位
        for (int i = 0; i < this.size(); i++) {
            ItemStack slotStack = this.getStack(i);
            if (isFlowerPot(slotStack) && !hasFlower(i)) {
                // 设置花盆插花数据
                setFlowerData(i, flowerBlock);
                this.markDirtyAndSync();
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.FAIL;
    }

    /**
     * 寻找剩余的空槽位
     * @return 剩余空槽位的索引，如果有多个空槽位则返回第一个。无空槽位时返回-1
     */
    public int foundSlot(){
        for (int i = 0; i < inventory.size(); i++) {
            if (this.getStack(i).isEmpty()){
                return i;
            }
        }
        return -1;
    }

    @Override
    public ActionResult tryFetchItem(PlayerEntity player) {
        for (int i = this.size() - 1; i >= 0; i--) {
            ItemStack stack = this.getStack(i);
            if (!stack.isEmpty()) {
                // 如果是花盆且有花，先尝试取出花
                if (isFlowerPot(stack) && hasFlower(i)) {
                    return tryFetchFlower(player, i);
                }

                // 如果是粉尘袋，恢复内容物
                if (stack.getItem() instanceof BlockItem blockItem &&
                        blockItem.getBlock() instanceof FlourSackBlock) {
                    stack = restoreFlourContent(i, stack);
                }

                // 取出普通物品
                ItemStack extractedStack = stack.copy();
                extractedStack.setCount(1);

                this.fetchStacks = List.of(extractedStack.copy());

                if (!player.isCreative() && !player.giveItemStack(extractedStack)) {
                    player.dropItem(extractedStack, false);
                }

                stack.decrement(1);
                if (stack.isEmpty()) {
                    this.setStack(i, ItemStack.EMPTY);
                    // 清除内容数据
                    clearContentData(i);
                }

                this.markDirtyAndSync();
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.FAIL;
    }

    /**
     * 获取所有掉落物，包括花盆中的花和粉尘袋中的粉尘
     */
    public DefaultedList<ItemStack> getDroppedStacks() {
        DefaultedList<ItemStack> drops = DefaultedList.of();

        // 添加库存中的物品
        for (int i = 0; i < this.size(); i++) {
            ItemStack stack = this.getStack(i);
            if (!stack.isEmpty()) {
                // 恢复粉尘袋内容物
                if (stack.getItem() instanceof BlockItem blockItem &&
                        blockItem.getBlock() instanceof FlourSackBlock) {
                    stack = restoreFlourContent(i, stack);
                }

                // 如果是花盆且有花，掉落花
                if (isFlowerPot(stack) && hasFlower(i)) {
                    Block flowerBlock = getFlowerBlock(i);
                    if (flowerBlock != null) {
                        drops.add(new ItemStack(flowerBlock));
                    }
                }

                drops.add(stack);
            }
        }

        return drops;
    }

    /**
     * 尝试从花盆中取出花
     */
    private ActionResult tryFetchFlower(PlayerEntity player, int slot) {
        Block flowerBlock = getFlowerBlock(slot);
        if (flowerBlock == null) {
            return ActionResult.FAIL;
        }

        // 创建花的物品堆栈
        ItemStack flowerStack = new ItemStack(flowerBlock);

        this.fetchStacks = List.of(flowerStack.copy());
        // 给予玩家花
        if (!player.isCreative() && !player.giveItemStack(flowerStack)) {
            player.dropItem(flowerStack, false);
        }

        // 清除花盆中的花数据
        clearFlowerData(slot);
        this.markDirtyAndSync();

        return ActionResult.SUCCESS;
    }

    @Override
    public int getMaxCountPerStack() {
        return MAX_STACK_SIZE;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = this.createNbt();
        nbt.put(CONTENT_DATA_KEY, this.contentData);
        return nbt;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    /**
     * 获取内容数据
     * @return 当前内容的nbt数据
     */
    public NbtList getContentData() {
        return this.contentData.copy();
    }

    /**
     * 检查物品是否是花盆
     */
    private boolean isFlowerPot(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem &&
                blockItem.getBlock() instanceof FlowerPotBlock;
    }

    /**
     * 检查指定槽位的花盆是否有花
     */
    private boolean hasFlower(int slot) {
        if (slot < 0 || slot >= this.size()) {
            return false;
        }
        NbtCompound content = this.contentData.getCompound(slot);
        return content.getByte("Type") == CONTENT_TYPE_FLOWER;
    }

    /**
     * 获取指定槽位花盆中的花方块
     */
    private Block getFlowerBlock(int slot) {
        if (slot < 0 || slot >= this.contentData.size()) {
            return null;
        }
        NbtCompound content = this.contentData.getCompound(slot);
        if (content.getByte("Type") == CONTENT_TYPE_FLOWER && content.contains("flower")) {
            String flowerId = content.getString("flower");
            return Registries.BLOCK.get(new Identifier(flowerId));
        }
        return null;
    }

    /**
     * 设置花盆插花数据
     */
    private void setFlowerData(int slot, Block flowerBlock) {
        if (slot < 0 || slot >= this.contentData.size()) {
            return;
        }
        NbtCompound content = new NbtCompound();
        content.putByte("Type", CONTENT_TYPE_FLOWER);
        String flowerId = Registries.BLOCK.getId(flowerBlock).toString();
        content.putString("flower", flowerId);
        this.contentData.set(slot, content);
    }

    /**
     * 初始化花盆数据（空花盆）
     */
    private void initFlowerPotData(int slot) {
        if (slot < 0 || slot >= this.contentData.size()) {
            return;
        }
        this.contentData.set(slot, createEmptyContentData());
    }

    /**
     * 清除内容数据
     */
    private void clearContentData(int slot) {
        if (slot < 0 || slot >= this.contentData.size()) {
            return;
        }
        this.contentData.set(slot, createEmptyContentData());
    }

    /**
     * 清除花盆数据
     */
    private void clearFlowerData(int slot) {
        clearContentData(slot);
    }

    /**
     * 保存粉尘袋内容物
     */
    private void saveFlourContent(int slot, ItemStack flourSack) {
        if (slot < 0 || slot >= this.contentData.size()) {
            return;
        }

        Optional<ItemStack> flourContent = FlourSackItem.getFirstBundledStack(flourSack);
        if (flourContent.isPresent()) {
            NbtCompound content = new NbtCompound();
            content.putByte("Type", CONTENT_TYPE_FLOUR);

            NbtCompound flourNbt = new NbtCompound();
            flourContent.get().writeNbt(flourNbt);
            content.put("flour", flourNbt);

            this.contentData.set(slot, content);
        }
    }

    /**
     * 恢复粉尘袋内容物
     */
    private ItemStack restoreFlourContent(int slot, ItemStack flourSack) {
        if (slot < 0 || slot >= this.contentData.size()) {
            return flourSack;
        }

        NbtCompound content = this.contentData.getCompound(slot);
        if (content.getByte("Type") == CONTENT_TYPE_FLOUR && content.contains("flour")) {
            NbtCompound flourNbt = content.getCompound("flour");
            ItemStack flourStack = ItemStack.fromNbt(flourNbt);

            // 创建带有内容物的粉尘袋
            ItemStack resultSack = new ItemStack(flourSack.getItem());
            NbtCompound nbt = new NbtCompound();
            NbtList items = new NbtList();
            NbtCompound itemNbt = new NbtCompound();

            flourStack.writeNbt(itemNbt);
            items.add(itemNbt);
            nbt.put("Items", items);
            resultSack.setNbt(nbt);

            // 清除内容数据
            clearContentData(slot);

            return resultSack;
        }

        return flourSack;
    }

    /**
     * 创建空的内容数据
     */
    private NbtCompound createEmptyContentData() {
        NbtCompound content = new NbtCompound();
        content.putByte("Type", CONTENT_TYPE_EMPTY);
        return content;
    }

    /**
     * 检查是否有空花盆
     */
    public boolean hasEmptyFlowerPot() {
        for (int i = 0; i < this.size(); i++) {
            ItemStack stack = this.getStack(i);
            if (isFlowerPot(stack) && !hasFlower(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否有插了花的花盆
     */
    public boolean hasFloweredPot() {
        for (int i = 0; i < this.size(); i++) {
            if (isFlowerPot(this.getStack(i)) && hasFlower(i)) {
                return true;
            }
        }
        return false;
    }

    static {
        // 粉尘袋
        CanPlaceItem.add(stack -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof FlourSackBlock);
        // 花盆
        CanPlaceItem.add(stack -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof FlowerPotBlock);
        // 盐罐
        CanPlaceItem.add(stack -> stack.getItem() == ModItems.SALT_SHAKER);
    }
}