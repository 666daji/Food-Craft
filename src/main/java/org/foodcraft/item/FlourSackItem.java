package org.foodcraft.item;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 是专门用于存放{@linkplain FlourItem}的袋子。
 */
public class FlourSackItem extends BlockItem {
    public static final String STORED_ITEM_KEY = "StoredFlour";  // 存储完整物品堆栈
    private static final int MAX_STORAGE = 16;
    private static final int ITEM_BAR_COLOR = MathHelper.packRgb(0.4F, 0.4F, 1.0F);

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
        Optional<ItemStack> content = getBundledStack(stack);
        return Optional.of(new FlourSackTooltipData(content, getBundleOccupancy(stack), MAX_STORAGE));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        appendCapacityTooltip(stack, tooltip);
        appendContentTooltip(stack, tooltip);
        appendUsageTooltip(stack, tooltip);
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        ItemStack stack = entity.getStack();
        Optional<ItemStack> bundledStack = getBundledStack(stack);

        if (bundledStack.isPresent() && !bundledStack.get().isEmpty()) {
            // 将单个物品堆栈转换为Stream
            Stream<ItemStack> contents = Stream.of(bundledStack.get());
            ItemUsage.spawnItemContents(entity, contents);
        }
    }

    /**
     * 获取粉尘袋的填充比例（0.0 - 1.0）
     */
    public static float getAmountFilled(ItemStack stack) {
        return getBundleOccupancy(stack) / (float) MAX_STORAGE;
    }

    /**
     * 获取粉尘袋中存储的物品栈
     */
    public static Optional<ItemStack> getBundledStack(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(STORED_ITEM_KEY)) {
            return Optional.empty();
        }

        NbtCompound storedNbt = nbt.getCompound(STORED_ITEM_KEY);
        return Optional.of(ItemStack.fromNbt(storedNbt));
    }

    /**
     * 检查两个粉尘袋是否可以堆叠
     */
    public static boolean canStackWith(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) return false;

        Optional<ItemStack> content1 = getBundledStack(stack1);
        Optional<ItemStack> content2 = getBundledStack(stack2);

        // 两个空袋可以堆叠
        if (content1.isEmpty() && content2.isEmpty()) return true;

        // 一个有内容一个空，不能堆叠
        if (content1.isEmpty() != content2.isEmpty()) return false;

        // 检查内容物是否相同（包括NBT）
        return ItemStack.areEqual(content1.get(), content2.get());
    }

    /**
     * 检查物品是否可以被粉尘袋接受
     */
    private static boolean canAcceptItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof FlourItem;
    }

    /**
     * 添加物品到粉尘袋
     */
    private static int addToBundle(ItemStack bundle, ItemStack stack) {
        if (!canAcceptItem(stack)) {
            return 0;
        }

        NbtCompound nbt = bundle.getOrCreateNbt();
        int currentCount = getBundleOccupancy(bundle);
        int availableSpace = MAX_STORAGE - currentCount;
        int maxToAdd = Math.min(stack.getCount(), availableSpace);

        if (maxToAdd <= 0) {
            return 0;
        }

        // 如果粉尘袋是空的，设置物品
        if (!nbt.contains(STORED_ITEM_KEY)) {
            ItemStack copy = stack.copyWithCount(maxToAdd);
            NbtCompound storedNbt = new NbtCompound();
            copy.writeNbt(storedNbt);
            nbt.put(STORED_ITEM_KEY, storedNbt);
            return maxToAdd;
        }
        // 如果已经有物品，检查是否可以合并
        else {
            ItemStack existingStack = ItemStack.fromNbt(nbt.getCompound(STORED_ITEM_KEY));

            // 检查是否为同一物品（包括NBT）
            if (ItemStack.canCombine(existingStack, stack)) {
                int newTotal = existingStack.getCount() + maxToAdd;
                if (newTotal > MAX_STORAGE) {
                    maxToAdd = MAX_STORAGE - existingStack.getCount();
                    if (maxToAdd <= 0) return 0;
                    newTotal = MAX_STORAGE;
                }

                existingStack.setCount(newTotal);
                NbtCompound storedNbt = new NbtCompound();
                existingStack.writeNbt(storedNbt);
                nbt.put(STORED_ITEM_KEY, storedNbt);
                return maxToAdd;
            } else {
                // 不同种类的粉，不能添加
                return 0;
            }
        }
    }

    /**
     * 获取粉尘袋当前占用的总空间（也就是存储的数量）
     */
    private static int getBundleOccupancy(ItemStack stack) {
        return getBundledStack(stack)
                .map(ItemStack::getCount)
                .orElse(0);
    }

    /**
     * 从粉尘袋中取出所有物品栈
     */
    private static Optional<ItemStack> removeAllStack(ItemStack stack) {
        Optional<ItemStack> bundledStack = getBundledStack(stack);
        if (bundledStack.isPresent() && !bundledStack.get().isEmpty()) {
            stack.removeSubNbt(STORED_ITEM_KEY);
            return bundledStack;
        }
        return Optional.empty();
    }

    /**
     * 从粉尘袋中取出指定数量的物品
     */
    private static Optional<ItemStack> removeSomeStack(ItemStack stack, int amount) {
        Optional<ItemStack> bundledStack = getBundledStack(stack);
        if (bundledStack.isPresent() && !bundledStack.get().isEmpty()) {
            ItemStack storedStack = bundledStack.get();
            int storedCount = storedStack.getCount();

            if (amount >= storedCount) {
                // 取出全部
                stack.removeSubNbt(STORED_ITEM_KEY);
                return Optional.of(storedStack);
            } else {
                // 取出部分
                ItemStack removedStack = storedStack.copyWithCount(amount);
                storedStack.setCount(storedCount - amount);

                // 更新存储的NBT
                NbtCompound nbt = stack.getOrCreateNbt();
                NbtCompound storedNbt = new NbtCompound();
                storedStack.writeNbt(storedNbt);
                nbt.put(STORED_ITEM_KEY, storedNbt);

                return Optional.of(removedStack);
            }
        }
        return Optional.empty();
    }

    /**
     * 丢弃粉尘袋中的所有物品
     */
    private static boolean dropAllBundledItems(ItemStack stack, PlayerEntity player) {
        Optional<ItemStack> bundledStack = removeAllStack(stack);
        if (bundledStack.isPresent() && !bundledStack.get().isEmpty()) {
            if (player instanceof ServerPlayerEntity) {
                player.dropItem(bundledStack.get(), true);
            }
            return true;
        }
        return false;
    }

    /**
     * 处理从粉尘袋取出物品
     */
    private void handleRemoveFromBundle(ItemStack stack, Slot slot, PlayerEntity player) {
        playRemoveOneSound(player);
        removeSomeStack(stack, 1).ifPresent(removedStack -> {
            ItemStack remaining = slot.insertStack(removedStack);
            if (!remaining.isEmpty()) {
                // 如果有剩余，尝试放回粉尘袋
                addToBundle(stack, remaining);
            }
        });
    }

    /**
     * 处理向粉尘袋添加物品
     */
    private void handleAddToBundle(ItemStack stack, Slot slot, ItemStack slotStack, PlayerEntity player) {
        int availableSpace = MAX_STORAGE - getBundleOccupancy(stack);
        int maxToAdd = Math.min(slotStack.getCount(), availableSpace);

        if (maxToAdd > 0) {
            ItemStack toAdd = slot.takeStackRange(slotStack.getCount(), maxToAdd, player);
            int actuallyAdded = addToBundle(stack, toAdd);
            if (actuallyAdded > 0) {
                playInsertSound(player);
            } else {
                // 如果添加失败（例如类型不同），把物品放回原处
                slot.insertStack(toAdd);
            }
        }
    }

    /**
     * 处理取出物品到光标
     */
    private void handleRemoveToCursor(ItemStack stack, StackReference cursorStackReference, PlayerEntity player) {
        removeSomeStack(stack, 1).ifPresent(itemStack -> {
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

    private void appendCapacityTooltip(ItemStack stack, List<Text> tooltip) {
        int occupancy = getBundleOccupancy(stack);
        tooltip.add(Text.translatable("item.foodcraft.flour_sack.fullness", occupancy, MAX_STORAGE)
                .formatted(Formatting.GRAY));
    }

    private void appendContentTooltip(ItemStack stack, List<Text> tooltip) {
        getBundledStack(stack).ifPresent(content -> {
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