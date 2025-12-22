package org.foodcraft.client.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;

public class RenderUtils {
    /**
     * 根据粉尘物品获取对应的粉尘袋模型名称。
     * @apiNote 粉尘物品ID + "_sack" = 粉尘袋模型名称
     */
    public static String getFlourModelName(ItemStack flourStack) {
        // 获取粉尘物品的注册表ID
        Identifier itemId = Registries.ITEM.getId(flourStack.getItem());

        if (itemId.getNamespace().equals(FoodCraft.MOD_ID)) {
            // 直接在粉尘物品ID后添加"_sack"作为粉尘袋模型名称
            return itemId.getPath() + "_sack";
        }

        return null;
    }

    /**
     * 获取石板上的物品渲染
     */
    public static BakedModel getCookingModel(BlockState state, int foodValue, BakedModelManager manager) {
        // 根据食物值和方块状态生成唯一的模型标识符
        Identifier blockId = Registries.BLOCK.getId(state.getBlock());
        Identifier modelId = new Identifier(
                FoodCraft.MOD_ID,
                "other/" + blockId.getPath() + "_cooking_" + foodValue
        );

        // 从 BakedModelManager 获取自定义模型
        return manager.getModel(modelId);
    }

    /**
     * 获取切割模型
     * @param itemStack 正在切割的物品
     * @param cutCount 当前切割次数（从1开始）
     * @param manager BakedModelManager
     * @return 对应的切割模型，如果不存在则返回null
     */
    public static BakedModel getCuttingModel(ItemStack itemStack, int cutCount, BakedModelManager manager) {
        if (itemStack.isEmpty() || cutCount < 1) {
            return null;
        }

        // 获取物品的完整标识符
        Identifier itemId = Registries.ITEM.getId(itemStack.getItem());

        // 构建切割模型ID
        // 格式：foodcraft:process/cut_{namespace}_{itemPath}_{cutCount}
        String modelPath = String.format("cut_%s_%s_%d",
                itemId.getNamespace(), itemId.getPath(), cutCount);
        Identifier modelId = new Identifier(FoodCraft.MOD_ID, "process/" + modelPath);

        BakedModel model = manager.getModel(modelId);

        // 检查是否是有效模型（不是错误模型）
        if (model == manager.getMissingModel()) {
            return null;
        }

        return model;
    }

    /**
     * 检查指定物品和切割次数是否有对应的切割模型
     */
    public static boolean hasCuttingModel(ItemStack itemStack, int cutCount, BakedModelManager manager) {
        return getCuttingModel(itemStack, cutCount, manager) != null;
    }
}