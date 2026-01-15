package org.foodcraft.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.block.PlateBlock;
import org.foodcraft.block.process.PlatingProcess;
import org.foodcraft.contentsystem.content.DishesContent;
import org.foodcraft.recipe.PlatingRecipe;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class PlateBlockEntity extends BlockEntity implements PlatableBlockEntity {
    /** 方块库存的最大容量，同时也是摆盘流程的最大步骤数量 */
    public static final int MAX_STEPS = 10;

    /**
     * 已放置的物品列表
     * <p>当{@linkplain #platingProcess}处于关闭状态时该列表应该为空</p>
     */
    protected final DefaultedList<ItemStack> placedItems;
    /** 摆盘流程 */
    protected final PlatingProcess<PlateBlockEntity> platingProcess;
    /** 摆盘配方的最终产物。
     * <p>该值与对应方块的{@link PlateBlock#IS_COMPLETION}属性绑定。
     * 当该值不为空时，对应的属性为true，反之，对应的属性为false</p>
     */
    @Nullable
    private DishesContent outcome;

    public PlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.PLATE, pos, state);
        this.placedItems = DefaultedList.ofSize(MAX_STEPS, ItemStack.EMPTY);
        this.platingProcess = new PlatingProcess<>();
    }

    /**
     * 尝试摆盘
     * @param player 摆盘的玩家
     * @param hand 交互的手
     * @param hit 交互的上下文
     * @return 交互的结果
     */
    public ActionResult tryPlating(PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!platingProcess.isActive()) {
            platingProcess.start(world, this);
        }

        return platingProcess.executeStep(this, getCachedState(), world, pos, player, hand, hit);
    }

    @Override
    public Item getContainerType() {
        return this.getCachedState().getBlock().asItem();
    }

    @Override
    public List<ItemStack> getPlacedItems() {
        List<ItemStack> result = DefaultedList.of();
        for (ItemStack stack : placedItems) {
            if (!stack.isEmpty()) {
                result.add(stack.copy());
            } else {
                // 遇到第一个空位就停止，保持步骤连续性
                break;
            }
        }
        return result;
    }

    @Override
    public boolean placeItem(int step, ItemStack item) {
        // 验证参数
        if (step < 0 || step >= MAX_STEPS || item.isEmpty()) {
            return false;
        }

        // 检查步骤连续性：前面的步骤必须有物品
        for (int i = 0; i < step; i++) {
            if (placedItems.get(i).isEmpty()) {
                return false;
            }
        }

        // 检查该步骤是否已有物品
        if (!placedItems.get(step).isEmpty()) {
            return false;
        }

        // 放置物品（数量为1的副本）
        ItemStack itemToPlace = item.copy();
        itemToPlace.setCount(1);
        placedItems.set(step, itemToPlace);

        // 标记脏状态以保存
        markDirty();

        return true;
    }

    @Override
    public ItemStack removeItem(int step) {
        // 验证参数
        if (step < 0 || step >= placedItems.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = placedItems.get(step);
        if (removed.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 移除该步骤及之后的所有物品（保持连续性）
        for (int i = step; i < placedItems.size(); i++) {
            placedItems.set(i, ItemStack.EMPTY);
        }

        markDirty();
        return removed;
    }

    @Override
    public void clearPlacedItems() {
        Collections.fill(placedItems, ItemStack.EMPTY);
        markDirty();
    }

    @Override
    public boolean isCompletionItem(ItemStack stack) {
        return stack.isOf(ModItems.PLATE_LID);
    }

    @Override
    public void onPlatingComplete(World world, BlockPos pos, PlatingRecipe recipe) {
        setOutcome(recipe.getDishes());
    }

    @Override
    public int size() {
        return MAX_STEPS;
    }

    @Override
    public boolean isEmpty() {
        return placedItems.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return placedItems.get(slot);
    }

    /**
     * 设置当前的菜肴，这会同时影响{@link PlateBlock#IS_COMPLETION}。
     * @param outcome 要设置的菜肴，为null时会清空当前的菜肴
     */
    public void setOutcome(@Nullable DishesContent outcome) {
        this.outcome = outcome;

        if (world == null) {
            return;
        }

        BlockState currentState = getCachedState();
        boolean shouldBeComplete = outcome != null;
        boolean isCurrentlyComplete = currentState.get(PlateBlock.IS_COMPLETION);

        // 只有当状态确实需要改变时才更新
        if (shouldBeComplete != isCurrentlyComplete) {
            BlockState newState = currentState.with(PlateBlock.IS_COMPLETION, shouldBeComplete);
            world.setBlockState(getPos(), newState, Block.NOTIFY_LISTENERS);
        }
    }

    /**
     * 获取当前的菜肴
     * @return 当前的菜肴
     */
    @Nullable
    public DishesContent getOutcome() {
        return outcome;
    }
}
