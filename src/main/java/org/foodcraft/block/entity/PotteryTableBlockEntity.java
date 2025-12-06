package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.screen.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.PotteryTableBlock;
import org.foodcraft.recipe.PotteryRecipe;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.registry.ModRecipeTypes;
import org.foodcraft.screen.PotteryTableScreenHandler;
import org.foodcraft.util.ModAnimationState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class PotteryTableBlockEntity extends BlockEntity implements SidedInventory, RecipeUnlocker, RecipeInputProvider, NamedScreenHandlerFactory {
    /** 顶部可访问的槽位（输入槽） */
    private static final int[] TOP_SLOTS = new int[]{0};
    /** 侧面可访问的槽位（输入槽） */
    private static final int[] SIDE_SLOTS = new int[]{0};
    /** 底部可访问的槽位（输出槽） */
    private static final int[] BOTTOM_SLOTS = new int[]{1};

    /** 输入槽位索引 */
    public static final int INPUT_SLOT = 0;
    /** 输出槽位索引 */
    public static final int OUTPUT_SLOT = 1;
    /** 属性委托中属性的总数 */
    public static final int PROPERTY_COUNT = 4;
    /** 制作进度属性索引 */
    public static final int CRAFT_PROGRESS_PROPERTY = 0;
    /** 制作总时间属性索引 */
    public static final int CRAFT_TIME_PROPERTY = 1;
    /** 是否有输入物品属性 */
    public static final int HAS_INPUT_PROPERTY = 2;
    /** 是否正在制作属性 */
    public static final int IS_CRAFTING_PROPERTY = 3;

    private static final Logger LOGGER = FoodCraft.LOGGER;

    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private int craftProgress;
    private int craftTime;
    private boolean isCrafting = false;
    private PotteryRecipe currentRecipe;

    public final ModAnimationState workSurfaceAnimationState = new ModAnimationState();
    public final ModAnimationState clayBallAnimationState = new ModAnimationState();
    protected int age;

    /**
     * 属性委托，用于在服务器和客户端之间同步方块实体状态。
     *
     * <p>同步的数据包括：
     * <ul>
     *   <li>制作进度</li>
     *   <li>制作总时间</li>
     *   <li>是否有输入物品</li>
     *   <li>是否正在制作</li>
     * </ul>
     */
    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case CRAFT_PROGRESS_PROPERTY -> PotteryTableBlockEntity.this.craftProgress;
                case CRAFT_TIME_PROPERTY -> PotteryTableBlockEntity.this.craftTime;
                case HAS_INPUT_PROPERTY -> PotteryTableBlockEntity.this.hasInput() ? 1 : 0;
                case IS_CRAFTING_PROPERTY -> PotteryTableBlockEntity.this.isCrafting ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case CRAFT_PROGRESS_PROPERTY -> PotteryTableBlockEntity.this.craftProgress = value;
                case CRAFT_TIME_PROPERTY -> PotteryTableBlockEntity.this.craftTime = value;
                case IS_CRAFTING_PROPERTY -> PotteryTableBlockEntity.this.isCrafting = value == 1;
            }
        }

        @Override
        public int size() {
            return PROPERTY_COUNT;
        }
    };

    public PotteryTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.POTTERY_TABLE, pos, state);
    }

    /**
     * 每tick更新的逻辑，处理陶艺制作进度。
     *
     * @param world 世界实例
     * @param pos 方块位置
     * @param state 方块状态
     * @param blockEntity 陶艺台方块实体实例
     */
    public static void tick(World world, BlockPos pos, BlockState state, PotteryTableBlockEntity blockEntity) {
        blockEntity.age++;
        if (blockEntity.age == Integer.MAX_VALUE) {
            blockEntity.age = 0;
        }

        if (world.isClient) return;

        boolean dirty = false;

        // 检查是否可以继续制作
        if (blockEntity.isCrafting && !blockEntity.canContinueCrafting()) {
            LOGGER.debug("Crafting interrupted at pottery table at {}", pos);
            blockEntity.resetCrafting();
            dirty = true;
        }

        // 更新制作进度
        if (blockEntity.isCrafting && blockEntity.canContinueCrafting()) {
            blockEntity.craftProgress++;
            if (blockEntity.craftProgress >= blockEntity.craftTime) {
                LOGGER.debug("Crafting completed at pottery table at {}", pos);
                blockEntity.finishCrafting();
                dirty = true;
            }
        }

        if (dirty) {
            blockEntity.markDirty();
        }
    }

    /**
     * 获取当前可用的配方列表。
     *
     * @return 可用的配方列表
     */
    public List<PotteryRecipe> getAvailableRecipes() {
        if (world == null) {
            return List.of();
        }
        return world.getRecipeManager()
                .getAllMatches(ModRecipeTypes.POTTERY, this, world);
    }

    /**
     * 检查指定配方是否在当前可用的配方列表中。
     *
     * @param recipe 要检查的配方
     * @return 如果配方可用返回true，否则返回false
     */
    public boolean isRecipeAvailable(PotteryRecipe recipe) {
        return getAvailableRecipes().contains(recipe);
    }

    /**
     * 开始制作指定的配方。
     *
     * @param recipe 要制作的配方
     */
    public void startCrafting(PotteryRecipe recipe) {
        if (recipe == null) {
            LOGGER.warn("Cannot start crafting - recipe is null at {}", pos);
            return;
        }

        // 检查配方是否在当前可用的配方列表中
        if (!isRecipeAvailable(recipe)) {
            LOGGER.warn("Cannot start crafting - recipe {} is not available for current input at {}", recipe.getId(), pos);
            return;
        }

        this.currentRecipe = recipe;
        this.craftTime = recipe.getCraftTime();

        if (!canStartCrafting()) {
            LOGGER.warn("Cannot start crafting - conditions not met for recipe {} at {}", recipe.getId(), pos);
            this.currentRecipe = null;
            this.craftTime = 0;
            return;
        }

        this.isCrafting = true;
        this.craftProgress = 0;
        this.markDirty();

        LOGGER.debug("Started crafting recipe {} at pottery table at {}", recipe.getId(), pos);

        if (world != null) {
            world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.5F, 1.0F);
        }
    }

    /**
     * 完成制作，生成输出物品。
     */
    private void finishCrafting() {
        if (currentRecipe == null || !canContinueCrafting()) {
            LOGGER.warn("Attempted to finish crafting but conditions not met at {}", pos);
            resetCrafting();
            return;
        }

        ItemStack outputStack = getStack(OUTPUT_SLOT);
        ItemStack result = currentRecipe.craft(this, null);

        // 消耗输入物品
        removeStack(INPUT_SLOT, currentRecipe.getInputCount());

        if (outputStack.isEmpty()) {
            setStack(OUTPUT_SLOT, result);
        }

        if (getStack(INPUT_SLOT).isEmpty()){
            setStack(INPUT_SLOT, ItemStack.EMPTY);
        }

        // 重置制作状态
        resetCrafting();
    }

    /**
     * 重置制作状态。
     */
    public void resetCrafting() {
        this.isCrafting = false;
        this.craftProgress = 0;
        this.craftTime = 0;
        this.currentRecipe = null;
        this.markDirty();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }

    /**
     * 检查是否可以开始制作。
     *
     * @return 如果可以开始制作返回true，否则返回false
     */
    public boolean canStartCrafting() {
        if (currentRecipe == null) {
            LOGGER.debug("Cannot start crafting: currentRecipe is null");
            return false;
        }

        // 检查输入物品是否足够
        boolean matches = currentRecipe.matches(this, world);
        if (!matches) {
            LOGGER.debug("Cannot start crafting: Recipe {} doesn't match input", currentRecipe.getId());
            return false;
        }

        // 检查输出槽是否可以接受输出
        ItemStack outputStack = inventory.get(OUTPUT_SLOT);

        if (!outputStack.isEmpty()) {
            return false;
        }

        return !isCrafting;
    }

    /**
     * 检查是否可以继续制作。
     *
     * @return 如果可以继续制作返回true，否则返回false
     */
    public boolean canContinueCrafting() {
        if (currentRecipe == null) {
            return false;
        }

        // 检查输入物品是否仍然足够
        if (!currentRecipe.matches(this, world)) {
            return false;
        }

        // 检查输出槽是否可以接受输出（在制作过程中可能被玩家放入其他物品）
        ItemStack outputStack = inventory.get(OUTPUT_SLOT);

        return outputStack.isEmpty();
    }

    /**
     * 检查输入槽是否有物品。
     *
     * @return 如果输入槽有物品返回true，否则返回false
     */
    public boolean hasInput() {
        return !inventory.get(INPUT_SLOT).isEmpty();
    }

    /**
     * 检查是否正在制作物品。
     *
     * @return 如果正在制作返回true，否则返回false
     */
    public boolean isCrafting() {
        return isCrafting;
    }

    /**
     * 获取当前制作进度（0.0到1.0之间的值）。
     *
     * @return 制作进度，范围0.0到1.0
     */
    public float getCraftProgress() {
        return craftTime > 0 ? (float) craftProgress / craftTime : 0;
    }

    /**
     * 获取属性委托实例，用于屏幕处理器同步数据。
     *
     * @return 属性委托实例
     */
    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        inventory = DefaultedList.ofSize(size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, inventory);
        craftProgress = nbt.getInt("CraftProgress");
        craftTime = nbt.getInt("CraftTime");
        isCrafting = nbt.getBoolean("IsCrafting");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putInt("CraftProgress", craftProgress);
        nbt.putInt("CraftTime", craftTime);
        nbt.putBoolean("IsCrafting", isCrafting);
    }

    // NameScreenHandlerFactory implementation
    /**
     * 创建屏幕处理器。
     *
     * @param syncId 同步ID
     * @param inventory 玩家物品栏
     * @param player 玩家实例
     * @return 创建的屏幕处理器
     */
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new PotteryTableScreenHandler(syncId, inventory, this, ScreenHandlerContext.create(world, pos), getPropertyDelegate());
    }

    /**
     * 获取显示名称。
     *
     * @return 方块的显示名称
     */
    @Override
    public Text getDisplayName() {
        return PotteryTableBlock.TITLE;
    }

    // Inventory implementation
    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= inventory.size()) {
            return ItemStack.EMPTY;
        }
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(inventory, slot, amount);
        if (!result.isEmpty()) {
            markDirty();
            // 如果取出的是输入槽物品，检查是否需要取消制作
            if (slot == INPUT_SLOT && isCrafting && !canContinueCrafting()) {
                resetCrafting();
            }
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(this.inventory, slot);
        if (!result.isEmpty()) {
            markDirty();
            // 如果取出的是输入槽物品，检查是否需要取消制作
            if (slot == INPUT_SLOT && isCrafting && !canContinueCrafting()) {
                resetCrafting();
            }
        }
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= inventory.size()) {
            return;
        }

        ItemStack oldStack = inventory.get(slot);
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }

        // 如果设置的是输入槽，更新配方列表
        if (slot == INPUT_SLOT) {
            if (!ItemStack.areEqual(oldStack, stack)) {
                // 输入物品变化，如果正在制作且当前配方不再匹配，则重置制作
                if (isCrafting && !canContinueCrafting()) {
                    resetCrafting();
                }
            }
        }

        // 如果设置的是输出槽，检查是否与当前配方冲突
        if (slot == OUTPUT_SLOT && isCrafting && !canContinueCrafting()) {
            resetCrafting();
        }

        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        inventory.clear();
        resetCrafting();
        markDirty();
    }

    // SidedInventory implementation
    @Override
    public int[] getAvailableSlots(Direction side) {
        return switch (side) {
            case UP -> TOP_SLOTS;
            case DOWN -> BOTTOM_SLOTS;
            default -> SIDE_SLOTS;
        };
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT;
    }

    // RecipeUnlocker implementation
    @Override
    public void setLastRecipe(@Nullable Recipe<?> recipe) {
    }

    @Nullable
    @Override
    public Recipe<?> getLastRecipe() {
        return currentRecipe;
    }

    // RecipeInputProvider implementation
    @Override
    public void provideRecipeInputs(RecipeMatcher finder) {
        for (ItemStack stack : inventory) {
            finder.addInput(stack);
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    public int getAge() {
        return this.age;
    }
}