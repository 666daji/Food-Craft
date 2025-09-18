package org.foodcraft.block.entity;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.foodcraft.block.FlourSackBlock;
import org.foodcraft.block.ShelfBlock;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.registry.ModItems;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ShelfBlockEntity extends UpPlaceBlockEntity{
    private static final int INVENTORY_SIZE = 2;
    private static final int MAX_STACK_SIZE = 1;

    public static final Set<Predicate<ItemStack>> CanPlaceItem = new HashSet<>();

    public ShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SHELF, pos, state, INVENTORY_SIZE);
        // 粉尘袋
        CanPlaceItem.add(stack -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof FlourSackBlock);
        // 花盆
        CanPlaceItem.add(stack -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof FlowerPotBlock);
        // 盐罐
        CanPlaceItem.add(stack -> stack.getItem() == ModItems.SALT_SHAKER);
    }

    @Override
    public void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= this.size()) {
            throw new IllegalArgumentException("Slot " + slot + " not in valid range - [0," + this.size() + ")");
        }
    }

    @Override
    public VoxelShape getContentShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = this.getCachedState().get(Properties.HORIZONTAL_FACING);

        // 根据朝向计算偏移量
        double xOffset = 0.0;
        double zOffset = 0.0;

        // 根据槽位和朝向计算不同的偏移位置
        for (int i = 0; i < this.size(); i++) {
            if (!this.getStack(i).isEmpty()) {
                BlockState itemState = this.getInventoryBlockState(i);
                if (!itemState.isAir()) {
                    VoxelShape itemShape = itemState.getOutlineShape(world, pos, context);

                    // 根据架子的朝向和槽位计算偏移
                    switch (direction) {
                        case NORTH:
                            xOffset = i == 0 ? -0.2 : 0.2; // 左侧或右侧
                            zOffset = 0.0;
                            break;
                        case SOUTH:
                            xOffset = i == 0 ? 0.2 : -0.2; // 反转左右
                            zOffset = 0.0;
                            break;
                        case EAST:
                            xOffset = 0.0;
                            zOffset = i == 0 ? -0.2 : 0.2;
                            break;
                        case WEST:
                            xOffset = 0.0;
                            zOffset = i == 0 ? 0.2 : -0.2;
                            break;
                        default:
                            xOffset = 0.0;
                            zOffset = 0.0;
                    }

                    // 合并所有物品的形状
                    if (itemShape != null && !itemShape.isEmpty()) {
                        return itemShape.offset(xOffset, 0.3, zOffset);
                    }
                }
            }
        }

        return VoxelShapes.empty();
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
     * 获取当前物品栏中的物品对应的方块状态
     * @return 物品对应的方块状态
     */
    public BlockState getInventoryBlockState(int index) {
        ItemStack stack = this.inventory.get(index);
        Item item = stack.getItem();

        if (item instanceof BlockItem blockItem){
            if (hasProperty(blockItem.getBlock(), Properties.HORIZONTAL_FACING)){
                return blockItem.getBlock().getDefaultState()
                        .with(Properties.HORIZONTAL_FACING, this.getCachedState().get(ShelfBlock.FACING));
            }
            // 当对应方块没有HORIZONTAL_FACING属性时返回默认方块状态
            return blockItem.getBlock().getDefaultState();
        }

        return Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean isValidItem(ItemStack stack) {
        for (Predicate<ItemStack> canItem : CanPlaceItem) {
            if (canItem.test(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 物品的取出放置逻辑为先进后出
     * @param stack 要添加的物品堆栈
     * @return
     */
    @Override
    public ActionResult tryAddItem(ItemStack stack) {
        if (stack.isEmpty() || !isValidItem(stack)) {
            return ActionResult.FAIL;
        }

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
                // 创建一个物品堆栈副本用于给予玩家
                ItemStack extractedStack = stack.copy();
                extractedStack.setCount(1);

                // 给予玩家物品
                if (!player.isCreative() && !player.giveItemStack(extractedStack)) {
                    player.dropItem(extractedStack, false); // 背包满时掉落
                }

                // 减少架子中的物品数量
                stack.decrement(1);
                if (stack.isEmpty()) {
                    this.setStack(i, ItemStack.EMPTY);
                }

                this.markDirtyAndSync();
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.FAIL;
    }

    @Override
    public int getMaxCountPerStack() {
        return MAX_STACK_SIZE;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
