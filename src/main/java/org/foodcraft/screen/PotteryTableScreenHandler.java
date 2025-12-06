package org.foodcraft.screen;

import com.google.common.collect.Lists;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.block.entity.PotteryTableBlockEntity;
import org.foodcraft.recipe.PotteryRecipe;
import org.foodcraft.registry.ModRecipeTypes;
import org.foodcraft.registry.ModScreenHandlerTypes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PotteryTableScreenHandler extends ScreenHandler {
    /** 输入槽位索引 */
    public static final int INPUT_SLOT_INDEX = 0;
    /** 输出槽位索引 */
    public static final int OUTPUT_SLOT_INDEX = 1;
    /** 开始制作按钮ID */
    public static final int START_CRAFTING_BUTTON_ID = 100;
    /** 玩家物品栏起始槽位 */
    private static final int INVENTORY_START = 2;
    /** 玩家物品栏结束槽位 */
    private static final int INVENTORY_END = 29;
    /** 快捷栏起始槽位 */
    private static final int HOTBAR_START = 29;
    /** 快捷栏结束槽位 */
    private static final int HOTBAR_END = 38;

    /** 选中的配方索引属性 */
    protected final Property selectedRecipe = Property.create();
    protected List<PotteryRecipe> availableRecipes = Lists.newArrayList();
    protected ItemStack inputStack = ItemStack.EMPTY;

    protected final Slot inputSlot;
    protected final Slot outputSlot;
    protected final ScreenHandlerContext context;
    protected final Inventory inventory;
    protected final World world;
    protected final BlockPos pos;
    protected final PlayerEntity player;

    /** 内容变化监听器 */
    Runnable contentsChangedListener = () -> {};

    /** 属性委托，用于同步制作进度等数据 */
    private final PropertyDelegate propertyDelegate;

    /**
     * 构造陶艺台屏幕处理器（客户端使用）。
     * @param syncId 同步ID
     * @param playerInventory 玩家物品栏
     */
    public PotteryTableScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(2), ScreenHandlerContext.EMPTY, new ArrayPropertyDelegate(PotteryTableBlockEntity.PROPERTY_COUNT));
    }

    /**
     * 构造陶艺台屏幕处理器（服务器使用）。
     *
     * @param syncId 同步ID
     * @param playerInventory 玩家物品栏
     * @param inventory 对应的陶艺台方块实体
     * @param context 屏幕处理器上下文
     * @param propertyDelegate 属性委托
     */
    public PotteryTableScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, ScreenHandlerContext context, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlerTypes.POTTERY_TABLE, syncId);
        this.propertyDelegate = propertyDelegate;
        this.context = context;
        this.inventory = inventory;
        this.player = playerInventory.player;

        // 从上下文获取世界和位置
        this.world = playerInventory.player.getWorld();
        this.pos = context.get((world, pos) -> pos).orElse(BlockPos.ORIGIN);

        // 添加输入槽位
        this.inputSlot = this.addSlot(new Slot(inventory, PotteryTableBlockEntity.INPUT_SLOT, 20, 33){
            @Override
            public void markDirty() {
                super.markDirty();
                PotteryTableScreenHandler.this.onContentChanged(this.inventory);
            }
        });

        // 添加输出槽位
        this.outputSlot = this.addSlot(new Slot(inventory, PotteryTableBlockEntity.OUTPUT_SLOT, 143, 33) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerEntity) {
                return hasStack();
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                stack.onCraft(player.getWorld(), player, stack.getCount());
                super.onTakeItem(player, stack);
            }
        });

        // 添加玩家物品栏槽位
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // 添加快捷栏槽位
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        // 添加属性
        this.addProperty(this.selectedRecipe);
        this.addProperties(propertyDelegate);

        // 初始更新配方列表
        this.updateRecipeList();
    }

    /**
     * 获取当前选中的配方索引。
     *
     * @return 选中配方的索引，如果没有选中则返回-1
     */
    public int getSelectedRecipe() {
        return this.selectedRecipe.get();
    }

    /**
     * 获取可用的陶艺配方列表。
     *
     * @return 可用配方列表
     */
    public List<PotteryRecipe> getAvailableRecipes() {
        return this.availableRecipes;
    }

    /**
     * 获取可用配方数量。
     *
     * @return 可用配方数量
     */
    public int getAvailableRecipeCount() {
        return this.availableRecipes.size();
    }

    /**
     * 检查是否可以制作物品。
     *
     * @return 如果可以制作返回true，否则返回false
     */
    public boolean canCraft() {
        return this.inputSlot.hasStack() && !this.availableRecipes.isEmpty();
    }

    /**
     * 检查是否可以开始制作。
     *
     * @return 如果可以开始制作返回true，否则返回false
     */
    public boolean canStartCrafting() {
        int selected = getSelectedRecipe();
        if (selected < 0 || selected >= availableRecipes.size() || isCrafting()) {
            return false;
        }

        PotteryRecipe recipe = availableRecipes.get(selected);

        // 检查输入物品是否足够
        if (!recipe.matches(this.inventory, world)) {
            return false;
        }

        // 检查输出槽是否有物品
        if (outputSlot.hasStack()) {
            return false;
        }

        // 检查输出槽是否可以接受输出
        ItemStack outputStack = outputSlot.getStack();
        ItemStack recipeOutput = recipe.getOutput(world.getRegistryManager());

        if (!outputStack.isEmpty()) {
            // 如果输出槽有物品，必须与配方输出相同且有空位
            if (!ItemStack.areEqual(outputStack, recipeOutput)) {
                return false;
            }

            int resultCount = outputStack.getCount() + recipeOutput.getCount();
            return resultCount <= outputStack.getMaxCount() && resultCount <= outputSlot.inventory.getMaxCountPerStack();
        }

        return true;
    }

    /**
     * 检查是否可以继续制作。
     *
     * @return 如果可以继续制作返回true，否则返回false
     */
    public boolean canContinueCrafting() {
        int selected = getSelectedRecipe();
        if (selected < 0 || selected >= availableRecipes.size()) {
            return false;
        }

        PotteryRecipe recipe = availableRecipes.get(selected);

        // 检查输入物品是否仍然足够
        if (!recipe.matches(this.inventory, world)) {
            return false;
        }

        // 检查输出槽是否可以接受输出（在制作过程中可能被玩家放入其他物品）
        ItemStack outputStack = outputSlot.getStack();
        ItemStack recipeOutput = recipe.getOutput(world.getRegistryManager());

        return outputStack.isEmpty() || ItemStack.areEqual(outputStack, recipeOutput);
    }

    /**
     * 检查是否正在制作。
     *
     * @return 如果正在制作返回true，否则返回false
     */
    public boolean isCrafting() {
        return propertyDelegate.get(PotteryTableBlockEntity.IS_CRAFTING_PROPERTY) == 1;
    }

    /**
     * 重置制作状态。
     */
    public void resetCrafting() {
        propertyDelegate.set(PotteryTableBlockEntity.IS_CRAFTING_PROPERTY, 0); // 设置 isCrafting 为 false
        propertyDelegate.set(PotteryTableBlockEntity.CRAFT_PROGRESS_PROPERTY, 0);  // 重置进度
    }

    /**
     * 检查玩家是否可以使用这个屏幕处理器。
     *
     * @param player 玩家实例
     * @return 如果玩家可以使用返回true，否则返回false
     */
    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    /**
     * 当玩家点击按钮时调用，处理配方选择或开始制作。
     *
     * @param player 点击按钮的玩家
     * @param id 按钮ID（配方索引或开始制作按钮）
     * @return 是否成功处理按钮点击
     */
    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == START_CRAFTING_BUTTON_ID) {
            // 确保只在服务器端执行制作逻辑
            if (canStartCrafting() && !player.getWorld().isClient) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof PotteryTableBlockEntity potteryTable) {
                    PotteryRecipe recipe = getSelectedRecipeInstance();
                    if (recipe != null) {
                        potteryTable.startCrafting(recipe);
                    }
                }
            }
            return true;
        } else if (this.isInBounds(id)) {
            this.selectedRecipe.set(id);
            return true;
        }
        return false;
    }

    /**
     * 检查配方索引是否在有效范围内。
     *
     * @param id 配方索引
     * @return 如果在有效范围内返回true，否则返回false
     */
    private boolean isInBounds(int id) {
        return id >= 0 && id < this.availableRecipes.size();
    }

    /**
     * 当物品栏内容发生变化时调用，更新可用配方列表。
     *
     * @param inventory 发生变化的物品栏
     */
    @Override
    public void onContentChanged(Inventory inventory) {
        super.onContentChanged(inventory);

        // 检查输入槽是否发生变化
        ItemStack currentInput = this.inputSlot.getStack();
        if (!ItemStack.areEqual(currentInput, this.inputStack)) {
            this.inputStack = currentInput.copy();
            this.updateRecipeList();
            this.contentsChangedListener.run();
        }
    }

    /**
     * 更新输入物品并重新计算可用配方。
     */
    private void updateRecipeList() {
        this.availableRecipes.clear();
        this.selectedRecipe.set(-1);
        if (!this.inputSlot.getStack().isEmpty()) {
            this.availableRecipes = this.world.getRecipeManager()
                    .getAllMatches(ModRecipeTypes.POTTERY, this.inventory, this.world);
        }
    }

    /**
     * 获取当前选中的配方。
     *
     * @return 当前选中的配方，如果没有选中则返回null
     */
    @Nullable
    public PotteryRecipe getSelectedRecipeInstance() {
        int selected = getSelectedRecipe();
        if (selected >= 0 && selected < availableRecipes.size()) {
            return availableRecipes.get(selected);
        }
        return null;
    }

    /**
     * 获取屏幕处理器类型。
     *
     * @return 屏幕处理器类型
     */
    @Override
    public ScreenHandlerType<?> getType() {
        return ModScreenHandlerTypes.POTTERY_TABLE;
    }

    /**
     * 设置内容变化监听器。
     *
     * @param contentsChangedListener 内容变化监听器
     */
    public void setContentsChangedListener(Runnable contentsChangedListener) {
        this.contentsChangedListener = contentsChangedListener;
    }

    /**
     * 检查是否可以将物品插入指定槽位。
     *
     * @param stack 要插入的物品堆栈
     * @param slot 目标槽位
     * @return 如果可以插入返回true，否则返回false
     */
    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return slot.inventory != this.inventory || slot.getIndex() != PotteryTableBlockEntity.OUTPUT_SLOT;
    }

    /**
     * 快速移动物品（Shift+点击）。
     *
     * @param player 执行操作的玩家
     * @param slot 源槽位索引
     * @return 移动后的物品堆栈
     */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot2 = this.slots.get(slot);
        if (slot2.hasStack()) {
            ItemStack itemStack2 = slot2.getStack();
            itemStack = itemStack2.copy();

            boolean wasInputSlotChanged = false;

            if (slot == OUTPUT_SLOT_INDEX) {
                // 只有在制作完成后才能取出输出物品
                if (!isCrafting() && slot2.canTakeItems(player)) {
                    if (!this.insertItem(itemStack2, INVENTORY_START, HOTBAR_END, true)) {
                        return ItemStack.EMPTY;
                    }
                    slot2.onQuickTransfer(itemStack2, itemStack);
                }
            } else if (slot == INPUT_SLOT_INDEX) {
                if (!this.insertItem(itemStack2, INVENTORY_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
                wasInputSlotChanged = true;
            } else if (this.world.getRecipeManager()
                    .getFirstMatch(ModRecipeTypes.POTTERY, new SimpleInventory(itemStack2), this.world)
                    .isPresent()) {
                if (!this.insertItem(itemStack2, INPUT_SLOT_INDEX, INPUT_SLOT_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
                wasInputSlotChanged = true;
            } else if (slot >= INVENTORY_START && slot < HOTBAR_START) {
                if (!this.insertItem(itemStack2, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot >= HOTBAR_START && slot < HOTBAR_END
                    && !this.insertItem(itemStack2, INVENTORY_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot2.setStack(ItemStack.EMPTY);
            }

            slot2.markDirty();
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot2.onTakeItem(player, itemStack2);
            this.sendContentUpdates();

            // 如果输入槽发生变化，更新配方列表
            if (wasInputSlotChanged) {
                this.updateRecipeList();
                this.contentsChangedListener.run();
            }
        }
        return itemStack;
    }

    /**
     * 当屏幕关闭时调用，通知方块实体玩家已关闭界面。
     *
     * @param player 关闭屏幕的玩家
     */
    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);

        // 只在服务器端通知方块实体
        if (!player.getWorld().isClient && inventory instanceof PotteryTableBlockEntity potteryTable) {
            potteryTable.unregisterPlayerClosing(player);
        }

//        this.context.run((world, pos) -> dropInventory(player, inventory));
    }

    /**
     * 在屏幕关闭时返还输入槽的物品
     */
    @Override
    protected void dropInventory(PlayerEntity player, Inventory inventory) {
        if (!player.isAlive() || player instanceof ServerPlayerEntity && ((ServerPlayerEntity)player).isDisconnected()) {
            player.dropItem(inventory.removeStack(0), false);
        } else {
            PlayerInventory playerInventory = player.getInventory();
            if (playerInventory.player instanceof ServerPlayerEntity) {
                playerInventory.offerOrDrop(inventory.removeStack(0));
            }
        }
    }

    /**
     * 获取制作进度。
     *
     * @return 当前制作进度
     */
    public int getCraftProgress() {
        return propertyDelegate.get(PotteryTableBlockEntity.CRAFT_PROGRESS_PROPERTY);
    }

    /**
     * 获取总制作时间。
     *
     * @return 总制作时间
     */
    public int getCraftTime() {
        return propertyDelegate.get(PotteryTableBlockEntity.CRAFT_TIME_PROPERTY);
    }

    /**
     * 获取制作进度比例（0.0-1.0）。
     *
     * @return 制作进度比例
     */
    public float getCraftProgressRatio() {
        int craftTime = this.getCraftTime();
        return craftTime > 0 ? (float) this.getCraftProgress() / craftTime : 0;
    }
}