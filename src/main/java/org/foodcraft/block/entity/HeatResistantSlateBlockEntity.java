package org.foodcraft.block.entity;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.dfood.block.FoodBlock;
import org.dfood.item.DoubleBlockItem;
import org.dfood.shape.FoodShapeHandle;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.CombustionFirewoodBlock;
import org.foodcraft.block.FirewoodBlock;
import org.foodcraft.block.HeatResistantSlateBlock;
import org.foodcraft.block.multi.*;
import org.foodcraft.recipe.StoveRecipe;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.registry.ModRecipeTypes;
import org.foodcraft.util.FoodCraftUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public class HeatResistantSlateBlockEntity extends UpPlaceBlockEntity implements SidedInventory, RecipeUnlocker, RecipeInputProvider {
    protected static final int MIN_CHECK_INTERVAL = 10;
    protected static final String MULTIBLOCK_REF_KEY = "MultiBlockRef";
    protected static final double INPUT_OFFSET_Y = 0.1;
    protected static final int MIN_BAKING_TIME = 100; // 最小烘烤时间
    protected static final Logger LOGGER = FoodCraft.LOGGER;

    protected MultiBlockReference multiBlockRef;
    @Nullable
    protected BlockPattern.Result currentStoveResult;
    @Nullable
    protected Direction resultDirection;
    protected int stoveStructureType = -1;
    protected boolean isValidStove = false;
    protected NbtCompound refNbt;

    protected Set<BlockPos> firewoodPos = new HashSet<>(); // 绑定的柴火堆位置集合
    protected Set<CombustionFirewoodBlockEntity> firewoodEntities = new HashSet<>(); // 缓存的柴火堆方块实体集合

    /**@see HeatResistantSlateBlockEntity#getBakingSpeed() */
    protected int bakingTime;
    protected int bakingTimeTotal;

    protected final Object2IntOpenHashMap<Identifier> recipesUsed = new Object2IntOpenHashMap<>();
    protected final RecipeManager.MatchGetter<Inventory, ? extends StoveRecipe> matchGetter;
    @Nullable
    protected Recipe<?> lastRecipe;

    public int age;
    protected int lastCheckTime = 0;

    public HeatResistantSlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.HEAT_RESISTANT_SLATE, pos, state, 1);
        this.matchGetter = RecipeManager.createCachedMatchGetter(ModRecipeTypes.STOVE);
        this.bakingTime = 0;
        this.bakingTimeTotal = 0;
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        // 保存多方块引用信息
        if (multiBlockRef != null && !multiBlockRef.isDisposed()) {
            nbt.put(MULTIBLOCK_REF_KEY, multiBlockRef.toNbt());
        }

        nbt.putBoolean("IsStoveValid", isValidStove);
        nbt.putInt("StoveStructureType", stoveStructureType);

        // 保存柴火堆位置集合
        if (!firewoodPos.isEmpty()) {
            NbtList firewoodList = new NbtList();
            for (BlockPos pos : firewoodPos) {
                firewoodList.add(FoodCraftUtils.serializeBlockPos(pos));
            }
            nbt.put("FirewoodPositions", firewoodList);
        }
        // 删除 CombinedHeatLevel 的保存
        if (resultDirection != null) {
            nbt.putString("resultDirection", resultDirection.asString());
        }

        // 保存烘烤进度
        nbt.putInt("BakingTime", bakingTime);
        nbt.putInt("BakingTimeTotal", bakingTimeTotal);
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        // 保存nbt数据用于重建引用
        if (nbt.contains(MULTIBLOCK_REF_KEY)) {
            this.refNbt = nbt.getCompound(MULTIBLOCK_REF_KEY);
        }

        // 只在服务端重建功能性的MultiBlockReference
        if (world != null && !world.isClient) {
            if (this.refNbt != null) {
                MultiBlockReference ref = ServerMultiBlockReference.fromNbt(world, refNbt);
                if (ref != null) {
                    this.multiBlockRef = ref;
                }
            }
        } else {
            // 在客户端，只重建显示信息
            if (this.refNbt != null) {
                this.multiBlockRef = new ClientMultiBlockReference(refNbt);
            }
        }

        this.isValidStove = nbt.getBoolean("IsStoveValid");
        this.stoveStructureType = nbt.getInt("StoveStructureType");

        // 读取柴火堆位置集合
        firewoodPos.clear();
        if (nbt.contains("FirewoodPositions")) {
            NbtList firewoodList = nbt.getList("FirewoodPositions", 10);
            for (int i = 0; i < firewoodList.size(); i++) {
                NbtCompound posTag = firewoodList.getCompound(i);
                BlockPos pos = FoodCraftUtils.deserializeBlockPos(posTag);
                if (pos != null) {
                    firewoodPos.add(pos);
                }
            }
        }

        if (nbt.contains("resultDirection")){
            resultDirection = Direction.byName(nbt.getString("resultDirection"));
        }
        // 读取烘烤进度
        bakingTime = nbt.getInt("BakingTime");
        bakingTimeTotal = nbt.getInt("BakingTimeTotal");
    }

    @Override
    public VoxelShape getContentShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        BlockState itemState = this.getInventoryBlockState();
        Block itemBlock = itemState.getBlock();
        if (itemBlock instanceof FoodBlock foodBlock) {
            return FoodShapeHandle.getInstance().getShape(itemState, foodBlock.NUMBER_OF_FOOD)
                    .offset(0.0, INPUT_OFFSET_Y, 0.0);
        }else if (itemBlock != Blocks.AIR){
            return itemBlock.getDefaultState().getOutlineShape(world, pos).offset(0.0, INPUT_OFFSET_Y, 0.0);
        }
        return Block.createCuboidShape(0,0,0,0,0,0);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        this.inventory.set(slot, stack);
        limitStackSizeIfNeeded(stack);
    }

    @Override
    public boolean isValidItem(ItemStack stack) {
        return isValidGrindingInput(stack);
    }

    @Override
    public ActionResult tryAddItem(ItemStack stack) {
        if (stack.isEmpty() || !isValidItem(stack)) {
            return ActionResult.FAIL;
        }

        ItemStack newStack = stack.copy();
        newStack.setCount(1);

        if (isEmpty()){
            this.setStack(0, newStack);
            this.markDirtyAndSync();
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    @Override
    public ActionResult tryFetchItem(PlayerEntity player) {
        ItemStack contentStack = this.getStack(0);
        if (contentStack.isEmpty()) {
            return ActionResult.FAIL;
        }
        // 给予玩家物品
        if (!player.isCreative() && !player.giveItemStack(contentStack)) {
            player.dropItem(contentStack, false); // 背包满时掉落
        }

        // 减少容器中的物品数量
        contentStack.decrement(1);
        if (contentStack.isEmpty()) {
            this.setStack(0, ItemStack.EMPTY);
        }

        this.markDirtyAndSync();
        return ActionResult.SUCCESS;
    }

    /**
     * 获取当前物品栏中的物品对应的方块状态
     * @return 物品对应的方块状态
     */
    public BlockState getInventoryBlockState() {
        ItemStack stack = this.inventory.get(0);
        Item item = stack.getItem();
        Direction facing = Direction.EAST;

        if (resultDirection != null){
            facing = this.resultDirection;
        }
        if (item instanceof DoubleBlockItem doubleBlockItem && doubleBlockItem.getSecondBlock() instanceof FoodBlock) {
            return FoodCraftUtils.createFoodBlockState(doubleBlockItem.getSecondBlock().getDefaultState(), stack.getCount(), facing);
        } else if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof FoodBlock) {
            return FoodCraftUtils.createFoodBlockState(blockItem.getBlock().getDefaultState(), stack.getCount(), facing);
        }else if (item instanceof BlockItem blockItem){
            if (FoodCraftUtils.hasProperty(blockItem.getBlock(), Properties.HORIZONTAL_FACING)){
                return blockItem.getBlock().getDefaultState()
                        .with(Properties.HORIZONTAL_FACING, facing);
            }
            return blockItem.getBlock().getDefaultState();
        }

        return Blocks.AIR.getDefaultState();
    }

    /**
     * 设置多方块引用，并标记需要同步
     */
    public void setMultiBlockReference(@Nullable MultiBlockReference ref) {
        // 客户端不允许直接设置引用
        if (world != null && (world.isClient || ref instanceof ClientMultiBlockReference)) {
            return;
        }

        // 先清理旧的引用
        if (this.multiBlockRef != null) {
            this.multiBlockRef.dispose();
        }

        this.multiBlockRef = ref;

        // 重置结构检查状态
        this.currentStoveResult = null;
        this.stoveStructureType = -1;
        this.isValidStove = false;

        // 标记需要保存和同步
        this.markDirty();

        if (world != null) {
            // 通知客户端更新
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    /**
     * 获取多方块引用
     */
    @Nullable
    public MultiBlockReference getMultiBlockReference() {
        return multiBlockRef;
    }

    /**
     * 检查是否为多方块结构的主方块
     */
    public boolean isMasterBlock() {
        return multiBlockRef != null && multiBlockRef.isMasterBlock();
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        // 清理引用
        if (multiBlockRef != null) {
            multiBlockRef.dispose();
            multiBlockRef = null;
        }
    }

    /**
     * 检查方块堆是否符合炉子结构要求
     */
    public boolean isValidStoveStructure() {
        if (multiBlockRef == null || multiBlockRef.isDisposed()) {
            return false;
        }
        if (multiBlockRef instanceof ServerMultiBlockReference multiBlockReference){
            MultiBlock.PatternRange range = multiBlockReference.getMultiBlock().getRange();

            int width = range.getWidth();
            int height = range.getHeight();
            int depth = range.getDepth();

            // 炉子结构要求：水平方向，高度为1
            if (height != 1) {
                return false;
            }

            // 检查是否为有效尺寸：1x1, 1x2, 2x2, 2x3
            return (width == 1 && depth == 1) ||  // 1x1
                    (width == 1 && depth == 2) ||  // 1x2
                    (width == 2 && depth == 1) ||
                    (width == 2 && depth == 2) ||  // 2x2
                    (width == 3 && depth == 2) ||
                    (width == 2 && depth == 3);    // 2x3
        }
        return false;
    }

    /**
     * 获取炉子结构的类型索引
     */
    public int getStoveStructureType() {
        if (!isValidStoveStructure()) {
            return -1;
        }

        if (multiBlockRef instanceof ServerMultiBlockReference multiBlockReference){
            MultiBlock.PatternRange range = multiBlockReference.getMultiBlock().getRange();
            int width = range.getWidth();
            int depth = range.getDepth();

            if (width == 1 && depth == 1) return 1;  // 1x1
            if (width == 2 && depth == 1) return 2;  // 2x1
            if (width == 1 && depth == 2) return 2;  // 1x2
            if (width == 2 && depth == 2) return 3;  // 2x2
            if (width == 3 && depth == 2) return 4;  // 3x2
            if (width == 2 && depth == 3) return 4;  // 2x3
        }
        return -1;
    }

    /**
     * 获取对应的炉子图案
     * @param index 图案索引
     * @return 对应的炉子图案
     */
    @Nullable
    public BlockPattern getStovePattern(int index){
        if (getCachedState().getBlock() instanceof HeatResistantSlateBlock heatResistantSlateBlock){
            return heatResistantSlateBlock.getStovePattern(index);
        }
        return null;
    }

    /**
     * 获取当前炉子结构检查结果
     */
    @Nullable
    public BlockPattern.Result getCurrentStoveResult() {
        return currentStoveResult;
    }

    /**
     * 获取当前炉子结构类型
     */
    public int getCurrentStoveStructureType() {
        return stoveStructureType;
    }

    /**
     * 检查当前是否有效的炉子结构
     */
    public boolean isStoveValid() {
        return isValidStove;
    }

    /**
     * 当炉子结构有效时调用
     */
    private void onStoveStructureValid(World world, BlockPos pos, BlockPattern.Result result, int patternType) {
        // 结构匹配成功，绑定柴火堆
        bindFirewoodFromStructure(world, result, patternType);
        if (this.currentStoveResult != null) {
            this.resultDirection = this.currentStoveResult.getForwards();
        }
    }

    /**
     * 当炉子结构无效时调用
     */
    private void onStoveStructureInvalid(World world, BlockPos pos) {
        clearFirewoodBinding();
        if (this.currentStoveResult == null){
            this.resultDirection = null;
        }
    }

    /**
     * 扩大搜索范围以提高匹配成功率
     */
    private BlockPattern.Result searchAround(World world, BlockPos searchPos, int patternType, BlockPattern pattern) {
        for (int i = 0; i < patternType + 2; i++) {
            List<BlockPos> params = Arrays.asList(
                    searchPos.offset(Direction.EAST, i),
                    searchPos.offset(Direction.WEST, i),
                    searchPos.offset(Direction.NORTH, i),
                    searchPos.offset(Direction.SOUTH, i)
            );

            for (BlockPos pos : params) {
                BlockPattern.Result result = pattern.searchAround(world, pos);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void bindFirewoodFromStructure(World world, BlockPattern.Result result, int patternType) {
        // 获取所有'~'字符对应的位置
        Set<BlockPos> newFirewoodPositions = FoodCraftUtils.findTargetPositionsFromPattern(result, patternType, HeatResistantSlateBlockEntity::isFirewoodPositionPredicate);

        if (!newFirewoodPositions.isEmpty()) {
            this.firewoodPos = newFirewoodPositions;
            this.firewoodEntities.clear(); // 将在下次tick时重新获取
            markDirty();
        } else {
            clearFirewoodBinding();
            LOGGER.warn("Failed to find firewood positions in pattern type: {}", patternType);
        }
    }

    /**
     * 检查位置是否匹配柴火堆的谓词条件
     * @param cachedPos 缓存的方块位置
     * @return 如果位置是空气或有效的柴火堆则返回true，否则返回false
     */
    private static boolean isFirewoodPositionPredicate(@NotNull CachedBlockPosition cachedPos) {
        BlockState state = cachedPos.getBlockState();
        return state.isAir() ||
                state.getBlock() instanceof FirewoodBlock ||
                state.getBlock() instanceof CombustionFirewoodBlock;
    }

    /**
     * 检查绑定的柴火堆位置是否有效（即使当前是空气）
     * @param world 世界
     * @return 如果所有位置都是空气或有效的柴火堆则返回true，否则返回false
     */
    private boolean areFirewoodPositionsValid(World world) {
        if (firewoodPos.isEmpty()) return false;

        for (BlockPos pos : firewoodPos) {
            BlockState currentState = world.getBlockState(pos);
            if (!currentState.isAir() &&
                    !(currentState.getBlock() instanceof FirewoodBlock) &&
                    !(currentState.getBlock() instanceof CombustionFirewoodBlock)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证位置是否是有效的柴火堆
     * @param world 世界
     * @param pos 位置
     * @return 如果位置是有效的柴火堆则返回true，否则返回false
     */
    private boolean isValidFirewoodPosition(@NotNull World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof CombustionFirewoodBlock;
    }

    /**
     * 检查绑定的柴火堆是否有效
     * @param world 世界
     * @return 如果至少有一个有效的柴火堆则返回true，否则返回false
     */
    protected boolean areFirewoodValid(World world) {
        boolean result = false;
        if (!firewoodPos.isEmpty()) {
            for (BlockPos pos : firewoodPos) {
                BlockState firewoodState = world.getBlockState(pos);
                if (firewoodState.getBlock() instanceof CombustionFirewoodBlock) {
                    result = true;
                    break;// 至少有一个有效地燃烧柴火堆
                }
            }
        }

        return result;
    }

    /**
     * 清除柴火堆绑定
     */
    private void clearFirewoodBinding() {
        this.firewoodPos.clear();
        this.firewoodEntities.clear();
        markDirty();
    }

    /**
     * 更新绑定的柴火堆
     */
    private void updateFirewood(World world) {
        if (world.isClient) return;

        // 如果没有绑定柴火堆位置，清空缓存
        if (firewoodPos.isEmpty()) {
            firewoodEntities.clear();
            return;
        }

        // 获取所有柴火堆方块实体
        if (firewoodEntities.isEmpty() || firewoodEntities.stream().anyMatch(BlockEntity::isRemoved)) {
            firewoodEntities.clear();
            for (BlockPos pos : firewoodPos) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof CombustionFirewoodBlockEntity combustionBE) {
                    firewoodEntities.add(combustionBE);
                }
            }
        }
    }

    public Set<BlockPos> getFirewoodPositions() {
        return Collections.unmodifiableSet(firewoodPos);
    }

    /**
     * 注意：尝试修改此集合是没有效果的
     * @return 绑定的柴火堆方块实体的集合
     */
    public Set<CombustionFirewoodBlockEntity> getFirewoodEntities() {
        return firewoodEntities;
    }

    public int getActiveFirewoodCount() {
        return (int) firewoodEntities.stream()
                .filter(Objects::nonNull)
                .filter(CombustionFirewoodBlockEntity::isCombusting)
                .count();
    }

    /**
     * 处理烘烤逻辑
     */
    private void processBaking(World world) {
        // 检查是否有输入物品
        if (isEmpty()) {
            resetBakingProgress();
            return;
        }

        // 获取匹配的配方
        StoveRecipe recipe = this.matchGetter.getFirstMatch(this, world).orElse(null);
        if (recipe == null) {
            resetBakingProgress();
            return;
        }

        // 初始化烘烤总时间
        if (bakingTimeTotal == 0) {
            bakingTimeTotal = Math.max(recipe.getBakingTime(), MIN_BAKING_TIME);
        }

        // 根据热量等级决定烘烤速度
        int bakingSpeed = getBakingSpeed();

        // 增加烘烤进度
        bakingTime += bakingSpeed;

        // 检查是否烘烤完成
        if (bakingTime >= bakingTimeTotal) {
            completeBaking(world, recipe);
        }

        // 标记需要同步（用于客户端渲染进度）
        markDirtyAndSync();
    }

    /**
     * 获取烘烤速度
     */
    private int getBakingSpeed() {
        if (firewoodEntities.isEmpty()) {
            return 0;
        }

        int activeFirewoodCount = 0;
        double totalEffectiveSpeed = 0;

        // 计算每个柴火堆的有效速度
        for (CombustionFirewoodBlockEntity firewoodBlock : firewoodEntities) {
            if (firewoodBlock != null && firewoodBlock.isCombusting()) {
                activeFirewoodCount++;

                // 单个柴火堆的基础速度 + 热量加成
                double individualSpeed = 10 + (firewoodBlock.getHeatLevel() - 1);

                // 应用收益递减：使用平方根函数
                double effectiveSpeed = individualSpeed / Math.pow(activeFirewoodCount, 0.7);
                totalEffectiveSpeed += effectiveSpeed;
            }
        }

        if (activeFirewoodCount == 0) {
            return 0;
        }

        // 最终结果取整，并确保至少为10
        int result = (int) Math.round(totalEffectiveSpeed);
        return Math.max(10, result);
    }

    /**
     * 完成烘烤，产出结果
     */
    private void completeBaking(World world, StoveRecipe recipe) {
        ItemStack inputStack = getStack(0);
        ItemStack outputStack = recipe.getOutput(world.getRegistryManager());

        if (inputStack.isEmpty() || outputStack.isEmpty()) {
            resetBakingProgress();
            return;
        }

        // 消耗输入物品
        setStack(0, outputStack.copy());

        // 记录使用的配方（用于奖励经验）
        setLastRecipe(recipe);
        recipesUsed.addTo(recipe.getId(), 1);

        // 重置烘烤进度
        resetBakingProgress();

        // 播放完成音效
        world.playSound(null, pos, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 0.5f, 1.0f);
    }

    /**
     * 重置烘烤进度
     */
    private void resetBakingProgress() {
        this.bakingTime = 0;
        this.bakingTimeTotal = 0;
        markDirty();
    }

    /**
     * 获取当前烘烤进度的百分比
     * @return 烘烤进度百分比（0.0 - 1.0）
     */
    public float getBakingProgress() {
        if (bakingTimeTotal > 0) {
            return (float) bakingTime / bakingTimeTotal;
        }
        return 0.0f;
    }

    /**
     * 检查是否可以开始烘烤
     */
    public boolean canBake() {
        if (isEmpty() || !hasHeat() || !isValidStove) {
            return false;
        }

        StoveRecipe recipe = this.matchGetter.getFirstMatch(this, world).orElse(null);
        return recipe != null;
    }

    public int getBakingTime() {
        return bakingTime;
    }

    public int getBakingTimeTotal() {
        return bakingTimeTotal;
    }

    public boolean hasHeat(){
        for (CombustionFirewoodBlockEntity firewood : firewoodEntities) {
            if (firewood.isCombusting()) {
                return true;
            }
        }
        return false;
    }

    public boolean isBaking() {
        return bakingTime > 0;
    }

    /**
     * 检查物品是否能作为配方输入
     * @param stack 要输入的物品堆栈
     * @return 是否可以作为配方输入
     */
    private boolean isValidGrindingInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Inventory tempInventory = new SimpleInventory(stack);
        return this.matchGetter.getFirstMatch(tempInventory, this.world).isPresent();
    }

    /**
     * 获取预期的输出
     * @return 预期输出的物品
     */
    public ItemStack getExpectedOutput() {
        StoveRecipe recipe = this.matchGetter.getFirstMatch(this, this.world).orElse(null);
        if (recipe != null) {
            return recipe.getOutput(world != null ? world.getRegistryManager() : null);
        }
        return ItemStack.EMPTY;
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

    public static void tick(World world, BlockPos pos, BlockState state, @NotNull HeatResistantSlateBlockEntity blockEntity) {
        // 每tick增加方块寿命
        blockEntity.age++;
        if (blockEntity.age == Integer.MAX_VALUE) {
            blockEntity.age = 0;
        }

        // 尝试检查结构
        boolean interval = blockEntity.age - blockEntity.lastCheckTime < MIN_CHECK_INTERVAL;
        if (!interval) {
            blockEntity.checkPattern(world, pos, state);
            blockEntity.lastCheckTime = blockEntity.age;
        }

        // 更新绑定的柴火堆
        blockEntity.updateFirewood(world);

        // 如果有热量且炉子结构有效，处理烘烤逻辑
        if (blockEntity.hasHeat() && blockEntity.isValidStove) {
            blockEntity.processBaking(world);
        } else {
            // 没有热量时重置烘烤进度
            blockEntity.resetBakingProgress();
        }

        blockEntity.markDirty();
    }

    private void checkPattern(World world, BlockPos pos, BlockState state) {
        // 重置检查结果
        this.currentStoveResult = null;
        this.stoveStructureType = -1;
        this.isValidStove = false;

        if (world != null && !world.isClient &&
                (this.multiBlockRef == null || this.multiBlockRef instanceof ClientMultiBlockReference)){
            // 如果因为某些意外导致引用为空或者实现了客户端引用，则尝试重构引用
            MultiBlockReference ref = ServerMultiBlockReference.fromNbt(world, refNbt);
            if (ref != null) {
                setMultiBlockReference(ref);
            }
        }

        // 如果没有多方块引用，不进行检查
        if (multiBlockRef == null || multiBlockRef.isDisposed()) {
            return;
        }

        // 直接同步主方块的数据
        if (!multiBlockRef.isMasterBlock()){
            if (world != null &&
                    world.getBlockEntity(this.multiBlockRef.getMasterWorldPos()) instanceof HeatResistantSlateBlockEntity masterBlockEntity) {
                // 同步主方块的柴火堆信息
                this.firewoodPos = new HashSet<>(masterBlockEntity.firewoodPos);
                this.firewoodEntities = new HashSet<>(masterBlockEntity.firewoodEntities);

                // 同步主方块的方块图案数据
                this.currentStoveResult = masterBlockEntity.currentStoveResult;
                this.resultDirection = masterBlockEntity.resultDirection;
                this.stoveStructureType = masterBlockEntity.stoveStructureType;
                this.isValidStove = masterBlockEntity.isValidStove;

                return;
            }
        }

        // 检查多方块结构是否有效
        if (!multiBlockRef.checkIntegrity()) {
            return;
        }

        // 检查是否符合炉子结构要求
        if (!isValidStoveStructure()) {
            return;
        }

        // 根据多方块尺寸获取对应的炉子图案类型
        int patternType = getStoveStructureType();
        if (patternType == -1) {
            return;
        }

        // 获取对应的BlockPattern
        BlockPattern pattern = getStovePattern(patternType);
        if (pattern == null) {
            return;
        }

        // 在周围搜索匹配的炉子结构
        // 使用主方块位置作为搜索起点
        BlockPos searchPos = multiBlockRef.getMasterWorldPos();
        pattern.searchAround(world, searchPos);
        BlockPattern.Result result = searchAround(world, searchPos, patternType, pattern);

        if (result != null) {
            this.currentStoveResult = result;
            this.stoveStructureType = patternType;
            this.isValidStove = true;

            // 结构匹配成功
            onStoveStructureValid(world, pos, result, patternType);
        } else {
            // 结构不匹配
            onStoveStructureInvalid(world, pos);
        }
    }

    public @Nullable Direction getResultDirection(){
        return this.resultDirection;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{0};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == 0;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == 0;
    }
}