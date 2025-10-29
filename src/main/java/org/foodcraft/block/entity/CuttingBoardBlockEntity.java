package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.foodcraft.block.CuttingBoardBlock;
import org.foodcraft.recipe.CutRecipe;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.registry.ModRecipeTypes;
import org.foodcraft.util.FoodCraftUtils;

import java.util.Optional;

public class CuttingBoardBlockEntity extends UpPlaceBlockEntity {
    private static final VoxelShape CONTENT_SHAPE = VoxelShapes.cuboid(0.125, 0.125, 0.125, 0.875, 0.25, 0.875);

    private final RecipeManager.MatchGetter<Inventory, CutRecipe> cutRecipeMatchGetter;

    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.CUTTING_BOARD, pos, state, 1);
        this.cutRecipeMatchGetter = RecipeManager.createCachedMatchGetter(ModRecipeTypes.CUT);
    }

    @Override
    public VoxelShape getContentShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return !isEmpty() ? CONTENT_SHAPE : VoxelShapes.empty();
    }

    /**
     * 获取当前物品栏中的物品对应的方块状态
     * @return 物品对应的方块状态
     */
    public BlockState getInventoryBlockState() {
        ItemStack stack = this.inventory.get(0);
        Direction facing = this.getCachedState().get(CuttingBoardBlock.FACING);

        return FoodCraftUtils.createCountBlockstate(stack, facing);
    }

    @Override
    public boolean isValidItem(ItemStack stack) {
        if (world == null || stack.isEmpty()) return false;

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
    public ActionResult tryFetchItem(PlayerEntity player) {
        if (!isEmpty()) {
            ItemStack stack = removeStack(0);
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
            markDirtyAndSync();
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    /**
     * 尝试切割物品
     */
    public ActionResult tryCutItem(PlayerEntity player, ItemStack tool) {
        if (world == null || isEmpty()) {
            return ActionResult.FAIL;
        }

        // 检查工具是否是菜刀（剑）
        if (!(tool.getItem() instanceof SwordItem)) {
            return ActionResult.FAIL;
        }

        // 使用 MatchGetter 查找匹配的配方
        Optional<CutRecipe> recipeOpt = cutRecipeMatchGetter.getFirstMatch(this, world);

        if (recipeOpt.isPresent()) {
            CutRecipe recipe = recipeOpt.get();

            // 移除输入物品
            removeStack(0);

            // 创建输出物品
            ItemStack output = recipe.getOutput(world.getRegistryManager()).copy();
            output.setCount(recipe.getOutputCount());

            // 放置输出物品到菜板上
            setStack(0, output);

            // 消耗工具耐久
            if (!player.isCreative()) {
                tool.damage(1, player, p -> p.sendToolBreakStatus(Hand.MAIN_HAND));
            }

            markDirtyAndSync();
            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
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