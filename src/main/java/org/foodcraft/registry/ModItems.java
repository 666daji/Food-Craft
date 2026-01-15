package org.foodcraft.registry;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponents;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.dfood.item.HaveBlock;
import org.foodcraft.FoodCraft;
import org.foodcraft.food.ModFoodComponents;
import org.foodcraft.item.*;

import java.util.function.BiFunction;

public class ModItems {
    // 工作方块
    public static final Item BRACKET = registerItem(ModBlocks.BRACKET);
    public static final Item GRINDING_STONE = registerItem(ModBlocks.GRINDING_STONE);
    public static final Item HEAT_RESISTANT_SLATE = registerItem(ModBlocks.HEAT_RESISTANT_SLATE);
    public static final Item FIREWOOD = registerItem(ModBlocks.FIREWOOD);
    public static final Item POTTERY_TABLE = registerItem(ModBlocks.POTTERY_TABLE);
    public static final Item IRON_PLATE = registerItem(ModBlocks.IRON_PLATE, new Item.Settings(), DishesItem::new);

    // 工具
    public static final Item IRON_GARNISH_DISHES = registerItem(ModBlocks.IRON_GARNISH_DISHES);
    public static final Item WOODEN_SHELF = registerItem(ModBlocks.WOODEN_SHELF);
    public static final Item CUTTING_BOARD = registerItem(ModBlocks.CUTTING_BOARD);
    public static final Item IRON_POTS = registerItem(ModBlocks.IRON_POTS);
    public static final Item PLATE_LID = registerItem("plate_lid", new Item(new Item.Settings()));

    // 厨具
    public static final Item BREAD_SPATULA = registerItem(ModBlocks.BREAD_SPATULA, new Item.Settings(),
            ((block, settings) -> new ModSharpKitchenwareItem(block, settings, ModSharpKitchenwareItem.SpatulaMaterials.BREAD_SPATULA)));
    public static final Item KITCHEN_KNIFE = registerItem(ModBlocks.KITCHEN_KNIFE, new Item.Settings(),
            ((block, settings) -> new ModSharpKitchenwareItem(block, settings, ModSharpKitchenwareItem.SpatulaMaterials.KITCHEN_KNIFE)));

    // 粉尘
    public static final Item WHEAT_FLOUR = registerItem("wheat_flour",
            new FlourItem(new Item.Settings(), 0xFFF8E1, FlourItem.FlourType.WHEAT));
    public static final Item LAPIS_LAZULI_FLOUR = registerItem("lapis_lazuli_flour",
            new FlourItem(new Item.Settings(), 0x2666FF, FlourItem.FlourType.LAPIS_LAZULI));
    public static final Item COCOA_FLOUR = registerItem("cocoa_flour",
            new FlourItem(new Item.Settings(), 0x8B4513, FlourItem.FlourType.COCOA));
    public static final Item AMETHYST_FLOUR = registerItem("amethyst_flour",
            new FlourItem(new Item.Settings(), 0x8A2BE2, FlourItem.FlourType.AMETHYST));
    public static final Item SUGAR_FLOUR = registerItem("sugar_flour",
            new FlourItem(new Item.Settings(), 0xFFF5F5F0, FlourItem.FlourType.SUGAR));
    public static final Item SALT_FLOUR = registerItem("salt_flour",
            new FlourItem(new Item.Settings(), 0xFFFDFCF5, FlourItem.FlourType.SUGAR));

    // 粉尘袋
    public static final Item FLOUR_SACK = registerItem("flour_sack", new FlourSackItem(ModBlocks.FLOUR_SACK ,new Item.Settings().maxCount(1)));

    // 奶制品
    public static final Item MILK_POTION = registerItem(ModBlocks.MILK_POTION, new Item.Settings().food(ModFoodComponents.MILK).maxCount(16), FoodPotionItem::new);

    // 面食
    public static final Item DOUGH = registerItem(ModBlocks.DOUGH);
    public static final Item HARD_BREAD = registerItem(ModBlocks.HARD_BREAD, new Item.Settings().food(ModFoodComponents.HARD_BREAD));
    public static final Item SMALL_BREAD_EMBRYO = registerItem(ModBlocks.SMALL_BREAD_EMBRYO);
    public static final Item SMALL_BREAD = registerItem(ModBlocks.SMALL_BREAD, new Item.Settings().food(ModFoodComponents.SMALL_BREAD));
    public static final Item BAGUETTE = registerItem(ModBlocks.BAGUETTE, new Item.Settings().food(ModFoodComponents.BAGUETTE));
    public static final Item BAGUETTE_EMBRYO = registerItem(ModBlocks.BAGUETTE_EMBRYO);
    public static final Item TOAST_DOUGH = registerItem("toast_dough", new Item(new Item.Settings()));
    public static final Item TOAST_EMBRYO = registerItem(ModBlocks.TOAST_EMBRYO);
    public static final Item TOAST = registerItem(ModBlocks.TOAST, new Item.Settings().food(ModFoodComponents.TOAST));
    public static final Item CAKE_DOUGH = registerItem("cake_dough", new Item(new Item.Settings()));
    public static final Item CAKE_EMBRYO = registerItem(ModBlocks.CAKE_EMBRYO);
    public static final Item BAKED_CAKE_EMBRYO = registerItem(ModBlocks.BAKED_CAKE_EMBRYO);
    public static final Item HARD_BREAD_BOAT = registerItem(ModBlocks.HARD_BREAD_BOAT,
            new Item.Settings().food(ModFoodComponents.HARD_BREAD_BOAT), BreadBoatItem::new);
    public static final Item SALTY_DOUGH = registerItem(ModBlocks.SALTY_DOUGH);

