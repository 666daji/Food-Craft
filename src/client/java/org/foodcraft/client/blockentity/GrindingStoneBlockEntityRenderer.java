package org.foodcraft.client.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
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
import org.foodcraft.registry.ModBlocks;
import org.foodcraft.block.entity.GrindingStoneBlockEntity;
import org.foodcraft.client.entity.ModModelLayers;

public class GrindingStoneBlockEntityRenderer extends WithAnimationBlockEntityRenderer<GrindingStoneBlockEntity> {
    private final ModelPart root;
    private final ModelPart base;
    private final ModelPart top;
    private final ModelPart handle;
    private static final Identifier TEXTURE = new Identifier(FoodCraft.MOD_ID, "textures/blockentity/grinding_stone.png");
    private final ItemRenderer itemRenderer;

    public GrindingStoneBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
        this.root = ctx.getLayerModelPart(ModModelLayers.GRINDING_STONE);
        this.base = root.getChild("base");
        this.top = root.getChild("top");
        this.handle = root.getChild("handle");

        // 填充模型部件映射
        modelParts.put("top", top);

        // 保存模型部件的初始状态
        saveInitialState("top", top);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        // 基础部分 (对应原 bone)
        ModelPartData base = modelPartData.addChild("base", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-8.0F, -2.0F, -8.0F, 16.0F, 3.0F, 16.0F, new Dilation(0.0F))
                        .uv(56, 37).cuboid(-8.0F, -4.0F, 6.0F, 16.0F, 2.0F, 2.0F, new Dilation(0.0F))
                        .uv(56, 33).cuboid(-8.0F, -4.0F, -8.0F, 16.0F, 2.0F, 2.0F, new Dilation(0.0F))
                        .uv(42, 55).cuboid(6.0F, -4.0F, -6.0F, 2.0F, 2.0F, 12.0F, new Dilation(0.0F))
                        .uv(56, 19).cuboid(-8.0F, -4.0F, -6.0F, 2.0F, 2.0F, 12.0F, new Dilation(0.0F))
                        .uv(64, 0).cuboid(-1.5F, -6.0F, -1.5F, 3.0F, 4.0F, 3.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 23.0F, 0.0F));

        // 顶部部分 (对应原 bone2)
        ModelPartData top = modelPartData.addChild("top", ModelPartBuilder.create()
                        .uv(42, 69).cuboid(0.0F, -2.0F, -2.0F, 1.0F, 1.0F, 4.0F, new Dilation(0.0F))
                        .uv(64, 7).cuboid(5.0F, -2.0F, -2.0F, 2.0F, 1.0F, 4.0F, new Dilation(0.0F))
                        .uv(0, 38).cuboid(-7.0F, -1.0F, -7.0F, 14.0F, 3.0F, 14.0F, new Dilation(0.0F))
                        .uv(56, 41).cuboid(0.0F, -2.0F, -7.0F, 7.0F, 1.0F, 5.0F, new Dilation(0.0F))
                        .uv(64, 12).cuboid(6.9F, -1.3F, -1.0F, 5.0F, 1.0F, 2.0F, new Dilation(0.0F))
                        .uv(0, 55).cuboid(-7.0F, -2.0F, -7.0F, 7.0F, 1.0F, 14.0F, new Dilation(0.0F))
                        .uv(56, 47).cuboid(0.0F, -2.0F, 2.0F, 7.0F, 1.0F, 5.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 12.0F, 0.0F));

        // 手柄部分 (对应原 bone3)
        ModelPartData handle = modelPartData.addChild("handle", ModelPartBuilder.create()
                        .uv(0, 19).cuboid(-7.0F, -10.0F, -7.0F, 14.0F, 4.5F, 14.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void render(GrindingStoneBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        PartState savedState = new PartState(top);
        int seed = (int)entity.getPos().asLong();
        World world = entity.getWorld();
        ItemStack output = new ItemStack(entity.getExpectedOutput());
        BlockState state = world != null?
                entity.getCachedState():
                ModBlocks.GRINDING_STONE.getDefaultState();

        try {
            // 重置模型部件到初始状态
            resetPart("top", top);
            // 更新动画状态
            if (entity.isGrinding()) {
                if (!entity.grindingAnimationState.isRunning()) {
                    entity.grindingAnimationState.start(entity.getAge());
                }
            } else {
                entity.grindingAnimationState.stop();
            }

            // 应用动画
            alwaysUpdateAnimation(
                    entity.grindingAnimationState,
                    BlockAnimations.GRINDINGSTONESPIN,
                    getAnimationProgress(entity.getAge(), tickDelta),
                    1.0F,
                    1.0F
            );

            matrices.push();
            // 将模型平移到方块中心（默认原点在方块左下角）
            matrices.translate(0.5, 1.5, 0.5);
            float facing = state.get(ChestBlock.FACING).asRotation();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing + 90));
            // 如果模型是"倒着"建的，绕 X 轴旋转 180 度
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));

            this.base.render(matrices, vertexConsumer, light, overlay);
            this.top.render(matrices, vertexConsumer, light, overlay);
            this.handle.render(matrices, vertexConsumer, light, overlay);
            // 渲染产出物品
            if (!output.isEmpty()) {
                matrices.pop();
                matrices.push();
                float grindingProgress = entity.getGrindingProgress();
                matrices.translate(0.5, grindingProgress * 0.005f + 0.45, 0.5);
                matrices.scale(0.5f, 0.5f, 0.5f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                this.itemRenderer.renderItem(output, ModelTransformationMode.FIXED,
                        light, overlay, matrices, vertexConsumers, world, seed);
            }
        } finally {
            // 恢复模型部件状态
            savedState.applyTo(top);
            matrices.pop();
        }
    }
}