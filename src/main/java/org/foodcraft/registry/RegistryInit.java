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
        registerShapes();
    }

    private static void registerShapes() {
        Shapes.shapeMap.put("foodcraft:wheat_flour_sack",new int[][]{
                {1, 2, 8}
        });
        Shapes.shapeMap.put("foodcraft:lapis_lazuli_flour_sack",new int[][]{
                {1, 2, 8}
        });
        Shapes.shapeMap.put("foodcraft:cocoa_flour_sack",new int[][]{
                {1, 2, 8}
        });
        Shapes.shapeMap.put("foodcraft:amethyst_flour_sack",new int[][]{
                {1, 2, 8}
        });
        Shapes.shapeMap.put("foodcraft:sugar_sack",new int[][]{
                {1, 2, 8}
        });
    }
}
