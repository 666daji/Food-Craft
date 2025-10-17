package org.foodcraft.integration.dfood;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.dfood.block.FoodBlocks;
import org.dfood.shape.FoodShapeHandle;
import org.dfood.util.IntPropertyManager;
import org.foodcraft.block.CrippledBlock;
import org.foodcraft.item.ModPotions;
import org.foodcraft.util.FoodCraftUtils;

/**
 * 水桶的残损方块，表示被使用过的水桶
 */
public class CrippledWaterBucketBlock extends CrippledBlock {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final FoodShapeHandle foodShapeHandle = FoodShapeHandle.getInstance();

    private final Potion potionType;

    public CrippledWaterBucketBlock(Settings settings, Block baseBlock, Potion potionType) {
        super(settings, 3, baseBlock, new ItemStack(Items.BUCKET));
        this.potionType = potionType;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return foodShapeHandle.getShape(state, NUMBER_OF_USE);
    }

    @Override
    protected ActionResult tryUse(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player) {
        ItemStack handStack = player.getMainHandStack();

        // 检查是否手持空瓶子
        if (handStack.getItem() == Items.GLASS_BOTTLE) {
            if (!player.canConsume(false)) {
                return ActionResult.PASS;
            }

            int i = state.get(NUMBER_OF_USE);
            world.playSound(player, pos, SoundEvents.ITEM_BOTTLE_FILL, player.getSoundCategory(), 1.0F, 1.0F);

            // 消耗空瓶子并给予水瓶
            if (!player.isCreative()) {
                handStack.decrement(1);
            }
            ItemStack waterBottle = PotionUtil.setPotion(new ItemStack(Items.POTION), this.potionType);
            if (this.potionType == ModPotions.MILK) {
                waterBottle = FoodCraftUtils.getMilkPotion();
            }
            if (!player.giveItemStack(waterBottle)) {
                player.dropItem(waterBottle, false);
            }

            world.emitGameEvent(player, GameEvent.FLUID_PICKUP, pos);

            if (i < 3) {
                world.setBlockState(pos, state.with(NUMBER_OF_USE, i + 1), Block.NOTIFY_ALL);
            } else {
                // 水用完了，变成空桶
                world.setBlockState(pos, FoodBlocks.BUCKET.getDefaultState()
                        .with(FACING, state.get(FACING)), Block.NOTIFY_ALL);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    public static BlockState getWaterBucketState(BlockState state) {
        for (Block block : AssistedBlocks.assistedBlocks) {
            if (block instanceof CrippledWaterBucketBlock crippledWaterBucketBlock && crippledWaterBucketBlock.isBaseBlock(state)) {
                return crippledWaterBucketBlock.getDefaultState()
                        .with(CrippledWaterBucketBlock.FACING, state.get(CrippledWaterBucketBlock.FACING))
                        .with(IntPropertyManager.create("number_of_use", 3), 1);
            }
        }
        return Blocks.AIR.getDefaultState();
    }
}