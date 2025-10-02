package org.foodcraft.client.blockentity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.foodcraft.block.entity.HeatResistantSlateBlockEntity;
import org.foodcraft.block.multi.MultiBlockReference;

public class HeatResistantSlateBlockEntityRenderer implements BlockEntityRenderer<HeatResistantSlateBlockEntity> {
    private final TextRenderer textRenderer;

    public HeatResistantSlateBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
    }

    @Override
    public void render(HeatResistantSlateBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // 获取多方块引用
        MultiBlockReference multiBlockRef = entity.getMultiBlockReference();
        if (multiBlockRef == null || multiBlockRef.isDisposed()) {
            return;
        }

        // 获取坐标信息
        BlockPos masterPos = multiBlockRef.getMasterWorldPos();
        BlockPos relativePos = multiBlockRef.getRelativePos();
        BlockPos currentPos = entity.getPos();

        // 构建显示的文本
        String masterText = String.format("Master: %d,%d,%d", masterPos.getX(), masterPos.getY(), masterPos.getZ());
        String relativeText = String.format("Relative: %d,%d,%d", relativePos.getX(), relativePos.getY(), relativePos.getZ());
        String currentText = String.format("Current: %d,%d,%d", currentPos.getX(), currentPos.getY(), currentPos.getZ());

        // 检查是否为master方块
        boolean isMaster = multiBlockRef.isMasterBlock();
        String masterStatus = isMaster ? "MASTER" : "SLAVE";

        // 检查方块堆完整性
        boolean isIntact = multiBlockRef.checkIntegrity();
        String integrityStatus = isIntact ? "INTACT" : "BROKEN";

        matrices.push();

        try {
            // 将坐标系移动到方块中心上方
            matrices.translate(0.5, 1.2, 0.5);

            // 让文本始终面向相机（billboard效果）
            matrices.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation().rotationY(180));

            // 缩放文本，使其不会太大
            float scale = 0.02F;
            matrices.scale(scale, -scale, scale);

            // 渲染文本
            int white = 0xFFFFFFFF;  // 添加Alpha通道
            int green = 0xFF00FF00;
            int red = 0xFFFF0000;
            int yellow = 0xFFFFFF00;
            int blue = 0xFF0088FF;

            // 计算文本宽度用于居中
            int masterWidth = textRenderer.getWidth(masterText);
            int relativeWidth = textRenderer.getWidth(relativeText);
            int currentWidth = textRenderer.getWidth(currentText);
            int statusWidth = textRenderer.getWidth(masterStatus);
            int integrityWidth = textRenderer.getWidth(integrityStatus);

            int maxWidth = Math.max(Math.max(masterWidth, relativeWidth),
                    Math.max(currentWidth, Math.max(statusWidth, integrityWidth)));

            // 获取位置矩阵
            var positionMatrix = matrices.peek().getPositionMatrix();

            // 渲染主方块坐标（蓝色）
            textRenderer.draw(
                    masterText,
                    -masterWidth / 2f, -40,
                    blue,
                    false,
                    positionMatrix,
                    vertexConsumers,
                    TextRenderer.TextLayerType.POLYGON_OFFSET,
                    0,
                    light
            );

            // 渲染相对坐标（黄色）
            textRenderer.draw(
                    relativeText,
                    -relativeWidth / 2f, -30,
                    yellow,
                    false,
                    positionMatrix,
                    vertexConsumers,
                    TextRenderer.TextLayerType.POLYGON_OFFSET,
                    1,
                    light
            );

            // 渲染当前坐标（白色）
            textRenderer.draw(
                    currentText,
                    -currentWidth / 2f, -20,
                    white,
                    false,
                    positionMatrix,
                    vertexConsumers,
                    TextRenderer.TextLayerType.POLYGON_OFFSET,
                    0,
                    light
            );

            // 渲染主方块状态（绿色表示主方块，白色表示从方块）
            int masterColor = isMaster ? green : white;
            textRenderer.draw(
                    masterStatus,
                    -statusWidth / 2f, -10,
                    masterColor,
                    false,
                    positionMatrix,
                    vertexConsumers,
                    TextRenderer.TextLayerType.POLYGON_OFFSET,
                    0,
                    light
            );

            // 渲染完整性状态（绿色表示完整，红色表示损坏）
            int integrityColor = isIntact ? green : red;
            textRenderer.draw(
                    integrityStatus,
                    -integrityWidth / 2f, 0,
                    integrityColor,
                    false,
                    positionMatrix,
                    vertexConsumers,
                    TextRenderer.TextLayerType.POLYGON_OFFSET,
                    0,
                    light
            );

            // 如果炉子结构有效，显示额外信息
            if (entity.isStoveValid()) {
                String stoveType = "Stove: " + entity.getCurrentStoveStructureType();
                int stoveWidth = textRenderer.getWidth(stoveType);
                textRenderer.draw(
                        stoveType,
                        -stoveWidth / 2f, 10,
                        green,
                        false,
                        positionMatrix,
                        vertexConsumers,
                        TextRenderer.TextLayerType.POLYGON_OFFSET,
                        0,
                        light
                );
            }

        } finally {
            matrices.pop();
        }
    }
}