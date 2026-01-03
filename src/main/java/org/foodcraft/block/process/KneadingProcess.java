package org.foodcraft.block.process;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.foodcraft.block.process.step.Step;
import org.foodcraft.block.process.step.StepExecutionContext;
import org.foodcraft.block.process.step.StepResult;
import org.foodcraft.item.FlourItem;
import org.foodcraft.recipe.DoughRecipe;
import org.foodcraft.registry.ModItems;
import org.foodcraft.registry.ModRecipeTypes;
import org.foodcraft.util.FoodCraftUtils;

import java.util.*;
import java.util.function.Predicate;

/**
 * 揉面流程实现类，管理所有状态数据。
 */
public class KneadingProcess<T extends BlockEntity & Inventory> extends AbstractProcess<T> implements Inventory {
    /** 可加入盆中的额外物品集合 */
    public static final Set<Item> CAN_ADD_OTHER = Set.of(ModItems.SALT_CUBES, ModItems.SALT_FLOUR, Items.SUGAR, ModItems.SUGAR_FLOUR, Items.EGG);
    public static final Set<Item> CAN_ADD_FLOUR = Set.of(ModItems.WHEAT_FLOUR, ModItems.COCOA_FLOUR);

    /** 流程步骤ID常量 */
    public static final String STEP_ADD_FLOUR = "add_flour";
    public static final String STEP_ADD_LIQUID = "add_liquid";
    public static final String STEP_ADD_EXTRA = "add_extra";
    public static final String STEP_KNEAD = "knead";

    /** 额外物品库存槽位数量 */
    private static final int EXTRA_SLOT_COUNT = 10;
    private static final int TOTAL_SLOTS = EXTRA_SLOT_COUNT;

    /** 额外物品库存 */
    private final DefaultedList<ItemStack> extraInventory;

    /** 面粉类型计数 */
    private final Map<FlourItem.FlourType, Integer> flourCounts;

    /** 液体类型计数 */
    private final Map<LiquidType, Integer> liquidCounts;

    /** 揉面次数 */
    private int kneadingCount;

    /** 是否已经处理了跳过逻辑 */
    private boolean processedSkip;

    public KneadingProcess() {
        this.extraInventory = DefaultedList.ofSize(TOTAL_SLOTS, ItemStack.EMPTY);
        this.flourCounts = new HashMap<>();
        this.liquidCounts = new HashMap<>();
        this.kneadingCount = 0;
        this.processedSkip = false;

        // 注册步骤
        registerSteps();

        // 设置初始步骤（仅在全新流程时）
        if (currentStepId == null) {
            setInitialStep(STEP_ADD_FLOUR);
        }
    }

    private void registerSteps() {
        // 1. 加粉步骤 - 需要加入3次面粉
        registerStep(STEP_ADD_FLOUR, new FlourStep());

        // 2. 加水步骤 - 需要加入3次液体
        registerStep(STEP_ADD_LIQUID, new LiquidStep());

        // 3. 加额外物品步骤 - 可跳过，最多加10次
        registerStep(STEP_ADD_EXTRA, new ExtraItemStep());

        // 4. 揉面步骤 - 需要揉面3次
        registerStep(STEP_KNEAD, new KneadingStep());
    }

    // ============ beforeGetStep实现 ============

    @Override
    protected void beforeGetStep(StepExecutionContext<T> context) {
        // 重置跳过标记
        processedSkip = false;

        // 只在添加额外物品步骤时检查
        if (STEP_ADD_EXTRA.equals(currentStepId)) {
            ItemStack heldStack = context.getHeldItemStack();

            // 如果玩家空手，跳转到揉面步骤
            if (heldStack.isEmpty()) {
                jumpToStep(STEP_KNEAD);
            }
        }
    }

    // ============ 步骤实现类 ============

    private class FlourStep implements Step<T> {
        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            ItemStack heldStack = context.getHeldItemStack();

            // 检查是否为面粉
            if (!(heldStack.getItem() instanceof FlourItem flourItem)) {
                return StepResult.fail(STEP_ADD_FLOUR, ActionResult.PASS);
            }

            // 播放刷子清扫可疑沙砾的声音
            context.playSound(SoundEvents.ITEM_BRUSH_BRUSHING_SAND_COMPLETE);

