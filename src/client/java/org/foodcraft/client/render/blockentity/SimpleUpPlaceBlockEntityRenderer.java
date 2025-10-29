package org.foodcraft.client.render.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.random.Random;
import org.foodcraft.block.UpPlaceBlock;
import org.foodcraft.block.entity.UpPlaceBlockEntity;

/**
 * 用于简单地渲染放置在{@link UpPlaceBlock}中的内容
 * @param <T> 对应的{@link UpPlaceBlock}的子类
 */
public abstract class SimpleUpPlaceBlockEntityRenderer <T extends UpPlaceBlockEntity> implements BlockEntityRenderer<T> {
    private final BlockRenderManager blockRenderManager;

    public SimpleUpPlaceBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.blockRenderManager = ctx.getRenderManager();
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState state = getInventoryBlockState(entity);

        if (state.getBlock() != Blocks.AIR){
            matrices.push();
            ApplyTransformations(matrices);
            if (state.getBlock() != Blocks.AIR){
                blockRenderManager.renderBlock(state, entity.getPos(), entity.getWorld(), matrices,
                        vertexConsumers.getBuffer(RenderLayers.getBlockLayer(state)), true, Random.create());
            }
            matrices.pop();
        }
    }

    /**
     * 获取内容物的方块状态
     * @param entity 对应的方块实体
     * @return 内容物方块状态
     */
    protected abstract BlockState getInventoryBlockState(T entity);

    /**
     * 应用变换
     * @param matrices 渲染使用的矩阵堆栈
     */
    protected abstract void ApplyTransformations(MatrixStack matrices);
}
