package org.foodcraft.client.render.model;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.Block;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.dfood.block.FoodBlocks;
import org.foodcraft.FoodCraft;
import org.foodcraft.item.FlourItem;
import org.foodcraft.registry.ModBlocks;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class FoodCraftModelLoader implements ModelLoadingPlugin {
    private static final Logger LOGGER = FoodCraft.LOGGER;
    /**
     * 存储所有需要加载的模型标识符
     */
    private static final List<Identifier> MODELS_TO_LOAD = new ArrayList<>();

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        MODELS_TO_LOAD.clear();

        // 注册所有模型
        registerAllFlourSackModels();
        registerAllCookingModels();
        registerDoughKneadingModel();

        // 将所有模型添加到加载上下文
        pluginContext.addModels(MODELS_TO_LOAD.toArray(new Identifier[0]));
    }

    // =========== 食物烘烤模型 ===========

    /**
     * 注册所有需要加载烘烤模型的食物方块
     */
    private void registerAllCookingModels() {
        registerCookingModelsForBlock(FoodBlocks.POTATO, 4);
        registerCookingModelsForBlock(FoodBlocks.BAKED_POTATO, 4);
        registerCookingModelsForBlock(FoodBlocks.BEEF, 2);
        registerCookingModelsForBlock(FoodBlocks.COOKED_BEEF, 2);
        registerCookingModelsForBlock(FoodBlocks.MUTTON, 2);
        registerCookingModelsForBlock(FoodBlocks.COOKED_MUTTON, 2);
        registerCookingModelsForBlock(FoodBlocks.PORKCHOP, 2);
        registerCookingModelsForBlock(FoodBlocks.COOKED_PORKCHOP, 2);
        registerCookingModelsForBlock(ModBlocks.FLOWER_POT_EMBRYO, 4);
    }

    /**
     * 为指定的FoodBlock注册所有烘烤模型
     * @param block FoodBlock实例
     * @param maxFood 最大食物数量
     */
    public static void registerCookingModelsForBlock(Block block, int maxFood) {
        if (maxFood < 2) {
            LOGGER.warn("Max food value {} is less than 2 for block {}, skipping cooking models",
                    maxFood, Registries.BLOCK.getId(block));
            return;
        }

        String blockPath = Registries.BLOCK.getId(block).getPath();

        for (int foodValue = 2; foodValue <= maxFood; foodValue++) {
            Identifier modelId = createCookingModel(blockPath, foodValue);
            MODELS_TO_LOAD.add(modelId);
            LOGGER.debug("Registered cooking model: {} for food value {}",
                    modelId, foodValue);
        }
    }

    /**
     * 创建烘烤食物模型的标识符
     */
    public static Identifier createCookingModel(String blockPath, int foodValue) {
        String modelPath = blockPath + "_cooking_" + foodValue;
        return new Identifier(FoodCraft.MOD_ID, "other/" + modelPath);
    }

    // =========== 粉尘袋模型 ===========

    /**
     * 注册所有粉尘袋模型
     */
    private void registerAllFlourSackModels() {
        for (FlourItem flourItem : FlourItem.FLOURS) {
            Identifier itemId = Registries.ITEM.getId(flourItem);

            String flourSackModelName = itemId.getPath() + "_sack";
            ModelIdentifier modelId = createItemModel(flourSackModelName);
            MODELS_TO_LOAD.add(modelId);
            LOGGER.debug("Dynamically registered flour sack model: {}", modelId);
        }
    }

    // =========== 揉面流程 ===========

    public static void registerDoughKneadingModel() {
        MODELS_TO_LOAD.add(createProcessModel("knead_add_flour_1"));
        MODELS_TO_LOAD.add(createProcessModel("knead_add_flour_2"));
        MODELS_TO_LOAD.add(createProcessModel("knead_add_flour_3"));

        MODELS_TO_LOAD.add(createProcessModel("knead_add_liquid_1"));
        MODELS_TO_LOAD.add(createProcessModel("knead_add_liquid_2"));
        MODELS_TO_LOAD.add(createProcessModel("knead_add_liquid_3"));

        MODELS_TO_LOAD.add(createProcessModel("knead_add_extra_1"));

        MODELS_TO_LOAD.add(createProcessModel("knead_knead_1"));
        MODELS_TO_LOAD.add(createProcessModel("knead_knead_2"));
        MODELS_TO_LOAD.add(createProcessModel("knead_knead_3"));
    }

    // =========== 辅助方法 ===========

    /**
     * 创建物品模型的标识符
     */
    public static ModelIdentifier createItemModel(String itemPath) {
        return new ModelIdentifier(new Identifier(FoodCraft.MOD_ID, itemPath), "inventory");
    }

    /**
     * 创建步骤所需的额外模型的标识符
     */
    public static Identifier createProcessModel(String blockPath) {
        return new Identifier(FoodCraft.MOD_ID, "process/" + blockPath);
    }

    /**
     * 获取已注册的所有模型
     */
    public static List<Identifier> getRegisteredModels() {
        return new ArrayList<>(MODELS_TO_LOAD);
    }
}