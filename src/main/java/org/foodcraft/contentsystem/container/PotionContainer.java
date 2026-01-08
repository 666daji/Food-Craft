package org.foodcraft.contentsystem.container;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.util.Identifier;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.BaseLiquidContent;
import org.foodcraft.contentsystem.foodcraft.ModContents;
import org.foodcraft.registry.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PotionContainer extends ContainerType{

    public PotionContainer(Identifier id, ContainerSettings settings) {
        super(id, settings);
    }

    @Override
    public boolean matches(ItemStack stack) {
        // 空瓶
        if (stack.isOf(getEmptyItem())) {
            return true;
        }

        // 奶瓶
        if (stack.isOf(ModItems.MILK_POTION)) {
            return true;
        }

        // 水瓶
        return isWaterPotion(stack);
    }

    @Override
    public boolean canContain(AbstractContent content) {
        return BaseLiquidContent.CATEGORY.equals(content.getCategory());
    }

    @Override
    public @Nullable AbstractContent extractContent(ItemStack stack) {
        if (stack.isOf(ModItems.MILK_POTION)) {
            return ModContents.MILK;
        }

        if (isWaterPotion(stack)) {
            return ModContents.WATER;
        }

        return null;
    }

    @Override
    public @NotNull ItemStack createItemStack(AbstractContent content, int amount) {
        if (content.equals(ModContents.WATER)) {
            ItemStack result = new ItemStack(Items.POTION, amount);
            return PotionUtil.setPotion(result, Potions.WATER);
        }

        if (content.equals(ModContents.MILK)) {
            return new ItemStack(ModItems.MILK_POTION, amount);
        }

        return createEmptyItemStack(amount);
    }

    /**
     * 检查物品堆是否为水瓶。
     * @param stack 要检查的物品堆
     * @return 如果物品堆是水瓶则返回true，否则返回false
     */
    public static boolean isWaterPotion(ItemStack stack) {
        if (stack.getItem() instanceof PotionItem) {
            return stack.isOf(Items.POTION) &&
                    PotionUtil.getPotion(stack) == Potions.WATER;
        }
        return false;
    }
}
