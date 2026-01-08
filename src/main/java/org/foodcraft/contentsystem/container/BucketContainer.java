package org.foodcraft.contentsystem.container;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.BaseLiquidContent;
import org.foodcraft.contentsystem.foodcraft.ModContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BucketContainer extends ContainerType{

    public BucketContainer(Identifier id, ContainerSettings settings) {
        super(id, settings);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.isOf(Items.WATER_BUCKET) || stack.isOf(Items.MILK_BUCKET)
                || stack.isOf(getEmptyItem());
    }

    @Override
    public boolean canContain(AbstractContent content) {
        return BaseLiquidContent.CATEGORY.equals(content.getCategory());
    }

    @Override
    public @Nullable AbstractContent extractContent(ItemStack stack) {
        if (stack.isOf(Items.WATER_BUCKET)) {
            return ModContents.WATER;
        }

        if (stack.isOf(Items.MILK_BUCKET)) {
            return ModContents.MILK;
        }

        return null;
    }

    @Override
    public @NotNull ItemStack createItemStack(AbstractContent content, int amount) {
        if (content.equals(ModContents.MILK)) {
            return new ItemStack(Items.MILK_BUCKET, amount);
        }

        if (content.equals(ModContents.WATER)) {
            return new ItemStack(Items.WATER_BUCKET, amount);
        }

        return createEmptyItemStack(amount);
    }
}
