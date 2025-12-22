package org.foodcraft.client.render.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.dfood.block.FoodBlock;
import org.foodcraft.block.entity.HeatResistantSlateBlockEntity;
import org.foodcraft.block.multi.MultiBlockReference;
import org.foodcraft.registry.ModBlocks;
import org.foodcraft.util.FoodCraftUtils;

public class HeatResistantSlateBlockEntityRenderer extends MultiBlockDebugRenderer<HeatResistantSlateBlockEntity> {
    private final BlockRenderManager blockRenderManager;
    private final ItemRenderer itemRenderer;

    public HeatResistantSlateBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
        this.blockRenderManager = ctx.getRenderManager();
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(HeatResistantSlateBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
        ItemStack otherStack = entity.getOtherStacks().get(0);

        if (!entity.isEmpty()) {
            matrices.push();
            BlockState blockState = entity.getInventoryBlockState();

            // 针对花盆的特殊处理
            if (blockState.getBlock() == Blocks.FLOWER_POT && entity.getStack(0).getCount() > 1){
                blockState = ModBlocks.FLOWER_POT_COOKING.getDefaultState()
                        .with(FoodBlock.FACING, entity.getResultDirection() == null ? Direction.EAST : entity.getResultDirection())
                        .with(FoodCraftUtils.getFoodBlockProperty(ModBlocks.FLOWER_POT_COOKING), entity.getStack(0).getCount());
            }

            matrices.translate(0.0, 0.125, 0.0);
            if (!otherStack.isEmpty()){
                matrices.translate(0.0, 0.125, 0.0);
            }

            // 渲染主要内容
            if (blockState.getBlock() != Blocks.AIR){
                blockRenderManager.renderBlock(blockState, entity.getPos(), entity.getWorld(), matrices,
                        vertexConsumers.getBuffer(RenderLayers.getBlockLayer(blockState)), true, Random.create());
            }else {
                matrices.translate(0.5, 0, 0.5);
                matrices.scale(0.7f, 0.7f, 0.7f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                if (entity.getResultDirection() != null) {
                    float facing = entity.getResultDirection().asRotation();
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing + 90));
                }
                itemRenderer.renderItem(entity.getStack(0), ModelTransformationMode.FIXED, light, overlay, matrices,
                        vertexConsumers, entity.getWorld(), 0);
            }
            matrices.pop();
        }

        // 渲染叠加内容
        if (!otherStack.isEmpty()){
            matrices.push();
            BlockState blockState = entity.getOtherBlockState();
            matrices.translate(0.0, 0.125, 0.0);

            if (blockState.getBlock() != Blocks.AIR){
                blockRenderManager.renderBlock(blockState, entity.getPos(), entity.getWorld(), matrices,
                        vertexConsumers.getBuffer(RenderLayers.getBlockLayer(blockState)), true, Random.create());
            }
            matrices.pop();
        }
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