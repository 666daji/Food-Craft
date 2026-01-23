package org.foodcraft.client.render.block.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.dfood.block.FoodBlock;
import org.foodcraft.block.PlateBlock;
import org.foodcraft.block.entity.PlateBlockEntity;
import org.foodcraft.client.render.model.ModModelLoader;
import org.foodcraft.client.render.model.PlatingModelManager;

public class PlateBlockEntityRenderer implements BlockEntityRenderer<PlateBlockEntity> {
    private final BakedModelManager modelManager;
    private final BlockModelRenderer modelRenderer;

    public PlateBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.modelManager = context.getRenderManager().getModels().getModelManager();
        this.modelRenderer = context.getRenderManager().getModelRenderer();
    }

    @Override
    public void render(PlateBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (entity.getCachedState().get(PlateBlock.IS_COVERED)) {
            return;
        }

        BlockState state = entity.getCachedState();
        Item item = state.getBlock().asItem();
        PlatingModelManager manager = PlatingModelManager.getInstance();
        Identifier renderModelId = manager.getModelForActions(item, entity.getPerformedActions());;

        if (entity.getOutcome() != null) {
            renderModelId = ModModelLoader.createDishesModel(item, entity.getOutcome());
        }

        // 获取模型
        BakedModel renderModel = modelManager.getModel(renderModelId);

        // 渲染最终模型
        if (renderModel != null) {
            matrices.push();
            matrices.translate(0.5, 0, 0.5);
            float facing = state.get(FoodBlock.FACING).asRotation();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing));
            matrices.translate(-0.5, 0, -0.5);

            modelRenderer.render(
                    entity.getWorld(),
                    renderModel,
                    state,
                    entity.getPos(),
                    matrices,
                    vertexConsumers.getBuffer(RenderLayer.getCutout()),
                    true,
                    Random.create(),
                    state.getRenderingSeed(entity.getPos()),
                    OverlayTexture.DEFAULT_UV
            );

            matrices.pop();
        }
    }
}