    // 切片食物
    public static final Item CARROT_SLICES = registerItem(ModBlocks.CARROT_SLICES, new Item.Settings().food(ModFoodComponents.CARROT_SLICES));
    public static final Item CARROT_HEAD = registerItem("carrot_head", new Item(new Item.Settings().food(ModFoodComponents.CARROT_HEAD)));
    public static final Item POTATO_CUBES = registerItem(ModBlocks.POTATO_CUBES, new Item.Settings().food(FoodComponents.POTATO));
    public static final Item BAKED_POTATO_CUBES = registerItem(ModBlocks.BAKED_POTATO_CUBES, new Item.Settings().food(FoodComponents.BAKED_POTATO));
    public static final Item APPLE_SLICES = registerItem(ModBlocks.APPLE_SLICES, new Item.Settings().food(FoodComponents.APPLE));
    public static final Item COD_CUBES = registerItem(ModBlocks.COD_CUBES, new Item.Settings().food(ModFoodComponents.COD_CUBES));
    public static final Item COD_HEAD = registerItem("cod_head", new Item(new Item.Settings().food(ModFoodComponents.COD_HEAD)));
    public static final Item COOKED_COD_CUBES = registerItem(ModBlocks.COOKED_COD_CUBES, new Item.Settings().food(ModFoodComponents.COOKED_COD_CUBES));
    public static final Item COOKED_COD_HEAD = registerItem("cooked_cod_head", new Item(new Item.Settings().food(ModFoodComponents.COOKED_COD_HEAD)));
    public static final Item SALMON_CUBES = registerItem(ModBlocks.SALMON_CUBES, new Item.Settings().food(ModFoodComponents.SALMON_CUBES));
    public static final Item COOKED_SALMON_CUBES = registerItem(ModBlocks.COOKED_SALMON_CUBES, new Item.Settings().food(ModFoodComponents.COOKED_SALMON_CUBES));
    public static final Item KITCHEN_WASTE = registerItem("kitchen_waste", new Item(new Item.Settings()));

    // 模具
    public static final Item CAKE_DOUGH_CAKE_EMBRYO_MOLD = registerItem("cake_dough_cake_embryo_mold",
            new MoldContentItem(ModBlocks.CAKE_EMBRYO_MOLD, new Item.Settings(), CAKE_DOUGH));
    public static final Item TOAST_DOUGH_TOAST_EMBRYO_MOLD = registerItem("toast_dough_toast_embryo_mold",
            new MoldContentItem(ModBlocks.TOAST_EMBRYO_MOLD, new Item.Settings(), TOAST_DOUGH));
    public static final Item CAKE_EMBRYO_MOLD = registerItem(ModBlocks.CAKE_EMBRYO_MOLD);
    public static final Item TOAST_EMBRYO_MOLD = registerItem(ModBlocks.TOAST_EMBRYO_MOLD);

    // 调味料
    public static final Item SALT_CUBES = registerItem("salt_cubes", new Item(new Item.Settings()));
    public static final Item SALT_SHAKER = registerItem(ModBlocks.SALT_SHAKER);

    // 矿物
    public static final Item SALT_ORE = registerItem(ModBlocks.SALT_ORE);
    public static final Item DEEPSLATE_SALT_ORE = registerItem(ModBlocks.DEEPSLATE_SALT_ORE);

    // 陶制品
    public static final Item CLAY_POTS_EMBRYO = registerItem(ModBlocks.CLAY_POTS_EMBRYO);
    public static final Item CLAY_POTS = registerItem(ModBlocks.CLAY_POTS);
    public static final Item FLOWER_POT_EMBRYO = registerItem(ModBlocks.FLOWER_POT_EMBRYO);

    public static Item registerItem(String name, Item item) {
        if (item instanceof BlockItem blockItem) {
            blockItem.appendBlocks(Item.BLOCK_ITEMS, item);
        } else if (item instanceof HaveBlock haveBlock){
            haveBlock.appendBlocks(Item.BLOCK_ITEMS, item);
        }
        return Registry.register(Registries.ITEM, new Identifier(FoodCraft.MOD_ID, name), item);
    }

    private static Item registerItem(Block block){
        return registerItem(block, new Item.Settings());
    }

    private static Item registerItem(Block block, Item.Settings settings){
        return registerItem(block, settings, BlockItem::new);
    }

    private static Item registerItem(Block block, Item.Settings settings, BiFunction<Block, Item.Settings, Item> blockItemCreator){
        return registerItem(Registries.BLOCK.getId(block).getPath(), blockItemCreator.apply(block, settings));
    }

    public static void registerModItems() {}
}
