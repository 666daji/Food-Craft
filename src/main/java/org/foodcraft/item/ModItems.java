package org.foodcraft.item;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.ModBlocks;

public class ModItems {
    public static final Item BRACKET = registerItem("bracket_block",
            new BlockItem(ModBlocks.BRACKET, new Item.Settings()));
    public static final Item GRINDING_STONE = registerItem("grinding_stone",
            new BlockItem(ModBlocks.GRINDING_STONE, new Item.Settings()));
    public static final Item WHEAT_FLOUR = registerItem("wheat_flour",
            new Item(new Item.Settings()));

    public static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(FoodCraft.MOD_ID, name), item);
    }

    public static void registerModItems() {

    }
}
