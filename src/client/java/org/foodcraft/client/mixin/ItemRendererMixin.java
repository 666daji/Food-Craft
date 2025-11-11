package org.foodcraft.client.mixin;

import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.client.util.RenderUtils;
import org.foodcraft.item.FlourSackItem;
import org.spongepowered.asm.mixin.Mixin;
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
                String flourName = RenderUtils.getFlourModelName(flourStack);

                if (flourName != null) {
                    // 使用 MOD_ID 常量创建自定义模型标识符
                    ModelIdentifier customModelId = new ModelIdentifier(
                            new Identifier(FoodCraft.MOD_ID, flourName),
                            "inventory"
                    );

                    // 获取模型管理器
                    BakedModelManager manager = ((ItemRendererAccessor) this).getModel().getModelManager();
                    BakedModel customModel = manager.getModel(customModelId);

                    // 如果找到了自定义模型且不是缺失模型，则返回自定义模型
                    if (customModel != null && !customModel.equals(manager.getMissingModel())) {
                        return customModel;
                    }
                }
            }
        }

        // 返回原始模型
        return originalModel;
    }
}