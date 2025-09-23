package org.foodcraft.registry;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.item.FlourItem;

public class ModItems {
    // 工作方块
    public static final Item BRACKET = registerItem(ModBlocks.BRACKET);
    public static final Item GRINDING_STONE = registerItem(ModBlocks.GRINDING_STONE);
    public static final Item HEAT_RESISTANT_SLATE = registerItem(ModBlocks.HEAT_RESISTANT_SLATE);

    // 工具
    public static final Item IRON_DISHES = registerItem(ModBlocks.IRON_DISHES);
    public static final Item WOODEN_SHELF = registerItem(ModBlocks.WOODEN_SHELF);

    // 粉尘
    public static final Item WHEAT_FLOUR = registerItem("wheat_flour", new FlourItem(new Item.Settings()));
    public static final Item LAPIS_LAZULI_FLOUR = registerItem("lapis_lazuli_flour", new FlourItem(new Item.Settings()));
    public static final Item COCOA_FLOUR = registerItem("cocoa_flour", new FlourItem(new Item.Settings()));
    public static final Item AMETHYST_FLOUR = registerItem("amethyst_flour", new FlourItem(new Item.Settings()));

    // 粉尘袋
    public static final Item FLOUR_SACK = registerItem("flour_sack", new Item(new Item.Settings().maxCount(1)));
    public static final Item WHEAT_FLOUR_SACK = registerItem(ModBlocks.WHEAT_FLOUR_SACK, new Item.Settings().maxCount(1));
    public static final Item LAPIS_LAZULI_FLOUR_SACK = registerItem(ModBlocks.LAPIS_LAZULI_FLOUR_SACK, new Item.Settings().maxCount(1));
    public static final Item COCOA_FLOUR_SACK = registerItem(ModBlocks.COCOA_FLOUR_SACK, new Item.Settings().maxCount(1));
    public static final Item AMETHYST_FLOUR_SACK = registerItem(ModBlocks.AMETHYST_FLOUR_SACK, new Item.Settings().maxCount(1));
    public static final Item SUGAR_SACK = registerItem(ModBlocks.SUGAR_SACK, new Item.Settings().maxCount(1));

    // 面食
    public static final Item DOUGH = registerItem(ModBlocks.DOUGH);
    public static final Item CAKE_EMBRYO = registerItem(ModBlocks.CAKE_EMBRYO);
    public static final Item CAKE_EMBRYO_MOLD = registerItem(ModBlocks.CAKE_EMBRYO_MOLD);
    public static final Item BAGUETTE = registerItem(ModBlocks.BAGUETTE);
    public static final Item BAGUETTE_EMBRYO = registerItem(ModBlocks.BAGUETTE_EMBRYO);

    // 调味料
    public static final Item SALT_SHAKER = registerItem(ModBlocks.SALT_SHAKER);

    public static Item registerItem(String name, Item item) {
        if (item instanceof BlockItem) {
            ((BlockItem)item).appendBlocks(Item.BLOCK_ITEMS, item);
        }
        return Registry.register(Registries.ITEM, new Identifier(FoodCraft.MOD_ID, name), item);
    }

    private static Item registerItem(Block block){
        return registerItem(block, new Item.Settings());
    }

    private static Item registerItem(Block block, Item.Settings settings){
        return registerItem(Registries.BLOCK.getId(block).getPath(), new BlockItem(block, settings));
    }

    public static void registerModItems() {}
}
