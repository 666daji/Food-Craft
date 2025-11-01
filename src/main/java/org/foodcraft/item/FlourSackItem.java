package org.foodcraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.foodcraft.block.entity.FlourSackBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FlourSackItem extends BlockItem {
    private static final String ITEMS_KEY = "Items";
    private static final int MAX_STORAGE = 16;
    private static final int ITEM_BAR_COLOR = MathHelper.packRgb(0.4F, 0.4F, 1.0F);
    private static final int ITEM_OCCUPANCY = 1; // 每个物品占用空间

    public FlourSackItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        ItemStack stack = context.getStack();

        // 只有装有粉尘的粉尘袋才能放置
        if (getBundleOccupancy(stack) == 0) {
            return ActionResult.FAIL;
        }

        return super.useOnBlock(context);
    }

    @Override
    protected boolean place(ItemPlacementContext context, BlockState state) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        ItemStack itemStack = context.getStack();

        if (!world.setBlockState(pos, state, Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD)) {
            return false;
        }

        // 将物品数据写入方块实体
        initializeBlockEntity(world, pos, itemStack);
        return true;
    }

    @Override
    public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT) {
            return false;
        }

        ItemStack slotStack = slot.getStack();
        if (slotStack.isEmpty()) {
            handleRemoveFromBundle(stack, slot, player);
        } else if (canAcceptItem(slotStack)) {
            handleAddToBundle(stack, slot, slotStack, player);
        }

        return true;
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType == ClickType.RIGHT && slot.canTakePartial(player)) {
            if (otherStack.isEmpty()) {
                handleRemoveToCursor(stack, cursorStackReference, player);
            } else if (canAcceptItem(otherStack)) {
                handleAddFromCursor(stack, otherStack, cursorStackReference, player);
            }
            return true;
        }
        return false;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (dropAllBundledItems(itemStack, user)) {
            playDropContentsSound(user);
            user.incrementStat(Stats.USED.getOrCreateStat(this));
            return TypedActionResult.success(itemStack, world.isClient());
        } else {
            return TypedActionResult.fail(itemStack);
        }
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getBundleOccupancy(stack) > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.min(1 + 12 * getBundleOccupancy(stack) / MAX_STORAGE, 13);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return ITEM_BAR_COLOR;
    }

    public record FlourSackTooltipData(Optional<ItemStack> content, int occupancy, int maxStorage) implements TooltipData {}

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        Optional<ItemStack> content = getFirstBundledStack(stack);
        return Optional.of(new FlourSackTooltipData(content, getBundleOccupancy(stack), MAX_STORAGE));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        appendCapacityTooltip(stack, tooltip);
        appendContentTooltip(stack, tooltip);
        appendUsageTooltip(stack, tooltip);
        appendStackInfoTooltip(stack, tooltip);
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        ItemUsage.spawnItemContents(entity, getBundledStacks(entity.getStack()));
    }

    /**
     * 获取收纳袋的填充比例（0.0 - 1.0）
     */
    public static float getAmountFilled(ItemStack stack) {
        return getBundleOccupancy(stack) / (float) MAX_STORAGE;
    }

    /**
     * 获取收纳袋中的第一个物品栈
     */
    public static Optional<ItemStack> getFirstBundledStack(ItemStack stack) {
        return getBundledStacks(stack).findFirst();
    }

    /**
     * 从方块实体创建物品堆栈
     */
    public static DefaultedList<ItemStack> fromBlockEntity(FlourSackBlockEntity blockEntity) {
        DefaultedList<ItemStack> result = DefaultedList.of();

        for (int i = 0; i < blockEntity.getSackCount(); i++) {
            ItemStack sackContent = blockEntity.getSackContent(i);
            ItemStack sackItem = createSackItem(blockEntity, sackContent);
            result.add(sackItem);
        }

        return result;
    }

    /**
     * 检查两个粉尘袋是否可以堆叠
     */
    public static boolean canStackWith(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) return false;

        Optional<ItemStack> content1 = getFirstBundledStack(stack1);
        Optional<ItemStack> content2 = getFirstBundledStack(stack2);

        // 两个空袋可以堆叠
        if (content1.isEmpty() && content2.isEmpty()) return true;

        // 一个有内容一个空，不能堆叠
        if (content1.isEmpty() != content2.isEmpty()) return false;

        // 检查内容物是否相同
        return ItemStack.areItemsEqual(content1.get(), content2.get());
    }

    /**
     * 检查物品是否可以被收纳袋接受
     */
    private static boolean canAcceptItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof FlourItem;
    }

    /**
     * 添加物品到收纳袋
     */
    private static int addToBundle(ItemStack bundle, ItemStack stack) {
        if (!canAcceptItem(stack)) {
            return 0;
        }

        NbtCompound nbt = bundle.getOrCreateNbt();
        NbtList items = getOrCreateItemsList(nbt);

        int currentOccupancy = getBundleOccupancy(bundle);
        int availableSpace = MAX_STORAGE - currentOccupancy;
        int maxToAdd = Math.min(stack.getCount(), availableSpace);

        if (maxToAdd <= 0) {
            return 0;
        }

        if (items.isEmpty()) {
            return addNewItemToBundle(items, stack, maxToAdd);
        } else {
            return mergeWithExistingItem(items, stack, maxToAdd);
        }
    }

    /**
     * 获取收纳袋当前占用的总空间
     */
    private static int getBundleOccupancy(ItemStack stack) {
        return getBundledStacks(stack)
                .mapToInt(itemStack -> ITEM_OCCUPANCY * itemStack.getCount())
                .sum();
    }

    /**
     * 从收纳袋中取出第一个物品栈
     */
    private static Optional<ItemStack> removeFirstStack(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.contains(ITEMS_KEY)) {
            return Optional.empty();
        }

        NbtList items = nbt.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE);
        if (items.isEmpty()) {
            return Optional.empty();
        }

        NbtCompound itemNbt = items.getCompound(0);
        ItemStack itemStack = ItemStack.fromNbt(itemNbt);
        items.remove(0);

        if (items.isEmpty()) {
            stack.removeSubNbt(ITEMS_KEY);
        }

        return Optional.of(itemStack);
    }

    /**
     * 丢弃收纳袋中的所有物品
     */
    private static boolean dropAllBundledItems(ItemStack stack, PlayerEntity player) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.contains(ITEMS_KEY)) {
            return false;
        }

        if (player instanceof ServerPlayerEntity) {
            NbtList items = nbt.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE);
            dropItemsToPlayer(items, player);
        }

        stack.removeSubNbt(ITEMS_KEY);
        return true;
    }

    /**
     * 获取收纳袋中的所有物品栈
     */
    private static Stream<ItemStack> getBundledStacks(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(ITEMS_KEY)) {
            return Stream.empty();
        }

        NbtList items = nbt.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE);
        return items.stream()
                .map(NbtCompound.class::cast)
                .map(ItemStack::fromNbt);
    }

    /**
     * 初始化方块实体
     */
    private void initializeBlockEntity(World world, BlockPos pos, ItemStack itemStack) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof FlourSackBlockEntity flourSackBlockEntity) {
            NbtCompound itemNbt = itemStack.getOrCreateNbt();
            flourSackBlockEntity.readItemNbt(itemNbt);
            flourSackBlockEntity.setSackCount(itemStack.getCount());
        }
    }

    /**
     * 处理从收纳袋取出物品
     */
    private void handleRemoveFromBundle(ItemStack stack, Slot slot, PlayerEntity player) {
        playRemoveOneSound(player);
        removeFirstStack(stack).ifPresent(removedStack -> {
            ItemStack remaining = slot.insertStack(removedStack);
            if (!remaining.isEmpty()) {
                addToBundle(stack, remaining);
            }
        });
    }

    /**
     * 处理向收纳袋添加物品
     */
    private void handleAddToBundle(ItemStack stack, Slot slot, ItemStack slotStack, PlayerEntity player) {
        int availableSpace = MAX_STORAGE - getBundleOccupancy(stack);
        int maxToAdd = Math.min(slotStack.getCount(), availableSpace);

        if (maxToAdd > 0) {
            ItemStack toAdd = slot.takeStackRange(slotStack.getCount(), maxToAdd, player);
            int actuallyAdded = addToBundle(stack, toAdd);
            if (actuallyAdded > 0) {
                playInsertSound(player);
            }
        }
    }

    /**
     * 处理取出物品到光标
     */
    private void handleRemoveToCursor(ItemStack stack, StackReference cursorStackReference, PlayerEntity player) {
        removeFirstStack(stack).ifPresent(itemStack -> {
            playRemoveOneSound(player);
            cursorStackReference.set(itemStack);
        });
    }

    /**
     * 处理从光标添加物品
     */
    private void handleAddFromCursor(ItemStack stack, ItemStack otherStack, StackReference cursorStackReference, PlayerEntity player) {
        int availableSpace = MAX_STORAGE - getBundleOccupancy(stack);
        int maxToAdd = Math.min(otherStack.getCount(), availableSpace);

        if (maxToAdd > 0) {
            ItemStack toAdd = otherStack.copyWithCount(maxToAdd);
            int actuallyAdded = addToBundle(stack, toAdd);
            if (actuallyAdded > 0) {
                playInsertSound(player);
                otherStack.decrement(actuallyAdded);
            }
        }
    }

    /**
     * 创建粉尘袋物品
     */
    private static ItemStack createSackItem(FlourSackBlockEntity blockEntity, ItemStack content) {
        ItemStack sackItem = new ItemStack(blockEntity.getCachedState().getBlock().asItem());

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
     * 获取或创建物品列表
     */
    private static NbtList getOrCreateItemsList(NbtCompound nbt) {
        if (!nbt.contains(ITEMS_KEY)) {
            nbt.put(ITEMS_KEY, new NbtList());
        }
        return nbt.getList(ITEMS_KEY, NbtElement.COMPOUND_TYPE);
    }

    /**
     * 添加新物品到收纳袋
     */
    private static int addNewItemToBundle(NbtList items, ItemStack stack, int maxToAdd) {
        ItemStack toAdd = stack.copyWithCount(maxToAdd);
        NbtCompound itemNbt = new NbtCompound();
        toAdd.writeNbt(itemNbt);
        items.add(itemNbt);
        return maxToAdd;
    }

    /**
     * 合并到现有物品
     */
    private static int mergeWithExistingItem(NbtList items, ItemStack stack, int maxToAdd) {
        NbtCompound existingItemNbt = items.getCompound(0);
        ItemStack existingItem = ItemStack.fromNbt(existingItemNbt);

        if (ItemStack.canCombine(existingItem, stack)) {
            int canMerge = Math.min(maxToAdd, existingItem.getMaxCount() - existingItem.getCount());
            if (canMerge > 0) {
                existingItem.increment(canMerge);
                existingItem.writeNbt(existingItemNbt);
                return canMerge;
            }
        }
        return 0;
    }

    /**
     * 向玩家掉落物品
     */
    private static void dropItemsToPlayer(NbtList items, PlayerEntity player) {
        for (int i = 0; i < items.size(); i++) {
            NbtCompound itemNbt = items.getCompound(i);
            ItemStack itemStack = ItemStack.fromNbt(itemNbt);
            player.dropItem(itemStack, true);
        }
    }

    private void appendCapacityTooltip(ItemStack stack, List<Text> tooltip) {
        int occupancy = getBundleOccupancy(stack);
        tooltip.add(Text.translatable("item.foodcraft.flour_sack.fullness", occupancy, MAX_STORAGE)
                .formatted(Formatting.GRAY));
    }

    private void appendContentTooltip(ItemStack stack, List<Text> tooltip) {
        getFirstBundledStack(stack).ifPresent(content -> {
            MutableText contentText = Text.translatable("item.foodcraft.flour_sack.content",
                    content.getName(), content.getCount());
            tooltip.add(contentText.formatted(Formatting.GRAY));
        });
    }

    private void appendUsageTooltip(ItemStack stack, List<Text> tooltip) {
        int occupancy = getBundleOccupancy(stack);
        String translationKey = occupancy == 0 ?
                "item.foodcraft.flour_sack.tooltip.empty" :
                "item.foodcraft.flour_sack.tooltip.non_empty";

        tooltip.add(Text.translatable(translationKey)
                .formatted(Formatting.ITALIC, Formatting.DARK_GRAY));
    }

    private void appendStackInfoTooltip(ItemStack stack, List<Text> tooltip) {
        if (stack.getCount() > 1) {
            tooltip.add(Text.translatable("item.foodcraft.flour_sack.stack_count", stack.getCount())
                    .formatted(Formatting.DARK_BLUE));
        }
    }

    private void playRemoveOneSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F,
                0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
    }

    private void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_INSERT, 0.8F,
                0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
    }

    private void playDropContentsSound(Entity entity) {
        entity.playSound(SoundEvents.ITEM_BUNDLE_DROP_CONTENTS, 0.8F,
                0.8F + entity.getWorld().getRandom().nextFloat() * 0.4F);
    }
}