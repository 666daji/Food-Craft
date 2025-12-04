package org.foodcraft.registry;

import org.dfood.shape.Shapes;

public class RegistryInit {
    public static void init() {
        ModBlocks.registerModBlocks();
        ModItems.registerModItems();
        ModBlockEntityTypes.registerBlockEntityTypes();
        ModEntityTypes.registerModEntityTypes();
        ModRecipeTypes.initialize();
        ModItemGroups.RegistryModItemGroups();
        ModSounds.initialize();
        ModScreenHandlerTypes.registerScreenHandlerTypes();
        registerShapes();
    }

    private static void registerShapes() {
        Shapes.shapeMap.put("foodcraft:flour_sack",new int[][]{
                {1, 2, 8}
        });

        // 奶制品
        Shapes.shapeMap.put("foodcraft:milk_potion",new int[][]{
                {1, 2, 8}, {3, 3, 1}
        });

        // 面包
        Shapes.shapeMap.put("foodcraft:small_bread_embryo",new int[][]{
                {1, 1, 12}
        });
        Shapes.shapeMap.put("foodcraft:small_bread",new int[][]{
                {1, 1, 12}
        });
        Shapes.shapeMap.put("foodcraft:baguette_embryo",new int[][]{
                {1, 1, 2}
        });
        Shapes.shapeMap.put("foodcraft:baguette",new int[][]{
                {1, 1, 2}
        });
        Shapes.shapeMap.put("foodcraft:hard_bread",new int[][]{
                {1, 1, 8}
        });
        Shapes.shapeMap.put("foodcraft:dough",new int[][]{
                {1, 1, 12}
        });
        Shapes.shapeMap.put("foodcraft:cake_embryo",new int[][]{
                {1, 1, 1}
        });
        Shapes.shapeMap.put("foodcraft:hard_bread_boat",new int[][]{
                {1, 1, 8}
        });
        Shapes.shapeMap.put("foodcraft:mushroom_stew_hard_bread_boat",new int[][]{
                {1, 1, 8}
        });
        Shapes.shapeMap.put("foodcraft:beetroot_soup_hard_bread_boat",new int[][]{
                {1, 1, 8}
        });

        //其他
        Shapes.shapeMap.put("foodcraft:firewood",new int[][]{
                {1, 1, 1},{2, 2, 2},{3, 3, 3},{4, 4, 4},{5, 6, 5}
        });
    }
}
