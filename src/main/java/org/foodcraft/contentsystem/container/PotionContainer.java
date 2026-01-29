package org.foodcraft.contentsystem.container;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.util.Identifier;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.ContentCategories;
import org.foodcraft.registry.ModContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PotionContainer extends AbstractMappedContainer {
    public PotionContainer(Identifier id, ContainerSettings settings) {
        super(id, settings);
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (stack.isOf(getEmptyItem()) || supportsItem(stack.getItem())) {
            return true;
        }

        // 水瓶
        return isWaterPotion(stack);
    }

    @Override
    public boolean canContain(AbstractContent content) {
        return content.isIn(ContentCategories.BASE_LIQUID) || content.isIn(ContentCategories.SYRUP);
    }

    @Override
    public @Nullable AbstractContent extractContent(ItemStack stack) {
        if (isWaterPotion(stack)) {
            return ModContents.WATER;
        }

        return super.extractContent(stack);
    }

    @Override
    public @NotNull ItemStack replaceContent(@NotNull ItemStack stack, @Nullable AbstractContent content) {
        if (content != null && content.equals(ModContents.WATER)) {
            ItemStack result = new ItemStack(Items.POTION, stack.getCount());
            if (stack.hasNbt()) {
                result.setNbt(stack.getNbt());
            }
            return PotionUtil.setPotion(result, Potions.WATER);
        }

        return super.replaceContent(stack, content);
    }

    @Override
    @NotNull
    public ItemStack createItemStack(AbstractContent content, int amount) {
        // 特殊处理水瓶
        if (content.equals(ModContents.WATER)) {
            ItemStack result = new ItemStack(Items.POTION, amount);
            return PotionUtil.setPotion(result, Potions.WATER);
        }

        return super.createItemStack(content, amount);
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

    /**
     * @apiNote 如果要调用该方法请先使用isWaterPotion方法检查堆栈是否为水瓶
     */
    @Override
    public boolean supportsItem(Item item) {
        return super.supportsItem(item);
    }
}