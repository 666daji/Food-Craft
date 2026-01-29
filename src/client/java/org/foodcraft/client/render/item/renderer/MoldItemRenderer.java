package org.foodcraft.client.render.item.renderer;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.foodcraft.client.render.model.ModModelLoader;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.ShapedDoughContent;

public class MoldItemRenderer {
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

    public static void renderMold(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                                  VertexConsumerProvider vertexConsumers, int light, int overlay) {

        if (!(stack.getItem() instanceof BlockItem blockItem)) return;

        BlockRenderManager blockRenderer = CLIENT.getBlockRenderManager();
        BakedModelManager manager = CLIENT.getBakedModelManager();

        BlockState state = blockItem.getBlock().getDefaultState();
        AbstractContent content = ContainerUtil.extractContent(stack);

        // 渲染方块本身
        blockRenderer.renderBlockAsEntity(state, matrices, vertexConsumers, light, overlay);

        // 如果有定型面团内容，渲染它
        if (content instanceof ShapedDoughContent shapedDough) {
            BakedModel model = manager.getModel(ModModelLoader.createShapedDoughModel(shapedDough));
            blockRenderer.getModelRenderer().render(matrices.peek(),
                    vertexConsumers.getBuffer(RenderLayers.getBlockLayer(state)), state, model, 1, 1, 1, light, overlay);
        }
    }
}