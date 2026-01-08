package org.foodcraft.registry;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.dfood.shape.Shapes;
import org.foodcraft.block.multi.MultiBlockManager;
import org.foodcraft.contentsystem.foodcraft.ModContainers;
import org.foodcraft.contentsystem.foodcraft.ModContents;

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
        ModOreGeneration.registerOres();
        ModContents.registryContents();
        ModContainers.registryContainers();
        multiBlockInit();
        registerShapes();
    }

    /**
     * 多方块初始化
     */
    private static void multiBlockInit(){
        // 世界加载时恢复多方块数据
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!world.isClient()) {
                MultiBlockManager.loadWorldMultiBlocks(world);
            }
        });
        // 服务器停止时清理
        ServerLifecycleEvents.SERVER_STOPPING.register(MultiBlockManager::onServerStopping);
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

        // 陶艺品胚
        Shapes.shapeMap.put("foodcraft:flower_pot_embryo",new int[][]{
                {1, 1, 7}, {2, 4, 1}
        });
    }
}
