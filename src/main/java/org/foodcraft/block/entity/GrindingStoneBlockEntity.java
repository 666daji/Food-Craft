package org.foodcraft.block.entity;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.foodcraft.recipe.GrindingRecipe;
import org.foodcraft.registry.ModRecipeTypes;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.util.ModAnimationState;
import org.jetbrains.annotations.Nullable;

public class GrindingStoneBlockEntity extends BlockEntity implements SidedInventory, RecipeUnlocker, RecipeInputProvider {
    protected static final int INPUT_SLOT_INDEX = 0;
    protected static final int OUTPUT_SLOT_INDEX = 1;
    public static final int DEFAULT_GRIND_TIME = 200;
    private static final int MIN_ENERGY_ADD_INTERVAL = 10;

    protected DefaultedList<ItemStack> inventory = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private int lastEnergyAddTime = 0;
    int energy;
    static final int MAX_ENERGY = 1000;
    int grindingTime;
    int grindingTimeTotal;

    public final ModAnimationState grindingAnimationState = new ModAnimationState();
    protected int age;

    private final Object2IntOpenHashMap<Identifier> recipesUsed = new Object2IntOpenHashMap<>();
    private final RecipeManager.MatchGetter<Inventory, ? extends GrindingRecipe> matchGetter;

    @Nullable
    private Recipe<?> lastRecipe;

