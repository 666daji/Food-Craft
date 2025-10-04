package org.foodcraft.client.blockentity;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.foodcraft.block.entity.HeatResistantSlateBlockEntity;
import org.foodcraft.block.multi.MultiBlockReference;

public class HeatResistantSlateBlockEntityRenderer extends MultiBlockDebugRenderer<HeatResistantSlateBlockEntity> {

    public HeatResistantSlateBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    protected MultiBlockReference getReference(HeatResistantSlateBlockEntity entity) {
        return entity.getMultiBlockReference();
    }

    @Override
    protected boolean isDebug(HeatResistantSlateBlockEntity entity) {
        return false;
    }

    @Override
    protected void otherDebugRender(HeatResistantSlateBlockEntity entity, MultiBlockReference reference, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // 如果炉子结构有效，显示额外信息
        if (entity.isStoveValid()) {
            String stoveType = "Stove: " + entity.getCurrentStoveStructureType();
            int stoveWidth = textRenderer.getWidth(stoveType);
            textRenderer.draw(
                    stoveType,
                    -stoveWidth / 2f, 10,
                    0xFF00FF00,
                    false,
                    matrices.peek().getPositionMatrix(),
                    vertexConsumers,
                    TextRenderer.TextLayerType.POLYGON_OFFSET,
                    0,
                    light
            );
        }
    }
}