            // 服务器端执行
            if (context.isServerSide()) {
                // 消耗一个面粉
                if (!context.isCreateMode()){
                    heldStack.decrement(1);
                }

                // 记录面粉计数
                flourCounts.put(flourItem.getFlourType(),
                        flourCounts.getOrDefault(flourItem.getFlourType(), 0) + 1);

                // 更新方块实体
                context.blockEntity.markDirty();
            }

            // 检查是否加满3个面粉
            if (getTotalFlourCount() >= 3) {
                return StepResult.nextStep(STEP_ADD_LIQUID, ActionResult.SUCCESS);
            } else {
                return StepResult.continueSameStep(ActionResult.SUCCESS);
            }
        }
    }

    private class LiquidStep implements Step<T> {
        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            ItemStack heldStack = context.getHeldItemStack();

            // 检查手持物品是否为可接受的液体
            Optional<LiquidType> liquidType = LiquidType.fromItem(heldStack);
            if (liquidType.isEmpty()) {
                // 不是可接受的液体，步骤失败，回退到当前步骤
                return StepResult.fail(STEP_ADD_LIQUID, ActionResult.PASS);
            }

            // 服务器端执行消耗和记录逻辑
            if (context.isServerSide()) {
                LiquidType type = liquidType.get();

                // 播放水瓶的声音
                if (type == LiquidType.WATER) {
                    context.playSound(SoundEvents.ITEM_BOTTLE_EMPTY);
                } else if (type == LiquidType.MILK) {
                    context.playSound(SoundEvents.ENTITY_COW_MILK);
                }

                // 消耗1个液体物品
                if (!context.isCreateMode()) {
                    heldStack.decrement(1);
                }

                // 返还容器
                Item containerItem = type.getContainerItem();
                if (containerItem != null && !containerItem.equals(Items.AIR)) {
                    context.giveStack(new ItemStack(containerItem));
                }

                // 记录液体计数（使用LiquidType作为键）
                liquidCounts.put(type, liquidCounts.getOrDefault(type, 0) + 1);

                // 更新方块实体
                context.blockEntity.markDirty();
            }

            // 检查是否已加满3次液体
            if (isLiquidComplete()) {
                return StepResult.nextStep(STEP_ADD_EXTRA, ActionResult.SUCCESS);
            } else {
                return StepResult.continueSameStep(ActionResult.SUCCESS);
            }
        }

        /** 检查是否已加满3次液体 */
        private boolean isLiquidComplete() {
            return getTotalLiquidCount() >= 3;
        }
    }

    private class ExtraItemStep implements Step<T> {
        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            ItemStack heldStack = context.getHeldItemStack();

            // 检查手持物品是否为可接受的额外物品
            if (!CAN_ADD_OTHER.contains(heldStack.getItem())) {
                return StepResult.fail(STEP_ADD_EXTRA, ActionResult.PASS);
            }

            // 检查是否已满
            if (isExtraFull()) {
                return StepResult.nextStep(STEP_KNEAD, ActionResult.SUCCESS);
            }

            // 服务器端执行消耗和存储逻辑
            if (context.isServerSide()) {
                // 播放普通的石头放置音效
                context.playSound(SoundEvents.BLOCK_STONE_PLACE);

                // 先创建物品的副本，用于存储
                ItemStack extraCopy = new ItemStack(heldStack.getItem(), 1);

                // 如果需要保留NBT数据，可以这样复制
                if (heldStack.getNbt() != null && heldStack.hasNbt()) {
                    extraCopy.setNbt(heldStack.getNbt().copy());
                }

                // 消耗1个额外物品
                if (!context.isCreateMode()) {
                    heldStack.decrement(1);
                }

                // 存储额外物品到库存
                for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
                    if (extraInventory.get(i).isEmpty()) {
                        extraInventory.set(i, extraCopy);

                        // 标记库存已修改
                        break;
                    }
                }

                // 更新方块实体
                context.blockEntity.markDirty();

                // 如果加满了，跳转到揉面步骤
                if (isExtraFull()) {
                    return StepResult.nextStep(STEP_KNEAD, ActionResult.SUCCESS);
                }
            }

            // 继续执行当前步骤（添加更多额外物品）
            return StepResult.continueSameStep(ActionResult.SUCCESS);
        }

        /** 检查是否已加满10个额外物品 */
        private boolean isExtraFull() {
            int extraCount = 0;
            for (int i = 0; i < EXTRA_SLOT_COUNT; i++) {
                if (!extraInventory.get(i).isEmpty()) {
                    extraCount++;
                }
            }
            return extraCount >= EXTRA_SLOT_COUNT;
        }
    }

    private class KneadingStep implements Step<T> {
        @Override
        public StepResult execute(StepExecutionContext<T> context) {
            ItemStack heldStack = context.getHeldItemStack();

            // 检查是否空手
            if (!heldStack.isEmpty()) {
                return StepResult.fail(STEP_KNEAD, ActionResult.PASS);
            }

            context.playSound(SoundEvents.ENTITY_SLIME_DEATH_SMALL);

            // 服务器端执行揉面逻辑
            if (context.isServerSide()) {
                kneadingCount++;

                // 如果是第二次揉面，制作面团
                if (kneadingCount >= 2) {
                    ItemStack result = craftDough(context.world);
                    if (!result.isEmpty()) {

                        // 将面团放入盆方块实体
                        context.blockEntity.setStack(0, result);

                        // 重置流程
                        reset();

                        // 更新方块实体
                        context.blockEntity.markDirty();
                        return StepResult.complete(ActionResult.SUCCESS);
                    } else {
                        // 没有匹配的配方，失败
                        reset();
                        context.blockEntity.markDirty();
                        return StepResult.fail(null, ActionResult.FAIL);
                    }
                } else {
                    // 第一次揉面，继续执行当前步骤
                    context.blockEntity.markDirty();
                    return StepResult.continueSameStep(ActionResult.SUCCESS);
                }
            }

            return StepResult.continueSameStep(ActionResult.SUCCESS);
        }
    }

    // ============ 面团制作 ============

    private ItemStack craftDough(World world) {
        // 查找匹配的配方
        Optional<DoughRecipe> recipe = world.getRecipeManager()
                .getFirstMatch(ModRecipeTypes.DOUGH_MAKING, this, world);

        if (recipe.isPresent()) {
            // 消耗所有原料
            clear();
            flourCounts.clear();
            liquidCounts.clear();
            kneadingCount = 0;
            processedSkip = false;

            return recipe.get().getOutput(world.getRegistryManager()).copy();
        }

        return ItemStack.EMPTY;
    }

    // ============ 状态获取方法 ============

    public Map<FlourItem.FlourType, Integer> getFlourCounts() {
        return new HashMap<>(flourCounts);
    }

    /**
     * 获取液体类型计数
     */
    public Map<LiquidType, Integer> getLiquidCounts() {
        return new HashMap<>(liquidCounts);
    }

    /**
     * 获取用于配方匹配的液体计数（字符串形式）
     */
    public Map<String, Integer> getLiquidCountsForRecipe() {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<LiquidType, Integer> entry : liquidCounts.entrySet()) {
            result.put(entry.getKey().asString(), entry.getValue());
        }
        return result;
    }

    public List<ItemStack> getExtraItemStacks() {
        List<ItemStack> extras = new ArrayList<>();
        for (ItemStack stack : extraInventory) {
            if (!stack.isEmpty()) {
                extras.add(stack);
            }
        }
        return extras;
    }

    public int getKneadingCount() {
        return kneadingCount;
    }

    public int getExtraItemCount() {
        return (int) extraInventory.stream().filter(stack -> !stack.isEmpty()).count();
    }

    public int getTotalFlourCount() {
        return flourCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalLiquidCount() {
        return liquidCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static boolean isCanAddFlour(ItemStack stack) {
        return CAN_ADD_FLOUR.contains(stack.getItem());
    }

    public static boolean isCanAddExtra(ItemStack stack) {
        return CAN_ADD_OTHER.contains(stack.getItem());
    }

    // ============ AbstractProcess方法实现 ============

    @Override
    protected String getInitialStepId() {
        return STEP_ADD_FLOUR;
    }

    @Override
    protected void onStart(World world, T blockEntit) {
        clear();
        flourCounts.clear();
        liquidCounts.clear();
        kneadingCount = 0;
        processedSkip = false;
    }

    @Override
    protected void onReset() {
        clear();
        flourCounts.clear();
        liquidCounts.clear();
        kneadingCount = 0;
        processedSkip = false;
    }

    // ============ NBT持久化 ============

    @Override
    public void writeToNbt(NbtCompound nbt) {
        super.writeToNbt(nbt);

        // 保存额外物品库存
        Inventories.writeNbt(nbt, extraInventory);

        // 保存面粉计数
        NbtCompound floursNbt = new NbtCompound();
        for (Map.Entry<FlourItem.FlourType, Integer> entry : flourCounts.entrySet()) {
            floursNbt.putInt(entry.getKey().asString(), entry.getValue());
        }
        nbt.put("flours", floursNbt);

        // 保存液体计数（LiquidType作为键）
        NbtCompound liquidsNbt = new NbtCompound();
        for (Map.Entry<LiquidType, Integer> entry : liquidCounts.entrySet()) {
            liquidsNbt.putInt(entry.getKey().asString(), entry.getValue());
        }
        nbt.put("liquids", liquidsNbt);

        // 保存揉面次数
        nbt.putInt("kneading_count", kneadingCount);

        // 保存跳过标记
        nbt.putBoolean("processed_skip", processedSkip);
    }

    @Override
    public void readFromNbt(NbtCompound nbt) {
        super.readFromNbt(nbt);

        // 清空现有数据
        extraInventory.clear();
        flourCounts.clear();
        liquidCounts.clear();

        // 读取额外物品库存
        Inventories.readNbt(nbt, extraInventory);

        // 读取面粉计数
        if (nbt.contains("flours")) {
            NbtCompound floursNbt = nbt.getCompound("flours");
            for (String key : floursNbt.getKeys()) {
                flourCounts.put(FlourItem.FlourType.fromId(key), floursNbt.getInt(key));
            }
        }

        // 读取液体计数（从字符串转换为LiquidType）
        if (nbt.contains("liquids")) {
            NbtCompound liquidsNbt = nbt.getCompound("liquids");
            for (String key : liquidsNbt.getKeys()) {
                LiquidType.fromString(key).ifPresent(liquidType ->
                        liquidCounts.put(liquidType, liquidsNbt.getInt(key))
                );
            }
        }

        // 读取揉面次数
        kneadingCount = nbt.getInt("kneading_count");

        // 读取跳过标记
        processedSkip = nbt.getBoolean("processed_skip");
    }

    // ============ Inventory接口实现 ============

    @Override
    public int size() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : extraInventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= extraInventory.size()) {
            return ItemStack.EMPTY;
        }
        return extraInventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(extraInventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(extraInventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < extraInventory.size()) {
            extraInventory.set(slot, stack);
        }
    }

    @Override
    public void markDirty() {

    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        extraInventory.clear();
    }

    // ============ 液体类型枚举 ============

    public enum LiquidType implements StringIdentifiable {
        WATER("water", FoodCraftUtils::isWaterPotion, Items.GLASS_BOTTLE),
        MILK("milk", stack -> stack.isOf(ModItems.MILK_POTION), Items.GLASS_BOTTLE);

        private final String id;
        private final Predicate<ItemStack> itemChecker;
        private final Item containerItem;

        LiquidType(String id, Predicate<ItemStack> itemChecker, Item containerItem) {
            this.id = id;
            this.itemChecker = itemChecker;
            this.containerItem = containerItem;
        }

        @Override
        public String asString() {
            return id;
        }

        public boolean matches(ItemStack stack) {
            return itemChecker.test(stack);
        }

        public Item getContainerItem() {
            return containerItem;
        }

        public static Optional<LiquidType> fromItem(ItemStack stack) {
            for (LiquidType type : values()) {
                if (type.matches(stack)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }

        /**
         * 根据字符串ID获取液体类型
         */
        public static Optional<LiquidType> fromString(String id) {
            for (LiquidType type : values()) {
                if (type.id.equals(id)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * 获取当前流程状态的总和
     * @return 状态对象，包含当前步骤、上一个步骤和各步骤计数信息
     */
    public KneadingState getRenderState() {
        return new KneadingState(
                currentStepId,
                previousStepId,
                getTotalFlourCount(),
                getTotalLiquidCount(),
                getExtraItemCount(),
                getKneadingCount(),
                isActive
        );
    }

    /**
     * 状态数据类
     */
    public record KneadingState(String currentStepId, String previousStepId, int flourCount, int liquidCount,
                                int extraItemCount, int kneadingCount, boolean isActive) {}
}