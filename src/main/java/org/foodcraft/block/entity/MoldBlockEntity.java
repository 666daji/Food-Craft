package org.foodcraft.block.entity;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.block.MoldBlock;
import org.foodcraft.recipe.MoldRecipe;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.registry.ModRecipeTypes;
import org.foodcraft.util.FoodCraftUtils;
import org.jetbrains.annotations.Nullable;

public class MoldBlockEntity extends UpPlaceBlockEntity implements SidedInventory, RecipeUnlocker, RecipeInputProvider {
    protected static final int MAX_STACK_SIZE = 1;

    protected final Object2IntOpenHashMap<Identifier> recipesUsed = new Object2IntOpenHashMap<>();
    protected final RecipeManager.MatchGetter<Inventory, ? extends MoldRecipe> matchGetter;
    @Nullable
    protected Recipe<?> lastRecipe;

    public MoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.MOLD, pos, state, 1);
        this.matchGetter = RecipeManager.createCachedMatchGetter(ModRecipeTypes.MOLD);
    }

    @Override
    public VoxelShape getContentShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return Block.createCuboidShape(0, 0, 0, 0, 0, 0);
    }

    /**
     * 检查物品是否能作为配方输入
     * @param stack 要输入的物品堆栈
     * @return 是否可以作为配方输入
     */
    private boolean isValidMoldInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Inventory tempInventory = new SimpleInventory(stack);

        MoldRecipe recipe = this.matchGetter.getFirstMatch(tempInventory, this.world).orElse(null);
        if (recipe != null && recipe.getBaseMoldItem() instanceof BlockItem blockItem){
            return blockItem.getBlock() == this.getCachedState().getBlock();
        }

        return false;
    }

    public BlockState getInventoryBlockState() {
        ItemStack stack = this.getStack(0);
        Direction facing = this.getCachedState().get(MoldBlock.FACING);

        return FoodCraftUtils.createCountBlockstate(stack, facing);
    }

    @Override
    public boolean isValidItem(ItemStack stack) {
        return isValidMoldInput(stack);
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        MoldRecipe recipe = this.matchGetter.getFirstMatch(this, this.world).orElse(null);
        if (recipe == null) {
            return;
        }
        craftRecipe(recipe);
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
     * 使用指定的模具配方进行制作
     * @param recipe 要使用的模具配方
     */
    protected void craftRecipe(MoldRecipe recipe) {
        ItemStack output = recipe.craft(this, null);
        setStack(0, output);

        setLastRecipe(recipe);
        recipesUsed.addTo(recipe.getId(), 1);

        markDirtyAndSync();
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
        return null;
    }

    @Override
    public int getMaxCountPerStack() {
        return MAX_STACK_SIZE;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
