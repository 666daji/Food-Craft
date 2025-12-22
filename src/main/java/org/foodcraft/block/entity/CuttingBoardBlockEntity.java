package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.block.CuttingBoardBlock;
import org.foodcraft.block.process.CuttingProcess;
import org.foodcraft.recipe.CutRecipe;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.registry.ModItems;
import org.foodcraft.registry.ModRecipeTypes;
import org.foodcraft.registry.ModSounds;
import org.foodcraft.util.FoodCraftUtils;

import java.util.List;
import java.util.Optional;

public class CuttingBoardBlockEntity extends UpPlaceBlockEntity {
    private static final VoxelShape CONTENT_SHAPE = VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.875, 0.25, 0.875);

    private final RecipeManager.MatchGetter<Inventory, CutRecipe> cutRecipeMatchGetter;
    private final CuttingProcess<CuttingBoardBlockEntity> cuttingProcess;

    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CUTTING_BOARD, pos, state, 5); // 5个槽位
        this.cutRecipeMatchGetter = RecipeManager.createCachedMatchGetter(ModRecipeTypes.CUT);
        this.cuttingProcess = new CuttingProcess<>();
    }

    @Override
    public VoxelShape getContentShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return !isEmpty() ? CONTENT_SHAPE : VoxelShapes.empty();
    }

    /**
     * 获取当前物品栏中的物品对应的方块状态
     */
    public BlockState getInventoryBlockState() {
        ItemStack stack = this.inventory.get(0);
        Direction facing = this.getCachedState().get(CuttingBoardBlock.FACING);

        return FoodCraftUtils.createCountBlockstate(stack, facing);
    }

    @Override
    public boolean isValidItem(ItemStack stack) {
        if (world == null || stack.isEmpty()) return false;

        if (stack.getItem().equals(ModItems.KITCHEN_KNIFE)) {
            return true;
        }

        // 临时设置物品到槽位中进行配方匹配
        ItemStack originalStack = getStack(0);
        setStack(0, stack);

        boolean isValid = cutRecipeMatchGetter.getFirstMatch(this, world).isPresent();

        // 恢复原始状态
        setStack(0, originalStack);

        return isValid;
    }

    @Override
    public ActionResult tryAddItem(ItemStack stack) {
        if (isEmpty() && isValidItem(stack)) {
            ItemStack placedStack = stack.copy();
            placedStack.setCount(1);
            setStack(0, placedStack);
            markDirtyAndSync();
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    @Override
    public void onPlace(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, ItemStack placeStack) {
        if (placeStack.getItem().equals(ModItems.KITCHEN_KNIFE)) {
            world.playSound(
                    null, pos,
                    ModSounds.KITCHEN_KNIFE_BOARD_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F
            );

            if (!player.isCreative()) {
                placeStack.decrement(1);
            }
            return;
        }

        super.onPlace(state, world, pos, player, hand, hit, placeStack);
    }

    @Override
    public ActionResult tryFetchItem(PlayerEntity player) {
        // 如果切菜流程在进行中，不允许取出物品
        if (cuttingProcess.isActive() ) {
            return ActionResult.FAIL;
        }

        if (!isEmpty()) {
            ItemStack stack = removeStack(0, 1);
            this.fetchStacks = List.of(stack.copy());
            if (!player.isCreative() && !player.giveItemStack(stack)) {
                player.dropItem(stack, false);
            }
            markDirtyAndSync();
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    /**
     * 尝试开始或继续切菜流程
     */
    public ActionResult tryCutItem(PlayerEntity player, ItemStack tool, Hand hand, BlockHitResult hit) {
        // 如果没有活跃的流程，尝试开始新的流程
        if (!cuttingProcess.isActive() && cuttingProcess.isValidCuttingTool(tool) && !isEmpty()) {
            Optional<CutRecipe> recipeOpt = cutRecipeMatchGetter.getFirstMatch(this, world);

            if (recipeOpt.isPresent()) {
                cuttingProcess.start(world, this);
            }
        }

        // 继续执行切菜流程
        return cuttingProcess.executeStep(
                this, getCachedState(), world, pos, player, hand, hit
        );
    }

    /**
     * 获取切菜流程
     */
    public CuttingProcess<CuttingBoardBlockEntity> getCuttingProcess() {
        return cuttingProcess;
    }

    /**
     * 查找配方（用于NBT恢复）
     */
    public Optional<CutRecipe> findRecipeById(String recipeId) {
        if (world == null || recipeId == null) {
            return Optional.empty();
        }

        // 重新匹配当前库存
        return cutRecipeMatchGetter.getFirstMatch(this, world);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        inventory.clear();
        Inventories.readNbt(nbt, inventory);

        if (nbt.contains("CuttingProcess")) {
            NbtCompound processNbt = nbt.getCompound("CuttingProcess");
            cuttingProcess.readFromNbt(processNbt);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        Inventories.writeNbt(nbt, inventory);

        NbtCompound processNbt = new NbtCompound();
        cuttingProcess.writeToNbt(processNbt);
        nbt.put("CuttingProcess", processNbt);
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