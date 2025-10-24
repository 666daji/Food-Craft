package org.foodcraft.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.item.FlourSackItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BakedModel renderFlourItem(BakedModel originalModel, ItemStack stack) {
        // 检查是否为粉尘袋物品
        if (stack.getItem() instanceof FlourSackItem) {
            // 获取粉尘袋中的内容物
            Optional<ItemStack> content = FlourSackItem.getFirstBundledStack(stack);

            if (content.isPresent()) {
                ItemStack flourStack = content.get();
                String flourName = getFlourModelName(flourStack);

                if (flourName != null) {
                    // 使用 MOD_ID 常量创建自定义模型标识符
                    ModelIdentifier customModelId = new ModelIdentifier(
                            new Identifier(FoodCraft.MOD_ID, flourName),
                            "inventory"
                    );

                    // 获取模型管理器
                    MinecraftClient client = MinecraftClient.getInstance();
                    BakedModel customModel = ((ItemRendererAccessor) this).getModel().getModelManager().getModel(customModelId);

                    // 如果找到了自定义模型且不是缺失模型，则返回自定义模型
                    if (customModel != null && !customModel.equals(client.getBakedModelManager().getMissingModel())) {
                        return customModel;
                    }
                }
            }
        }

        // 返回原始模型
        return originalModel;
    }

    /**
     * 根据粉尘物品获取对应的粉尘袋模型名称
     * 规则：粉尘物品ID + "_sack" = 粉尘袋模型名称
     */
    @Unique
    private String getFlourModelName(ItemStack flourStack) {
        // 获取粉尘物品的注册表ID
        Identifier itemId = Registries.ITEM.getId(flourStack.getItem());

        // 只处理本mod的粉尘物品
        if (itemId.getNamespace().equals(FoodCraft.MOD_ID)) {
            // 直接在粉尘物品ID后添加"_sack"作为粉尘袋模型名称
            return itemId.getPath() + "_sack";
        }

        return null;
    }
}