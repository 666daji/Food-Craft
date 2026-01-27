package org.foodcraft.block.entity;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
import org.foodcraft.util.FoodCraftUtils;
import org.foodcraft.mixin.FlowerPotBlockAccessor;

import java.util.*;
import java.util.function.Predicate;

public class ShelfBlockEntity extends UpPlaceBlockEntity {
    private static final int INVENTORY_SIZE = 2;
    private static final int MAX_STACK_SIZE = 1;

    // 使用独立的字符串字段存储花信息
    private String flowerSlot0 = "";  // 槽位0的花ID，空字符串表示没有花
    private String flowerSlot1 = "";  // 槽位1的花ID，空字符串表示没有花

    /**
     * 允许放置在架子上的物品
     * @see ShelfBlockEntity#isValidItem
     * */
    public static final Set<Predicate<ItemStack>> CanPlaceItem = new HashSet<>();

    public ShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SHELF, pos, state, INVENTORY_SIZE);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        // 从NBT中读取花信息
        this.flowerSlot0 = nbt.getString("Flower0");
        this.flowerSlot1 = nbt.getString("Flower1");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        // 将花信息写入NBT
        nbt.putString("Flower0", this.flowerSlot0);
        nbt.putString("Flower1", this.flowerSlot1);
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
            String flowerId = getFlowerId(index);
            if (!flowerId.isEmpty()) {
                Block flowerBlock = Registries.BLOCK.get(new Identifier(flowerId));
                if (flowerBlock != null) {
                    Map<Block, Block> contentToPotted = FlowerPotBlockAccessor.getContentToPotted();
                    Block pottedBlock = contentToPotted.get(flowerBlock);
                    if (pottedBlock != null) {
                        return pottedBlock.getDefaultState();
                    }
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

        // 放置普通物品（包括粉尘袋）
        ItemStack newStack = stack.copy();
        newStack.setCount(1);
        int emptySlot = this.foundSlot();
        if (emptySlot != -1) {
            this.setStack(emptySlot, newStack);
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
        String flowerId = Registries.BLOCK.getId(flowerBlock).toString();

        // 寻找第一个空的或可插入的花盆槽位
        for (int i = 0; i < this.size(); i++) {
            ItemStack slotStack = this.getStack(i);
            if (isFlowerPot(slotStack) && !hasFlower(i)) {
                // 设置花盆插花数据
                setFlowerId(i, flowerId);
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

                // 直接取出物品
                ItemStack extractedStack = stack.copy();
                extractedStack.setCount(1);

                this.fetchStacks = List.of(extractedStack.copy());

                if (!player.isCreative() && !player.giveItemStack(extractedStack)) {
                    player.dropItem(extractedStack, false);
                }

                stack.decrement(1);
                if (stack.isEmpty()) {
                    this.setStack(i, ItemStack.EMPTY);
                    // 清除花盆数据
                    clearFlowerId(i);
                }

                this.markDirtyAndSync();
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.FAIL;
    }

    /**
     * 获取所有掉落物，包括花盆中的花
     */
    public DefaultedList<ItemStack> getDroppedStacks() {
        DefaultedList<ItemStack> drops = DefaultedList.of();

        // 添加库存中的物品
        for (int i = 0; i < this.size(); i++) {
            ItemStack stack = this.getStack(i);
            if (!stack.isEmpty()) {
                // 如果是花盆且有花，掉落花
                if (isFlowerPot(stack) && hasFlower(i)) {
                    String flowerId = getFlowerId(i);
                    if (!flowerId.isEmpty()) {
                        Block flowerBlock = Registries.BLOCK.get(new Identifier(flowerId));
                        if (flowerBlock != null) {
                            drops.add(new ItemStack(flowerBlock));
                        }
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
        String flowerId = getFlowerId(slot);
        if (flowerId.isEmpty()) {
            return ActionResult.FAIL;
        }

        Block flowerBlock = Registries.BLOCK.get(new Identifier(flowerId));
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
        clearFlowerId(slot);
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
        // 确保花信息包含在区块数据中
        nbt.putString("Flower0", this.flowerSlot0);
        nbt.putString("Flower1", this.flowerSlot1);
        return nbt;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
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
        return !getFlowerId(slot).isEmpty();
    }

    /**
     * 获取指定槽位的花ID
     */
    public String getFlowerId(int slot) {
        return switch (slot) {
            case 0 -> this.flowerSlot0;
            case 1 -> this.flowerSlot1;
            default -> "";
        };
    }

    /**
     * 设置指定槽位的花ID
     */
    private void setFlowerId(int slot, String flowerId) {
        switch (slot) {
            case 0 -> this.flowerSlot0 = flowerId;
            case 1 -> this.flowerSlot1 = flowerId;
        }
    }

    /**
     * 清除指定槽位的花ID
     */
    private void clearFlowerId(int slot) {
        setFlowerId(slot, "");
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
        CanPlaceItem.add(stack -> stack.getItem() instanceof FlourSackItem && FlourSackItem.getBundledStack(stack).isPresent());
        // 花盆
        CanPlaceItem.add(stack -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof FlowerPotBlock);
    }
}