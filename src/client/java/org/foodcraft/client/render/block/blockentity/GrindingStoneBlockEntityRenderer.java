package org.foodcraft.client.render.block.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.GrindingStoneBlock;
import org.foodcraft.block.entity.GrindingStoneBlockEntity;
import org.foodcraft.client.render.model.ModModelLayers;

public class GrindingStoneBlockEntityRenderer extends WithAnimationBlockEntityRenderer<GrindingStoneBlockEntity> {
    private static final Identifier TEXTURE = new Identifier(FoodCraft.MOD_ID, "textures/blockentity/grinding_stone.png");
    private static final float MODEL_Y_TRANSLATION = 1.5f;
    private static final float MODEL_X_ROTATION = 180.0f;
    private static final float ITEM_Y_BASE = 0.45f;
    private static final float ITEM_Y_OFFSET_FACTOR = 0.005f;
    private static final float ITEM_SCALE = 0.5f;
    private static final float ITEM_X_ROTATION = 90.0f;

    private final ModelPart base;
    private final ModelPart top;
    private final ModelPart handle;
    private final ItemRenderer itemRenderer;

    public GrindingStoneBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
        ModelPart root = ctx.getLayerModelPart(ModModelLayers.GRINDING_STONE);
        this.base = root.getChild("base");
        this.top = root.getChild("top");
        this.handle = root.getChild("handle");

        registerModelPart("top", top);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // 基础部分
        root.addChild("base", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-8.0F, -2.0F, -8.0F, 16.0F, 3.0F, 16.0F, new Dilation(0.0F))
                        .uv(56, 37).cuboid(-8.0F, -4.0F, 6.0F, 16.0F, 2.0F, 2.0F, new Dilation(0.0F))
                        .uv(56, 33).cuboid(-8.0F, -4.0F, -8.0F, 16.0F, 2.0F, 2.0F, new Dilation(0.0F))
                        .uv(42, 55).cuboid(6.0F, -4.0F, -6.0F, 2.0F, 2.0F, 12.0F, new Dilation(0.0F))
                        .uv(56, 19).cuboid(-8.0F, -4.0F, -6.0F, 2.0F, 2.0F, 12.0F, new Dilation(0.0F))
                        .uv(64, 0).cuboid(-1.5F, -6.0F, -1.5F, 3.0F, 4.0F, 3.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 23.0F, 0.0F));

        // 顶部部分
        root.addChild("top", ModelPartBuilder.create()
                        .uv(42, 69).cuboid(0.0F, -2.0F, -2.0F, 1.0F, 1.0F, 4.0F, new Dilation(0.0F))
                        .uv(64, 7).cuboid(5.0F, -2.0F, -2.0F, 2.0F, 1.0F, 4.0F, new Dilation(0.0F))
                        .uv(0, 38).cuboid(-7.0F, -1.0F, -7.0F, 14.0F, 3.0F, 14.0F, new Dilation(0.0F))
                        .uv(56, 41).cuboid(0.0F, -2.0F, -7.0F, 7.0F, 1.0F, 5.0F, new Dilation(0.0F))
                        .uv(64, 12).cuboid(6.9F, -1.3F, -1.0F, 5.0F, 1.0F, 2.0F, new Dilation(0.0F))
                        .uv(0, 55).cuboid(-7.0F, -2.0F, -7.0F, 7.0F, 1.0F, 14.0F, new Dilation(0.0F))
                        .uv(56, 47).cuboid(0.0F, -2.0F, 2.0F, 7.0F, 1.0F, 5.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 12.0F, 0.0F));

        // 手柄部分
        root.addChild("handle", ModelPartBuilder.create()
                        .uv(0, 19).cuboid(-7.0F, -10.0F, -7.0F, 14.0F, 4.5F, 14.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void render(GrindingStoneBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = entity.getWorld();

        // 普通渲染（无世界上下文）
        if (world == null) {
            renderStaticModel(matrices, vertexConsumers, light, overlay);
            return;
        }

        BlockState state = entity.getCachedState();
        ItemStack output = new ItemStack(entity.getExpectedOutput());

        // 管理动画状态
        manageAnimationState(entity, tickDelta, state);

        matrices.push();
        try {
            renderAnimatedModel(entity, tickDelta, matrices, vertexConsumers, light, overlay, state, output);
        } finally {
            matrices.pop();
        }
    }

    /**
     * 渲染静态模型（无世界上下文时使用）
     */
    private void renderStaticModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                   int light, int overlay) {
        matrices.push();
        resetAllModelParts();
        matrices.translate(0.5, MODEL_Y_TRANSLATION, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MODEL_X_ROTATION));

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
        base.render(matrices, vertexConsumer, light, overlay);
        top.render(matrices, vertexConsumer, light, overlay);
        handle.render(matrices, vertexConsumer, light, overlay);

        matrices.pop();
    }

    /**
     * 渲染带动画的模型
     */
    private void renderAnimatedModel(GrindingStoneBlockEntity entity, float tickDelta, MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers, int light, int overlay,
                                     BlockState state, ItemStack output) {
        matrices.translate(0.5, MODEL_Y_TRANSLATION, 0.5);
        float facing = state.get(GrindingStoneBlock.FACING).asRotation();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing + 90));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MODEL_X_ROTATION));

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
        base.render(matrices, vertexConsumer, light, overlay);
        top.render(matrices, vertexConsumer, light, overlay);
        handle.render(matrices, vertexConsumer, light, overlay);

        // 渲染产出物品
        if (!output.isEmpty()) {
            renderOutputItem(entity, tickDelta, matrices, vertexConsumers, light, overlay, output, state);
        }
    }

    /**
     * 管理动画状态的更新逻辑
     */
    private void manageAnimationState(GrindingStoneBlockEntity entity, float tickDelta, BlockState state) {
        // 更新动画状态
        if (entity.isGrinding()) {
            if (!entity.grindingAnimationState.isRunning()) {
                entity.grindingAnimationState.start(entity.getAge());
            }
        } else {
            entity.grindingAnimationState.stop();
        }

        // 应用动画
        applyAnimation(
                entity.grindingAnimationState,
                BlockAnimations.GRINDING_STONE_SPIN,
                getAnimationProgress(entity.getAge(), tickDelta),
                1.0F,
                1.0F
        );
    }

    /**
     * 渲染产出物品
     * 注意：这里需要先pop再push来重置矩阵状态，与原始代码保持一致
     */
    private void renderOutputItem(GrindingStoneBlockEntity entity, float tickDelta, MatrixStack matrices,
                                  VertexConsumerProvider vertexConsumers, int light, int overlay,
                                  ItemStack output, BlockState state) {
        matrices.pop();
        matrices.push();

        float grindingProgress = entity.getGrindingProgress();
        matrices.translate(0.5, grindingProgress * ITEM_Y_OFFSET_FACTOR + ITEM_Y_BASE, 0.5);
        matrices.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        float facing = state.get(GrindingStoneBlock.FACING).asRotation();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ITEM_X_ROTATION));

        World world = entity.getWorld();
        int seed = (int) entity.getPos().asLong();
        itemRenderer.renderItem(output, ModelTransformationMode.FIXED,
                light, overlay, matrices, vertexConsumers, world, seed);
    }
}