    public GrindingStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.GRINDING_STONE, pos, state);
        this.matchGetter = RecipeManager.createCachedMatchGetter(ModRecipeTypes.GRINDING);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);
        this.energy = nbt.getInt("Energy");
        this.grindingTime = nbt.getInt("GrindingTime");
        this.grindingTimeTotal = nbt.getInt("GrindingTimeTotal");
        this.age = nbt.getInt("Age");
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, this.inventory);
        nbt.putInt("Energy", this.energy);
        nbt.putInt("GrindingTime", this.grindingTime);
        nbt.putInt("GrindingTimeTotal", this.grindingTimeTotal);
        nbt.putInt("Age", this.age);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        // 输入槽(0)可以从任何面插入，输出槽(1)可以从任何面提取
        return new int[]{INPUT_SLOT_INDEX, OUTPUT_SLOT_INDEX};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == INPUT_SLOT_INDEX;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == OUTPUT_SLOT_INDEX;
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT_INDEX && !ItemStack.areEqual(stack, this.inventory.get(slot))) {
            // 输入物品发生变化，重置研磨进度
            this.resetGrindingProgress();
        }
        this.inventory.set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
    }

    @Override
    public int getMaxCountPerStack() {
        return 16;
    }

    public Item getExpectedOutput(){
        GrindingRecipe recipe = this.matchGetter.getFirstMatch(this, this.world).orElse(null);
        if (recipe != null) {
            return recipe.getOutput(null).getItem();
        }
        return ItemStack.EMPTY.getItem();
    }

    /**
     * 尝试将物品添加到输入槽
     *
     * @param stack 要添加的物品堆
     * @return {@link addInputResult}添加物品的结果
     */
    public addInputResult addInput(ItemStack stack) {
        if (!isValidGrindingInput(stack)) {
            return addInputResult.INVALID; // 物品不符合任何配方，拒绝添加
        }

        ItemStack inputSlot = this.inventory.get(INPUT_SLOT_INDEX);
        if (inputSlot.isEmpty()) {
            this.setStack(INPUT_SLOT_INDEX, stack.copy());
        } else if (ItemStack.areItemsEqual(inputSlot, stack) && inputSlot.getCount() < 16) {
            int newCount = Math.min(inputSlot.getCount() + stack.getCount(), this.getMaxCountPerStack());
            inputSlot.setCount(newCount);
            this.setStack(INPUT_SLOT_INDEX, inputSlot);
        } else if (ItemStack.areItemsEqual(inputSlot, stack) && inputSlot.getCount() >= 16){
            return addInputResult.FULL;
        }
        this.markDirty();
        return addInputResult.SUCCESS;
    }

    /**
     * 检查物品是否可以作为任何研磨配方的输入
     *
     * @param stack 要检查的物品堆
     * @return 如果物品符合任何研磨配方，返回 true
     */
    private boolean isValidGrindingInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Inventory tempInventory = new SimpleInventory(stack);
        return this.matchGetter.getFirstMatch(tempInventory, this.world).isPresent();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    @Override
    public void provideRecipeInputs(RecipeMatcher finder) {
        for (ItemStack stack : this.inventory) {
            finder.addInput(stack);
        }
    }

    @Override
    public void setLastRecipe(@Nullable Recipe<?> recipe) {
        this.lastRecipe = recipe;
    }

    @Override
    public @Nullable Recipe<?> getLastRecipe() {
        return this.lastRecipe;
    }

    /**
     * 重置研磨进度
     */
    public void resetGrindingProgress() {
        this.grindingTime = 0;
        this.grindingTimeTotal = 0;
        this.markDirty();
    }

    /**
     * 计算完成剩余研磨所需的能量
     * 如果当前没有研磨进度，则尝试获取配方来计算所需能量
     * @return 完成剩余研磨所需的能量
     */
    public int calculateRequiredEnergy() {
        // 获取输入槽中的物品数量
        int inputCount = this.inventory.get(INPUT_SLOT_INDEX).getCount();
        if (inputCount == 0) {
            return 0;
        }

        // 如果当前没有研磨进度，尝试获取配方来计算所需能量
        int grindingTimeForRecipe = this.grindingTimeTotal;
        if (grindingTimeForRecipe == 0) {
            // 尝试获取当前配方的研磨时间
            GrindingRecipe recipe = this.matchGetter.getFirstMatch(this, this.world).orElse(null);
            if (recipe != null) {
                grindingTimeForRecipe = recipe.getGrindingTime();
            } else {
                return 0; // 没有匹配的配方，不需要能量
            }
        }

        // 计算完成所有物品所需的能量
        // 当前正在研磨的物品还需要 (grindingTimeForRecipe - grindingTime) 能量
        // 剩余物品每个需要 grindingTimeForRecipe 能量
        int remainingEnergyForCurrent = Math.max(0, grindingTimeForRecipe - this.grindingTime);
        int energyForRemainingItems = (inputCount - 1) * grindingTimeForRecipe;

        return remainingEnergyForCurrent + energyForRemainingItems;
    }

    public static void tick(World world, BlockPos pos, BlockState state, GrindingStoneBlockEntity blockEntity) {
        // 每tick增加方块寿命
        blockEntity.age++;
        if (blockEntity.age == Integer.MAX_VALUE) {
            blockEntity.age = 0;
        }

        // 如果当前有能量且可以研磨，则继续或开始研磨
        if (blockEntity.energy > 0 && blockEntity.canGrind()) {
            // 如果当前没有研磨进度，则初始化
            if (blockEntity.grindingTime == 0) {
                GrindingRecipe recipe = blockEntity.matchGetter.getFirstMatch(blockEntity, world).orElse(null);
                if (recipe != null) {
                    blockEntity.grindingTimeTotal = recipe.getGrindingTime();
                }
            }

            // 消耗能量并增加进度
            blockEntity.energy--;
            blockEntity.grindingTime++;

            // 检查是否研磨完成
            if (blockEntity.grindingTime >= blockEntity.grindingTimeTotal) {
                blockEntity.resetGrindingProgress();
                blockEntity.grindItem();
            }
        }

        // 尝试给予产物
        if (!world.isClient && blockEntity.age % 10 == 0) {
            ItemStack outputStack = blockEntity.getStack(OUTPUT_SLOT_INDEX);
            if (!outputStack.isEmpty()) {
                blockEntity.ejectOutputItem(world, pos);
            }
        }

        // 无论如何，每tick保存数据
        blockEntity.markDirty();
        blockEntity.sync();
    }

    /**
     * 将输出槽的物品向上喷出
     */
    private void ejectOutputItem(World world, BlockPos pos) {
        ItemStack outputStack = this.getStack(OUTPUT_SLOT_INDEX);
        if (outputStack.isEmpty()) {
            return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        ItemEntity itemEntity = new ItemEntity(world, x, y, z, outputStack.copy());

        // 固定向上速度
        double verticalSpeed = 0.3;
        itemEntity.setVelocity(
                0, verticalSpeed, 0
        );

        // 设置物品实体的拾取延迟，防止立即被重新收集
        itemEntity.setPickupDelay(10);

        world.spawnEntity(itemEntity);

        // 清空输出槽
        this.setStack(OUTPUT_SLOT_INDEX, ItemStack.EMPTY);
        this.markDirty();
    }

    private boolean canGrind() {
        if (this.inventory.get(INPUT_SLOT_INDEX).isEmpty()) {
            return false;
        }

        GrindingRecipe recipe = this.matchGetter.getFirstMatch(this, world).orElse(null);
        if (recipe == null) {
            return false;
        }

        ItemStack output = recipe.getOutput(null); // 使用 null 作为参数，因为没有 DynamicRegistryManager
        if (output.isEmpty()) {
            return false;
        }

        ItemStack outputSlot = this.inventory.get(OUTPUT_SLOT_INDEX);
        if (outputSlot.isEmpty()) {
            return true;
        }

        if (!ItemStack.areItemsEqual(outputSlot, output)) {
            return false;
        }

        int resultCount = outputSlot.getCount() + output.getCount();
        return resultCount <= getMaxCountPerStack() && resultCount <= outputSlot.getMaxCount();
    }

    private void grindItem() {
        GrindingRecipe recipe = this.matchGetter.getFirstMatch(this, world).orElse(null);
        if (recipe != null && this.canGrind()) {
            ItemStack input = this.inventory.get(INPUT_SLOT_INDEX);
            ItemStack output = recipe.getOutput(null); // 使用 null 作为参数
            ItemStack outputSlot = this.inventory.get(OUTPUT_SLOT_INDEX);

            if (outputSlot.isEmpty()) {
                this.inventory.set(OUTPUT_SLOT_INDEX, output.copy());
            } else if (ItemStack.areItemsEqual(outputSlot, output)) {
                outputSlot.increment(output.getCount());
            }

            input.decrement(1);

            // 记录使用的配方（用于奖励经验）
            this.setLastRecipe(recipe);
            this.recipesUsed.addTo(recipe.getId(), 1);
        }
    }

    public boolean isGrinding() {
        return this.grindingTime > 0 && this.energy > 0;
    }

    public int getGrindingTimeTotal() {
        return this.grindingTimeTotal;
    }

    public int getEnergy() {
        return energy;
    }

    public int getMaxEnergy() {
        return MAX_ENERGY;
    }

    public void setEnergy(int energy) {
        this.energy = Math.min(energy, MAX_ENERGY);
        markDirty();
    }

    /**
     * 尝试添加能量，考虑时间间隔限制
     * @param amount 要添加的能量值
     * @return 是否成功添加能量
     */
    public boolean tryAddEnergy(int amount) {
        // 检查时间间隔
        if (this.age - this.lastEnergyAddTime < MIN_ENERGY_ADD_INTERVAL) {
            return false;
        }

        // 计算完成剩余研磨所需的能量
        int requiredEnergy = this.calculateRequiredEnergy();

        // 如果当前能量已经足够完成剩余研磨，则不添加能量
        if (this.getEnergy() >= requiredEnergy) {
            return false;
        }

        // 计算需要添加的能量（最大amount，但不超过所需能量）
        int energyToAdd = Math.min(amount, requiredEnergy - this.getEnergy());
        this.addEnergy(energyToAdd);

        // 更新最后添加能量的时间
        this.lastEnergyAddTime = this.age;

        return true;
    }

    public void addEnergy(int energy) {
        this.energy = Math.min(this.energy + energy, MAX_ENERGY);
        markDirty();
    }

    public void consumeEnergy(int energy) {
        this.energy = Math.max(0, this.energy - energy);
        markDirty();
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

    public void sync() {
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }

    /**
     * 获取当前研磨进度的百分比
     * @return 研磨进度百分比（0.0 - 100.0）
     */
    public float getGrindingProgress() {
        if (grindingTimeTotal != 0) {
            return (float) grindingTime / grindingTimeTotal * 100.0f;
        }
        return 0.0f;
    }

    public enum addInputResult {
        SUCCESS,
        FULL,
        INVALID
    }
}