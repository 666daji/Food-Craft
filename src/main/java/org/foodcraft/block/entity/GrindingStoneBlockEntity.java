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
        return 64; // 增加到64以容纳更多物品
    }

    public Item getExpectedOutput(){
        GrindingRecipe recipe = this.matchGetter.getFirstMatch(this, this.world).orElse(null);
        if (recipe != null) {
            return recipe.getOutput(null).getItem();
        }
        return ItemStack.EMPTY.getItem();
    }

    /**
     * 获取当前输入物品对应的配方
     */
    @Nullable
    public GrindingRecipe getCurrentRecipe() {
        ItemStack inputStack = this.inventory.get(INPUT_SLOT_INDEX);
        if (inputStack.isEmpty()) {
            return null;
        }
        Inventory tempInventory = new SimpleInventory(inputStack);
        return this.matchGetter.getFirstMatch(tempInventory, this.world).orElse(null);
    }

    /**
     * 检查物品是否可以作为任何研磨配方的输入
     */
    private boolean isValidGrindingInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Inventory tempInventory = new SimpleInventory(stack);
        return this.matchGetter.getFirstMatch(tempInventory, this.world).isPresent();
    }

    /**
     * 检查当前输入物品是否可以研磨（数量足够）
     */
    public boolean canGrindCurrentInput() {
        GrindingRecipe recipe = getCurrentRecipe();
        if (recipe == null) {
            return false;
        }

        ItemStack inputStack = this.inventory.get(INPUT_SLOT_INDEX);
        return inputStack.getCount() >= recipe.getInputCount();
    }

    /**
     * 尝试将物品添加到输入槽
     */
    public AddInputResult addInput(ItemStack stack, PlayerEntity player) {
        if (!isValidGrindingInput(stack)) {
            return AddInputResult.INVALID;
        }

        ItemStack inputSlot = this.inventory.get(INPUT_SLOT_INDEX);

        // 如果输入槽不为空且物品不同，先返还原有物品
        if (!inputSlot.isEmpty() && !ItemStack.areItemsEqual(inputSlot, stack)) {
            returnItemToPlayer(inputSlot, player);
            this.setStack(INPUT_SLOT_INDEX, ItemStack.EMPTY);
            inputSlot = ItemStack.EMPTY;
        }

        // 获取配方信息
        Inventory tempInventory = new SimpleInventory(stack);
        GrindingRecipe recipe = this.matchGetter.getFirstMatch(tempInventory, this.world).orElse(null);
        if (recipe == null) {
            return AddInputResult.INVALID;
        }

        int requiredCount = recipe.getInputCount();
        int playerStackCount = stack.getCount();

        // 如果输入槽为空，尝试一次性添加所需数量的物品
        if (inputSlot.isEmpty()) {
            int amountToAdd = Math.min(requiredCount, playerStackCount);
            if (amountToAdd < requiredCount) {
                return AddInputResult.NOT_ENOUGH; // 玩家手中的物品数量不足
            }

            ItemStack newInput = stack.copy();
            newInput.setCount(amountToAdd);
            this.setStack(INPUT_SLOT_INDEX, newInput);

            // 消耗玩家物品
            if (!player.isCreative()) {
                stack.decrement(amountToAdd);
            }

            return AddInputResult.SUCCESS;
        }
        // 如果输入槽不为空且物品相同，尝试补齐到配方的整数倍
        else if (ItemStack.areItemsEqual(inputSlot, stack)) {
            int currentCount = inputSlot.getCount();
            int remainder = currentCount % requiredCount;
            int neededToComplete = (remainder == 0) ? 0 : (requiredCount - remainder);

            // 如果已经是整数倍，检查是否可以再添加一组
            if (neededToComplete == 0) {
                int maxAddable = this.getMaxCountPerStack() - currentCount;
                int amountToAdd = Math.min(requiredCount, Math.min(maxAddable, playerStackCount));

                if (amountToAdd > 0) {
                    inputSlot.increment(amountToAdd);
                    this.setStack(INPUT_SLOT_INDEX, inputSlot);

                    if (!player.isCreative()) {
                        stack.decrement(amountToAdd);
                    }
                    return AddInputResult.SUCCESS;
                } else {
                    return AddInputResult.FULL;
                }
            }
            // 尝试补齐到整数倍
            else {
                if (playerStackCount >= neededToComplete) {
                    inputSlot.increment(neededToComplete);
                    this.setStack(INPUT_SLOT_INDEX, inputSlot);

                    if (!player.isCreative()) {
                        stack.decrement(neededToComplete);
                    }
                    return AddInputResult.SUCCESS;
                } else {
                    return AddInputResult.NOT_ENOUGH;
                }
            }
        }

        return AddInputResult.INVALID;
    }

    /**
     * 将物品返还给玩家
     */
    private void returnItemToPlayer(ItemStack stack, PlayerEntity player) {
        if (stack.isEmpty()) return;

        if (!player.getInventory().insertStack(stack.copy())) {
            // 如果玩家背包已满，掉落物品
            ItemEntity itemEntity = new ItemEntity(world,
                    player.getX(), player.getY(), player.getZ(), stack.copy());
            world.spawnEntity(itemEntity);
        }
        this.setStack(INPUT_SLOT_INDEX, ItemStack.EMPTY);
    }

    /**
     * 清空输入槽并将物品返还给玩家
     */
    public void returnInputToPlayer(PlayerEntity player) {
        ItemStack inputStack = this.inventory.get(INPUT_SLOT_INDEX);
        if (!inputStack.isEmpty()) {
            returnItemToPlayer(inputStack, player);
        }
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
     */
    public int calculateRequiredEnergy() {
        int inputCount = this.inventory.get(INPUT_SLOT_INDEX).getCount();
        if (inputCount == 0) {
            return 0;
        }

        GrindingRecipe recipe = getCurrentRecipe();
        if (recipe == null) {
            return 0;
        }

        int grindingTimeForRecipe = recipe.getGrindingTime();
        int requiredCount = recipe.getInputCount();

        // 计算可以研磨的次数
        int grindTimes = inputCount / requiredCount;
        if (grindTimes == 0) {
            return 0;
        }

        // 当前正在研磨的物品还需要 (grindingTimeForRecipe - grindingTime) 能量
        // 剩余物品每个配方需要 grindingTimeForRecipe 能量
        int remainingEnergyForCurrent = Math.max(0, grindingTimeForRecipe - this.grindingTime);
        int energyForRemainingItems = (grindTimes - 1) * grindingTimeForRecipe;

        return remainingEnergyForCurrent + energyForRemainingItems;
    }

    public static void tick(World world, BlockPos pos, BlockState state, GrindingStoneBlockEntity blockEntity) {
        blockEntity.age++;
        if (blockEntity.age == Integer.MAX_VALUE) {
            blockEntity.age = 0;
        }

        // 如果当前有能量且可以研磨，则继续或开始研磨
        if (blockEntity.energy > 0 && blockEntity.canGrind()) {
            // 如果当前没有研磨进度，则初始化
            if (blockEntity.grindingTime == 0) {
                GrindingRecipe recipe = blockEntity.getCurrentRecipe();
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

        itemEntity.setVelocity(0, 0.3, 0);
        itemEntity.setPickupDelay(10);

        world.spawnEntity(itemEntity);
        this.setStack(OUTPUT_SLOT_INDEX, ItemStack.EMPTY);
        this.markDirty();
    }

    private boolean canGrind() {
        ItemStack inputStack = this.inventory.get(INPUT_SLOT_INDEX);
        if (inputStack.isEmpty()) {
            return false;
        }

        GrindingRecipe recipe = this.getCurrentRecipe();
        if (recipe == null) {
            return false;
        }

        // 检查输入物品数量是否足够
        if (inputStack.getCount() < recipe.getInputCount()) {
            return false;
        }

        ItemStack output = recipe.getOutput(null);
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
        GrindingRecipe recipe = this.getCurrentRecipe();
        if (recipe != null && this.canGrind()) {
            ItemStack input = this.inventory.get(INPUT_SLOT_INDEX);
            ItemStack output = recipe.craft(this, null);
            ItemStack outputSlot = this.inventory.get(OUTPUT_SLOT_INDEX);
            int requiredCount = recipe.getInputCount();

            if (outputSlot.isEmpty()) {
                this.inventory.set(OUTPUT_SLOT_INDEX, output);
            } else if (ItemStack.areItemsEqual(outputSlot, output)) {
                outputSlot.increment(output.getCount());
            }

            // 消耗配方所需的物品数量
            input.decrement(requiredCount);

            // 记录使用的配方
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
     */
    public boolean tryAddEnergy(int amount) {
        if (this.age - this.lastEnergyAddTime < MIN_ENERGY_ADD_INTERVAL) {
            return false;
        }

        int requiredEnergy = this.calculateRequiredEnergy();
        if (this.getEnergy() >= requiredEnergy) {
            return false;
        }

        int energyToAdd = Math.min(amount, requiredEnergy - this.getEnergy());
        this.addEnergy(energyToAdd);
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
     */
    public float getGrindingProgress() {
        if (grindingTimeTotal != 0) {
            return (float) grindingTime / grindingTimeTotal * 100.0f;
        }
        return 0.0f;
    }

    /**
     * 获取所有物品（用于破坏方块时掉落）
     */
    public DefaultedList<ItemStack> getItemsToDrop() {
        DefaultedList<ItemStack> drops = DefaultedList.of();
        for (ItemStack stack : this.inventory) {
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return drops;
    }

    public enum AddInputResult {
        SUCCESS,
        FULL,
        INVALID,
        NOT_ENOUGH
    }
}