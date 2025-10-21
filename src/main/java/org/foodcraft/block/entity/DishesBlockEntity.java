package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.dfood.block.FoodBlock;
import org.dfood.item.DoubleBlockItem;
import org.dfood.shape.FoodShapeHandle;
import org.foodcraft.block.DishesBlock;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.util.FoodCraftUtils;

/**
 * 盘子方块实体，用于存储食物物品
 * 注意：item.getBlock()返回的Block必须是{@link FoodBlock}的实例，否则无法放入物品栏
 */
public class DishesBlockEntity extends UpPlaceBlockEntity {
    private static final int INVENTORY_SIZE = 1;
    private static final int MAX_STACK_SIZE = 11;
    private static final double FOOD_OFFSET_Y = 0.1;

    public DishesBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.DISHES, pos, state, INVENTORY_SIZE);
    }

    @Override
    public VoxelShape getContentShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        BlockState itemState = this.getInventoryBlockState();
        if (itemState.getBlock() instanceof FoodBlock foodBlock) {
            return FoodShapeHandle.getInstance().getShape(itemState, foodBlock.NUMBER_OF_FOOD)
                    .offset(0.0, FOOD_OFFSET_Y, 0.0);
        }
        return FoodShapeHandle.shapes.ALL.getShape().offset(0.0, FOOD_OFFSET_Y, 0.0);
    }

    @Override
    public boolean isValidItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // 蛋糕可以直接放入
        if (stack.getItem() == Items.CAKE) {
            return true;
        }

        Item item = stack.getItem();
        if (item instanceof DoubleBlockItem doubleBlockItem) {
            return doubleBlockItem.getSecondBlock() instanceof FoodBlock;
        } else if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock() instanceof FoodBlock;
        }
        return false;
    }

    /**
     * 获取当前物品栏中的物品对应的方块状态
     * @return 物品对应的方块状态
     */
    public BlockState getInventoryBlockState() {
        ItemStack stack = this.inventory.get(0);
        Direction facing = this.getCachedState().get(DishesBlock.FACING);

        return FoodCraftUtils.createCountBlockstate(stack, facing);
    }

    public ActionResult tryAddItem(ItemStack stack) {
        if (stack.isEmpty() || !isValidItem(stack)) {
            return ActionResult.FAIL;
        }

        Item item = stack.getItem();
        ItemStack newStack = stack.copy();
        newStack.setCount(1);
        ItemStack currentStack = this.getStack(0);

        if (currentStack.isEmpty()) {
            this.setStack(0, newStack);
            this.markDirtyAndSync();
            return ActionResult.SUCCESS;
        } else if (currentStack.getItem() == item) {
            FoodBlock block = (FoodBlock) getInventoryBlockState().getBlock();
            if (currentStack.getCount() < block.MAX_FOOD) {
                currentStack.increment(1);
                this.markDirtyAndSync();
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.FAIL;
    }

    public ActionResult tryFetchItem(PlayerEntity player) {
        ItemStack contentStack = this.getStack(0);
        if (contentStack.isEmpty()) {
            return ActionResult.FAIL;
        }

        // 创建一个物品堆栈副本用于给予玩家
        ItemStack extractedStack = contentStack.copy();
        extractedStack.setCount(1);

        // 给予玩家物品
        if (!player.isCreative() && !player.giveItemStack(extractedStack)) {
            player.dropItem(extractedStack, false); // 背包满时掉落
        }

        // 减少容器中的物品数量
        contentStack.decrement(1);
        if (contentStack.isEmpty()) {
            this.setStack(0, ItemStack.EMPTY);
        }

        this.markDirtyAndSync();
        return ActionResult.SUCCESS;
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