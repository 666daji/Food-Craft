package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.dfood.block.FoodBlock;
import org.foodcraft.block.entity.FlourSackBlockEntity;
import org.foodcraft.item.FlourItem;
import org.foodcraft.item.FlourSackItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FlourSackBlock extends FoodBlock implements BlockEntityProvider {
    public static final IntProperty SHELF_INDEX = IntProperty.of("shelf_index", 0, 1);

    public FlourSackBlock(Settings settings, int maxFood) {
        super(settings, maxFood);
        this.setDefaultState(this.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(NUMBER_OF_FOOD, 1)
                .with(SHELF_INDEX, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(SHELF_INDEX);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx))
                .with(SHELF_INDEX, 0);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FlourSackBlockEntity(pos, state);
    }

    @Override
    protected boolean tryAdd(BlockState state, World world, BlockPos pos, PlayerEntity player, ItemStack handStack, BlockEntity blockEntity) {
        if (isSackStackingOperation(handStack, blockEntity)) {
            return handleSackStacking(state, world, pos, handStack, (FlourSackBlockEntity) blockEntity);
        }

        return false;
    }

    @Override
    protected boolean tryRemove(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockEntity blockEntity) {
        if (isSackRemovalOperation(blockEntity)) {
            return handleSackRemoval(state, world, pos, player, (FlourSackBlockEntity) blockEntity);
        }

        // 回退到父类逻辑
        return super.tryRemove(state, world, pos, player, blockEntity);
    }

    @Override
    public boolean isSame(ItemStack stack, BlockEntity blockEntity) {
        if (!(blockEntity instanceof FlourSackBlockEntity flourSackEntity)) {
            return false;
        }

        // 检查基础物品类型
        if (stack.getItem() != this.asItem()) {
            return false;
        }

        // 根据物品类型进行兼容性检查
        if (stack.getItem() instanceof FlourSackItem) {
            return areSackContentsCompatible(stack, flourSackEntity);
        } else if (stack.getItem() instanceof FlourItem) {
            return isFlourCompatibleWithSacks(stack, flourSackEntity);
        }

        return false;
    }

    @Override
    public ItemStack createStack(int count, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof FlourSackBlockEntity flourSackEntity) {
            return createSackItemFromEntity(flourSackEntity);
        }
        return super.createStack(count, blockEntity);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        BlockEntity blockEntity = builder.get(LootContextParameters.BLOCK_ENTITY);

        if (blockEntity instanceof FlourSackBlockEntity flourSackEntity) {
            DefaultedList<ItemStack> droppedStacks = FlourSackItem.fromBlockEntity(flourSackEntity);
            if (!droppedStacks.isEmpty()) {
                return droppedStacks;
            }
        }

        return super.getDroppedStacks(state, builder);
    }

    /**
     * 检查是否为粉尘袋堆叠操作
     */
    private boolean isSackStackingOperation(ItemStack handStack, BlockEntity blockEntity) {
        return handStack.getItem() instanceof FlourSackItem &&
                blockEntity instanceof FlourSackBlockEntity;
    }

    /**
     * 检查是否为粉尘袋取出操作
     */
    private boolean isSackRemovalOperation(BlockEntity blockEntity) {
        return blockEntity instanceof FlourSackBlockEntity flourSackEntity &&
                flourSackEntity.getSackCount() > 1;
    }

    /**
     * 处理粉尘袋堆叠
     */
    private boolean handleSackStacking(BlockState state, World world, BlockPos pos,
                                       ItemStack handStack, FlourSackBlockEntity entity) {
        if (entity.canAddSack(handStack) && entity.addSack(handStack)) {
            updateBlockState(world, pos, state, entity);
            return true;
        }
        return false;
    }

    /**
     * 处理粉尘袋取出
     */
    private boolean handleSackRemoval(BlockState state, World world, BlockPos pos,
                                      PlayerEntity player, FlourSackBlockEntity entity) {
        ItemStack removedSack = entity.removeSack();
        if (!removedSack.isEmpty()) {
            giveItemToPlayer(player, removedSack);
            updateBlockState(world, pos, state, entity);
            return true;
        }
        return false;
    }

    /**
     * 检查粉尘袋内容物兼容性
     */
    private boolean areSackContentsCompatible(ItemStack stack, FlourSackBlockEntity entity) {
        Optional<ItemStack> handContent = FlourSackItem.getFirstBundledStack(stack);

        for (int i = 0; i < entity.getSackCount(); i++) {
            ItemStack sackContent = entity.getSackContent(i);

            if (!isContentCompatible(sackContent, handContent)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查粉尘物品兼容性
     */
    private boolean isFlourCompatibleWithSacks(ItemStack flourStack, FlourSackBlockEntity entity) {
        for (int i = 0; i < entity.getSackCount(); i++) {
            ItemStack sackContent = entity.getSackContent(i);
            if (!sackContent.isEmpty() && !ItemStack.areItemsEqual(sackContent, flourStack)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查内容物兼容性
     */
    private boolean isContentCompatible(ItemStack existingContent, Optional<ItemStack> newContent) {
        boolean hasExistingContent = !existingContent.isEmpty();
        boolean hasNewContent = newContent.isPresent();

        if (hasExistingContent && hasNewContent) {
            return ItemStack.areItemsEqual(existingContent, newContent.get());
        }

        return false;
    }

    /**
     * 从方块实体创建粉尘袋物品
     */
    private ItemStack createSackItemFromEntity(FlourSackBlockEntity entity) {
        ItemStack sackItem = new ItemStack(this.asItem());

        if (entity.getSackCount() > 0) {
            ItemStack firstContent = entity.getSackContent(0);
            if (!firstContent.isEmpty()) {
                NbtCompound nbt = createSackNbt(firstContent);
                sackItem.setNbt(nbt);
            }
        }

        return sackItem;
    }

    /**
     * 创建粉尘袋NBT数据
     */
    private NbtCompound createSackNbt(ItemStack content) {
        NbtCompound nbt = new NbtCompound();
        NbtList items = new NbtList();
        NbtCompound itemNbt = new NbtCompound();

        content.writeNbt(itemNbt);
        items.add(itemNbt);
        nbt.put(FlourSackBlockEntity.ITEMS_KEY, items);

        return nbt;
    }

    /**
     * 给玩家物品
     */
    private void giveItemToPlayer(PlayerEntity player, ItemStack stack) {
        if (!player.isCreative()) {
            if (!player.giveItemStack(stack)) {
                player.dropItem(stack, false);
            }
        }
    }

    /**
     * 更新方块状态
     */
    private void updateBlockState(World world, BlockPos pos, BlockState state, FlourSackBlockEntity entity) {
        int sackCount = entity.getSackCount();

        if (sackCount <= 0) {
            world.breakBlock(pos, false);
        } else {
            BlockState newState = state.with(NUMBER_OF_FOOD, sackCount);
            world.setBlockState(pos, newState, Block.NOTIFY_ALL);
        }
    }

    /**
     * 获取指定位置的粉尘袋物品堆栈
     */
    public ItemStack getSackStack(World world, BlockPos pos, int index) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof FlourSackBlockEntity flourSackEntity) {
            return flourSackEntity.getSackStack(index);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取指定位置的粉尘颜色
     */
    public int getFlourColor(World world, BlockPos pos, int index) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof FlourSackBlockEntity flourSackEntity) {
            return flourSackEntity.getFlourColor(index);
        }
        return FlourSackBlockEntity.DEFAULT_FLOUR_COLOR;
    }

    /**
     * 获取所有堆叠的粉尘颜色
     */
    public int[] getAllFlourColors(World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof FlourSackBlockEntity flourSackEntity) {
            return flourSackEntity.getAllFlourColors();
        }
        return new int[0];
    }

    /**
     * 检查指定索引是否有效
     */
    public boolean isValidSackIndex(World world, BlockPos pos, int index) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof FlourSackBlockEntity flourSackEntity &&
                flourSackEntity.isValidSackIndex(index);
    }
}