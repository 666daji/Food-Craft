package org.foodcraft.client.render.block.blockentity;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.foodcraft.block.entity.MoldBlockEntity;

public class MoldBlockEntityRenderer extends UpPlaceBlockEntityRenderer<MoldBlockEntity> {
    public MoldBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(MoldBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {

    }
}
