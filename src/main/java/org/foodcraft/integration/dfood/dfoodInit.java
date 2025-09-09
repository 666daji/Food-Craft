package org.foodcraft.integration.dfood;

import org.dfood.shape.Shapes;

public class dfoodInit {
    public static void init() {
        FoodBlocksModifier.FoodBlockAdd();
        AssistedBlocks.registerAssistedBlocks();

        // 注册形状
        Shapes.shapeMap.put("foodcraft:crippled_rabbit_stew",new int[][]{
                {1, 4, 8}
        });
        Shapes.shapeMap.put("foodcraft:crippled_mushroom_stew",new int[][]{
                {1, 4, 8}
        });
        Shapes.shapeMap.put("foodcraft:crippled_beetroot_soup",new int[][]{
                {1, 4, 8}
        });
        Shapes.shapeMap.put("foodcraft:crippled_suspicious_stew",new int[][]{
                {1, 4, 8}
        });
    }
}
