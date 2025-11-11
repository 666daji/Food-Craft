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
}
