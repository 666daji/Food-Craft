package org.foodcraft.registry;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import org.dfood.block.FoodBlock;
import org.foodcraft.block.CrippledHardBreadBoatBlock;

import java.util.HashMap;
import java.util.Map;

public class FoodBlocksModifier {
    public static final Map<Item, BlockState> TARGET_BLOCK = new HashMap<>();

    public static final FoodBlock.onUseHook hardBreadBoatHook = ((state, world, pos, player, hand, hit) -> {
        ItemStack handStack = player.getStackInHand(hand);
        BlockState newState = CrippledHardBreadBoatBlock.getHardBreadBoatState(handStack, state);

        if (newState != null){
            world.setBlockState(pos, newState, 3);

            world.playSound(player, pos, SoundEvents.ITEM_BOTTLE_FILL, player.getSoundCategory(), 1.0F, 1.0F);
            if (!player.isCreative()) {
                handStack.decrement(1);
                ItemStack newStack = new ItemStack(Items.BOWL);
                if (!player.giveItemStack(newStack)) {
                    player.dropItem(newStack, false);
                }
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    });

    public static void init(){};

    static {
        TARGET_BLOCK.put(Items.BEETROOT_SOUP, ModBlocks.BEETROOT_SOUP_HARD_BREAD_BOAT.getDefaultState());
        TARGET_BLOCK.put(Items.MUSHROOM_STEW, ModBlocks.MUSHROOM_STEW_HARD_BREAD_BOAT.getDefaultState());

        ((FoodBlock)ModBlocks.HARD_BREAD_BOAT).setOnUseHook(hardBreadBoatHook);
    }
}
