package org.foodcraft.fluidsystem.foodcraft;

import net.minecraft.item.FoodComponents;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.fluidsystem.content.AbstractContent;
import org.foodcraft.fluidsystem.content.SoupContent;

public class ModContents {
    // 汤
    public static final AbstractContent BEETROOT_SOUP = new SoupContent(new Identifier(FoodCraft.MOD_ID, "beetroot_soup"), FoodComponents.BEETROOT_SOUP);
    public static final AbstractContent MUSHROOM_STEW = new SoupContent(new Identifier(FoodCraft.MOD_ID, "mushroom_stew"), FoodComponents.MUSHROOM_STEW);
}
