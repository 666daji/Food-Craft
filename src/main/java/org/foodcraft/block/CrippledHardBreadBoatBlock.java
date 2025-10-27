package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.dfood.shape.FoodShapeHandle;
import org.dfood.util.IntPropertyManager;
import org.foodcraft.integration.dfood.CrippledStewBlock;
import org.foodcraft.registry.FoodBlocksModifier;
import org.foodcraft.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;

public class CrippledHardBreadBoatBlock extends CrippledBlock {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final FoodShapeHandle foodShapeHandle = FoodShapeHandle.getInstance();

    public final FoodComponent foodComponent;

    public CrippledHardBreadBoatBlock(Settings settings, int useNumber, FoodComponent foodComponent, Block baseBlock) {
        super(settings, useNumber, baseBlock);
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
            if (i < useNumber) {
                world.setBlockState(pos, state.with(NUMBER_OF_USE, i + 1), Block.NOTIFY_ALL);
            } else {
                world.breakBlock(pos, false);
            }

            return ActionResult.SUCCESS;
        }
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

    @Nullable
    public static BlockState getHardBreadBoatState(ItemStack handStack, BlockState state) {
        Item item = handStack.getItem();

        if (FoodBlocksModifier.TARGET_BLOCK.containsKey(item)){
            return FoodBlocksModifier.TARGET_BLOCK.get(item)
                    .with(CrippledStewBlock.FACING, state.get(CrippledStewBlock.FACING))
                    .with(IntPropertyManager.create("number_of_use", 4), 1);
        }

        return null;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }
}
