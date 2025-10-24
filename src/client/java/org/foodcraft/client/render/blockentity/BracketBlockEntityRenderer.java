package org.foodcraft.client.render.blockentity;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.entity.BracketBlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import org.foodcraft.client.render.model.ModModelLayers;
import org.joml.Quaternionf;

public class BracketBlockEntityRenderer implements BlockEntityRenderer<BracketBlockEntity> {
    private final ModelPart bone4;
    private final ModelPart bone3;
    private final ModelPart bone;
    private final ModelPart bone2;
    private static final Identifier TEXTURE = new Identifier(FoodCraft.MOD_ID, "textures/blockentity/bracket.png");

    public BracketBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        ModelPart root = ctx.getLayerModelPart(ModModelLayers.BRACKET_BLOCK);
        this.bone4 = root.getChild("bone4");
        this.bone3 = this.bone4.getChild("bone3");
        this.bone = this.bone3.getChild("bone");
        this.bone2 = this.bone3.getChild("bone2");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData bone4 = modelPartData.addChild("bone4", ModelPartBuilder.create().uv(12, 21).cuboid(-1.0F, -40.0F, -1.0F, 2.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 34.0F, 0.0F));

        ModelPartData cube_r1 = bone4.addChild("cube_r1", ModelPartBuilder.create().uv(18, 6).cuboid(0.0F, 0.0F, -1.25F, 0.0F, 12.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.5F, -37.3F, -0.25F, 0.0F, -0.7854F, 0.0F));

        ModelPartData cube_r2 = bone4.addChild("cube_r2", ModelPartBuilder.create().uv(12, 6).cuboid(0.0F, 0.0F, -1.25F, 0.0F, 12.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.25F, -37.1F, -0.25F, 0.0F, 0.7854F, 0.0F));

        ModelPartData bone3 = bone4.addChild("bone3", ModelPartBuilder.create(), ModelTransform.pivot(0.3F, 0.0F, -0.5F));

        ModelPartData bone = bone3.addChild("bone", ModelPartBuilder.create(), ModelTransform.of(0.9244F, 0.0F, -0.0525F, 0.0F, 1.0472F, 0.0F));

        ModelPartData bone2 = bone3.addChild("bone2", ModelPartBuilder.create().uv(12, 0).cuboid(-2.2F, -39.2F, -1.5F, 4.0F, 2.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        ModelPartData cube_r3 = bone2.addChild("cube_r3", ModelPartBuilder.create().uv(8, 0).cuboid(0.0F, -43.0F, -1.0F, 1.0F, 43.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(16.9F, 0.0F, -10.2F, -0.2845F, 0.4976F, -0.5532F));

        ModelPartData cube_r4 = bone2.addChild("cube_r4", ModelPartBuilder.create().uv(0, 0).cuboid(0.0F, -43.0F, 0.0F, 1.0F, 43.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-18.85F, 0.0F, -10.85F, -0.48F, 1.0036F, 0.0F));

        ModelPartData cube_r5 = bone2.addChild("cube_r5", ModelPartBuilder.create().uv(4, 0).cuboid(0.0F, -43.0F, -1.0F, 1.0F, 43.0F, 1.0F, new Dilation(0.0F)), ModelTransform.of(-0.5F, 0.0F, 20.2F, 0.4363F, 0.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void render(BracketBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        matrices.scale(0.5F, 0.5F, 0.5F);
        matrices.multiply(new Quaternionf().rotationX(135));
        matrices.translate(1.0D, -2.2D, -1.0D);

        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
        bone4.render(matrices, vertexConsumer, 15, overlay, 1, 0, 0, 0.5F);

        matrices.pop();
    }
}