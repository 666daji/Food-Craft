package org.foodcraft.contentsystem.occupy;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.registry.ContentRegistry;
import org.foodcraft.registry.ModItems;
import org.jetbrains.annotations.Nullable;

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

    /**
     * 从占位物品堆栈中获取内容物。
     *
     * @param stack 要解析的物品堆栈
     * @return 获取到的内容物，如果无法获取则返回null
     */
    @Nullable
    public static AbstractContent getContentFromOccupy(ItemStack stack) {
        if (stack.getNbt() != null) {
            Identifier id = Identifier.tryParse(stack.getNbt().getString(OCCUPY_KEY));
            return ContentRegistry.get(id);
        }

        return null;
    }

    /**
     * 检查一个物品堆栈是否是占位内容物。
     *
     * @param stack 要检查的物品堆栈
     * @return 是否是占位
     */
    public static boolean isOccupy(ItemStack stack) {
        return stack.isOf(ModItems.MILK_POTION)
                && stack.getCount() == 6 && stack.getNbt() != null && stack.getNbt().contains(OCCUPY_KEY);
    }
}
