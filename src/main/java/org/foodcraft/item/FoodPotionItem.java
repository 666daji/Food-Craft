package org.foodcraft.item;

import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class FoodPotionItem extends BlockItem {
    public FoodPotionItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack itemStack = super.finishUsing(stack, world, user);
        return user instanceof PlayerEntity && ((PlayerEntity)user).getAbilities().creativeMode ? itemStack : new ItemStack(Items.GLASS_BOTTLE);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }
}