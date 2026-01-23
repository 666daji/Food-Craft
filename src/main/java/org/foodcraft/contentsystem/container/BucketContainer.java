package org.foodcraft.contentsystem.container;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.ContentCategories;

public class BucketContainer extends AbstractMappedContainer {
    public BucketContainer(Identifier id, ContainerSettings settings) {
        super(id, settings);
    }

    @Override
    public boolean matches(ItemStack stack) {
        // 检查是否是空桶或支持的桶装物品
        return stack.isOf(getEmptyItem()) || supportsItem(stack.getItem());
    }

    @Override
    public boolean canContain(AbstractContent content) {
        // 桶可以装基础液体类内容物
        return content.isIn(ContentCategories.BASE_LIQUID);
    }
}