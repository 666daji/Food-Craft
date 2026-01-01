package org.foodcraft.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.foodcraft.client.render.item.ReplaceItemModel;
import org.foodcraft.client.render.item.ReplaceItemModel.ReplaceContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Shadow @Final private ItemModels models;
    @Shadow @Final private MinecraftClient client;

    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private BakedModel renderFlourItem(
            BakedModel originalModel,
            ItemStack stack,
            ModelTransformationMode renderMode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            BakedModel model) {

        if (stack == null || stack.isEmpty()) {
            return originalModel;
        }

        // 跳过三叉戟和望远镜的原版特殊处理
        if (stack.isOf(Items.TRIDENT) || stack.isOf(Items.SPYGLASS)) {
            return originalModel;
        }

        ReplaceItemModel replacer = ReplaceItemModel.getReplace(stack.getItem());
        if (replacer != null) {
            ReplaceContext context = new ReplaceContext(
                    stack,
                    renderMode,
                    leftHanded,
                    originalModel,
                    this.models.getModelManager(),
                    this.client.world,
                    matrices,
                    vertexConsumers,
                    light,
                    overlay
            );
            BakedModel replacedModel = replacer.ReplaceModel(context);
            return replacedModel != null ? replacedModel : originalModel;
        }

        return originalModel;
    }
}