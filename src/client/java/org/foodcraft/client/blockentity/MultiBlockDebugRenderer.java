package org.foodcraft.client.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.foodcraft.block.multi.*;

/**
 * 对应用了{@link MultiBlock}的方块添加显示方块堆信息的效果
 * <p>该渲染类会在方块的上方渲染出方块堆的信息便于调试</p>
 * @param <T> 对应的方块实体
 */
public abstract class MultiBlockDebugRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    protected final TextRenderer textRenderer;

    public MultiBlockDebugRenderer(BlockEntityRendererFactory.Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
    }

    /**
     * 获取方块的多方块引用
     * @param entity 对应的方块实体
     */
    protected abstract MultiBlockReference getReference(T entity);

    /**
     * 判断当前是否为调试模式
     * @param entity 对应的方块实体
     */
    protected abstract boolean isDebug(T entity);

    /**
     * 进行其他的调试渲染
     * @param entity 对应的方块实体
     * @param reference 对应的多方块引用
     * @param matrices 变换矩阵，注意，此时的矩阵已经被变换到了渲染文字的地方。
     *                 如果需要一个新的矩阵，请再对矩阵进行一次推送
     */
    protected abstract void otherDebugRender(T entity, MultiBlockReference reference,float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay);

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!isDebug(entity)){
            return;
        }

        // 获取多方块引用
        MultiBlockReference multiBlockRef = getReference(entity);
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

            otherDebugRender(entity, multiBlockRef, tickDelta, matrices, vertexConsumers, light, overlay);
        } finally {
            matrices.pop();
        }
    }
}
