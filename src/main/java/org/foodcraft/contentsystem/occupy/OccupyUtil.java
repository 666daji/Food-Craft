package org.foodcraft.contentsystem.occupy;

import net.minecraft.item.ItemStack;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.registry.ModItems;

/**
 * <p>占位堆栈是用于适应原版的</p>
 */
public class OccupyUtil {
    public static final String OCCUPY_KEY = "o_content";

    public static ItemStack createAbstractOccupy(AbstractContent content) {
        ItemStack result = new ItemStack(ModItems.MILK_POTION, 6);
        result.getOrCreateNbt().putString(OCCUPY_KEY, content.getId().toString());

        return result;
    }
}
