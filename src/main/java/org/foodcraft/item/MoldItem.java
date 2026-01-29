package org.foodcraft.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.ShapedDoughContent;

public class MoldItem extends BlockItem {

    public MoldItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        if (ContainerUtil.extractContent(stack) != null) {
            return super.getTranslationKey() + ".dough";
        }

        return super.getTranslationKey(stack);
    }

    @Override
    public Text getName(ItemStack stack) {
        AbstractContent content = ContainerUtil.extractContent(stack);

        if (content instanceof ShapedDoughContent shapedDough) {
            Text doughName = shapedDough.getDisplayName();
            return Text.translatable(this.getTranslationKey(stack), doughName);
        }

        return super.getName(stack);
    }
}
