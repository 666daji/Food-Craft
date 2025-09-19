package org.foodcraft.integration.dfood;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.Collections;
import java.util.List;

/**
 * 该类的实例是一个过渡方块，表示不完整的方块
 */
public abstract class CrippledBlock extends Block {
    public static final IntProperty NUMBER_OF_USE = IntProperty.of("number_of_use", 1, 4);

    protected final Block baseBlock;
    protected final List<ItemStack> Remainder;

    public CrippledBlock(Settings settings, Block baseBlock, ItemStack... Remainder) {
        super(settings);
        this.baseBlock = baseBlock;
        this.Remainder = Remainder.length == 0 ? Collections.emptyList() : List.of(Remainder);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (world.isClient) {
            if (tryUse(world, pos, state, player).isAccepted()) {
                return ActionResult.SUCCESS;
            }

            if (itemStack.isEmpty()) {
                return ActionResult.CONSUME;
            }
        }

        return tryUse(world, pos, state, player);
    }

    protected abstract ActionResult tryUse(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player);

    public boolean isBaseBlock(BlockState state) {
        return state.getBlock() == baseBlock;
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        return Remainder;
    }

    public Block getBaseBlock() {
        return baseBlock;
    }
}
