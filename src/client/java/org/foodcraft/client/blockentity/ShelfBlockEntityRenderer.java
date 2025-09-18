package org.foodcraft.client.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.Random;
import org.foodcraft.block.FlourSackBlock;
import org.foodcraft.block.ShelfBlock;
import org.foodcraft.block.entity.ShelfBlockEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

public class ShelfBlockEntityRenderer implements BlockEntityRenderer<ShelfBlockEntity> {
    private final BlockRenderManager blockRenderManager;

    public ShelfBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.blockRenderManager = ctx.getRenderManager();
    }

    @Override
    public void render(ShelfBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!entity.isEmpty()) {
            // 获取架子的朝向
            Direction facing = entity.getCachedState().get(ShelfBlock.FACING);
            // 渲染第一个槽位的物品（左侧）
            if (!entity.getStack(0).isEmpty()) {
                renderContent(entity, matrices, vertexConsumers, facing, 0);
            }
            // 渲染第二个槽位的物品（右侧）
            if (!entity.getStack(1).isEmpty()) {
                renderContent(entity, matrices, vertexConsumers, facing, 1);
            }
        }
    }

    /**
     * 渲染架子上的物品
     * @param entity 架子方块实体
     * @param matrices 矩阵栈
     * @param vertexConsumers 顶点消费者提供者
     * @param facing 架子朝向
     * @param slot 槽位 (0 = 左侧, 1 = 右侧)
     */
    private void renderContent(ShelfBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, Direction facing, int slot) {
        matrices.push();
        BlockState blockState = entity.getInventoryBlockState(slot);

        // 根据架子朝向调整位置
        applyFacingTransformation(matrices, facing, slot);

        // 抬高物品
        matrices.translate(0.0, 0.32, 0.0);

        // 针对不同物品的特殊处理
        if (blockState.getBlock() instanceof FlourSackBlock){
            matrices.scale(0.8f, 0.8f, 0.8f);
            matrices.translate(0.05, 0.0, 0.1);
        }

        // 渲染方块
        blockRenderManager.renderBlock(blockState, entity.getPos(), entity.getWorld(), matrices,
                vertexConsumers.getBuffer(RenderLayers.getBlockLayer(blockState)), false, Random.create());
        matrices.pop();
    }

    /**
     * 根据架子朝向和槽位应用变换
     * @param matrices 矩阵栈
     * @param facing 架子朝向
     * @param slot 槽位 (0 = 左侧, 1 = 右侧)
     */
    private void applyFacingTransformation(MatrixStack matrices, Direction facing, int slot) {
        // 先移动到架子中心
        matrices.translate(0.5, 0, 0.5);

        // 根据朝向旋转
        switch (facing) {
            case NORTH:
                // 默认朝向，不需要旋转
                break;
            case SOUTH:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                break;
            case EAST:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270));
                break;
            case WEST:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                break;
        }

        // 根据槽位应用偏移（左侧或右侧）
        float xOffset = slot == 0 ? -0.25f : 0.25f;
        matrices.translate(xOffset, 0, -0.20);

        // 移回原点
        matrices.translate(-0.5, 0, -0.5);
    }
}
