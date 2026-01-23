package org.foodcraft.contentsystem.container;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.ContentCategories;

/**
 * 碗容器类型。
 * <p>
 * 用于承载汤类内容物，如蘑菇煲、甜菜汤等。
 * </p>
 */
public class BowlContainer extends AbstractMappedContainer {
    public BowlContainer(Identifier id, ContainerSettings settings) {
        super(id, settings);
    }

    @Override
    public boolean matches(ItemStack stack) {
        // 检查是否是碗或已知的汤类物品
        return stack.isOf(getEmptyItem()) || supportsItem(stack.getItem());
    }

    @Override
    public boolean canContain(AbstractContent content) {
        // 碗可以装汤类内容物
        return content.isIn(ContentCategories.SOUP);
    }
}