package org.foodcraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.foodcraft.block.multi.MultiBlockManager;
import org.foodcraft.registry.*;
import org.foodcraft.integration.dfood.dfoodInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FoodCraft implements ModInitializer {
    public static final String MOD_ID = "foodcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        dfoodInit.init();
        RegistryInit.init();
        multiBlockInit();
        ModOreGeneration.registerOres();
        LOGGER.info("FoodCraft mod is initializing");
    }

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
}
