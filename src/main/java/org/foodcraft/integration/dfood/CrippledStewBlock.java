package org.foodcraft.integration.dfood;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.dfood.block.FoodBlock;
import org.dfood.block.FoodBlocks;
import org.dfood.shape.FoodShapeHandle;

import java.util.List;

/**
 * 该类的实例是一个过渡方块，表示被食用过的汤
 */
public class CrippledStewBlock extends CrippledBlock {
    public final FoodComponent foodComponent;
    public static final DirectionProperty FACING = Properties.FACING;

    private static final FoodShapeHandle foodShapeHandle = FoodShapeHandle.getInstance();

    public CrippledStewBlock(Settings settings, FoodComponent foodComponent, Block baseBlock) {
        super(settings, baseBlock);
        this.foodComponent = foodComponent;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return foodShapeHandle.getShape(state, NUMBER_OF_USE);
    }

    @Override
    protected ActionResult tryUse(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!player.canConsume(false)) {
            return ActionResult.PASS;
        } else {
            world.playSound(player, pos, SoundEvents.ENTITY_GENERIC_DRINK, player.getSoundCategory(), 1.0F, 1.0F);
            player.getHungerManager().add(foodComponent.getHunger() / 4, foodComponent.getSaturationModifier() / 4.0F);
            int i = state.get(NUMBER_OF_USE);
            world.emitGameEvent(player, GameEvent.EAT, pos);
            if (i < 4) {
                world.setBlockState(pos, state.with(NUMBER_OF_USE, i + 1), Block.NOTIFY_ALL);
            } else {
                world.setBlockState(pos, FoodBlocks.BOWL.getDefaultState()
                        .with(FACING, state.get(FACING)), Block.NOTIFY_ALL);
            }

            return ActionResult.SUCCESS;
        }
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        return List.of(new ItemStack(Items.BOWL));
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world,pos, state, player);
        if (!world.isClient && state.get(NUMBER_OF_USE) > 0) {
            int hunger = foodComponent.getHunger() / 4;
            float saturation = foodComponent.getSaturationModifier() / 4.0F;
            int numberOfEat = state.get(NUMBER_OF_USE);
            player.getHungerManager().add(hunger * numberOfEat, saturation * numberOfEat);
            world.emitGameEvent(player, GameEvent.EAT, pos);
        }
    }

    public static BlockState getStewState(BlockState state) {
        for (Block block : AssistedBlocks.assistedBlocks) {
            if (block instanceof CrippledStewBlock crippledStewBlock) {
                if (crippledStewBlock.isBaseBlock(state)) {
                    return crippledStewBlock.getDefaultState()
                            .with(CrippledStewBlock.FACING, state.get(Properties.FACING))
                            .with(NUMBER_OF_USE, 1);
                }
            }
        }
        return Blocks.AIR.getDefaultState();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(NUMBER_OF_USE);
    }

    public boolean isSuspiciousStew() {
        return this instanceof CrippledSuspiciousStewBlock;
    }
}
