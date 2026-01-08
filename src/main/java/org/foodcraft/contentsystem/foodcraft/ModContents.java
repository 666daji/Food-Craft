package org.foodcraft.contentsystem.foodcraft;

import net.minecraft.item.FoodComponents;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.BaseLiquidContent;
import org.foodcraft.contentsystem.content.SoupContent;

public class ModContents {
    // 汤
    public static final SoupContent MUSHROOM_STEW = new SoupContent(
            new Identifier(FoodCraft.MOD_ID, "mushroom_stew"),
            FoodComponents.MUSHROOM_STEW
    );

    public static final SoupContent BEETROOT_SOUP = new SoupContent(
            new Identifier(FoodCraft.MOD_ID, "beetroot_soup"),
            FoodComponents.BEETROOT_SOUP
    );

    public static final SoupContent RABBIT_STEW = new SoupContent(
            new Identifier(FoodCraft.MOD_ID, "rabbit_stew"),
            FoodComponents.RABBIT_STEW
    );

    // 基础液体
    public static final AbstractContent WATER = new BaseLiquidContent(
            new Identifier(FoodCraft.MOD_ID, "water")
    );

    public static final AbstractContent MILK = new BaseLiquidContent(
            new Identifier(FoodCraft.MOD_ID, "milk")
    );

    public static void registryContents() {}
}