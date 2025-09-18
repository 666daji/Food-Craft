package org.foodcraft.client.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.Random;
import org.foodcraft.block.entity.DishesBlockEntity;

public class DishesBlockEntityRenderer implements BlockEntityRenderer<DishesBlockEntity> {
    private final BlockRenderManager blockRenderManager;

    public DishesBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.blockRenderManager = ctx.getRenderManager();
    }

    @Override
    public void render(DishesBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!entity.isEmpty()) {
            matrices.push();
            BlockState blockState = entity.getInventoryBlockState();
            matrices.translate(0.0, 0.1, 0.0);
            blockRenderManager.renderBlock(blockState, entity.getPos(), entity.getWorld(), matrices,
                    vertexConsumers.getBuffer(RenderLayers.getBlockLayer(blockState)), true, Random.create());
            matrices.pop();
        }
    }
}
