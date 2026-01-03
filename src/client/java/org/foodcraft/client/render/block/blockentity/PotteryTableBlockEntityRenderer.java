package org.foodcraft.client.render.block.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.PotteryTableBlock;
import org.foodcraft.block.entity.PotteryTableBlockEntity;
import org.foodcraft.client.render.model.ModModelLayers;
import org.foodcraft.util.FoodCraftUtils;

public class PotteryTableBlockEntityRenderer extends WithAnimationBlockEntityRenderer<PotteryTableBlockEntity> {
    private static final Identifier TEXTURE = new Identifier(FoodCraft.MOD_ID, "textures/blockentity/pottery_table.png");
    private static final float MODEL_Y_TRANSLATION = 1.5f;
    private static final float MODEL_X_ROTATION = 180.0f;
    private static final float OUTPUT_ITEM_Y = 1.2f;

    private final ModelPart base;
    private final ModelPart tableTop;
    private final ModelPart clayBall;
    private final ModelPart workSurface;
    private final BlockRenderManager blockRenderManager;

    public PotteryTableBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.blockRenderManager = ctx.getRenderManager();

        ModelPart root = ctx.getLayerModelPart(ModModelLayers.POTTERY_TABLE);
        this.base = root.getChild("base");
        this.tableTop = root.getChild("table_top");
        this.clayBall = root.getChild("clay_ball");
        this.workSurface = tableTop.getChild("work_surface");

        registerModelPart("clay_ball", clayBall);
        registerModelPart("work_surface", workSurface);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // 底座部分
        root.addChild("base", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-8.0F, 7.0F, -8.0F, 16.0F, 1.0F, 16.0F, new Dilation(0.0F))
                        .uv(32, 30).cuboid(-3.0F, 4.0F, -3.0F, 6.0F, 2.0F, 6.0F, new Dilation(0.0F))
                        .uv(0, 30).cuboid(-4.0F, 6.0F, -4.0F, 8.0F, 1.0F, 8.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 16.0F, 0.0F));

        // 桌面部分
        ModelPartData tableTop = root.addChild("table_top", ModelPartBuilder.create(),
                ModelTransform.pivot(0.0F, 16.0F, 0.0F));

        // 工作台面
        tableTop.addChild("work_surface", ModelPartBuilder.create()
                        .uv(0, 17).cuboid(-6.0F, 3.0F, -6.0F, 12.0F, 1.0F, 12.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        // 陶球部分
        root.addChild("clay_ball", ModelPartBuilder.create()
                        .uv(1, 51).cuboid(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 17.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void render(PotteryTableBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = entity.getWorld();

        // 无世界上下文的简单渲染（用于物品渲染器）
        if (world == null) {
            renderStaticModel(matrices, vertexConsumers, light, overlay);
            return;
        }

        // 有世界上下文的完整渲染
        renderAnimatedModel(entity, tickDelta, matrices, vertexConsumers, light, overlay);
    }

    /**
     * 渲染静态模型（无世界上下文时使用）
     */
    private void renderStaticModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                   int light, int overlay) {
        matrices.push();
        resetAllModelParts();
        try {
            matrices.translate(0.5, MODEL_Y_TRANSLATION, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MODEL_X_ROTATION));

            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
            base.render(matrices, vertexConsumer, light, overlay);
            tableTop.render(matrices, vertexConsumer, light, overlay);
        } finally {
            matrices.pop();
        }
    }

    /**
     * 渲染带动画的完整模型
     */
    private void renderAnimatedModel(PotteryTableBlockEntity entity, float tickDelta, MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState state = entity.getCachedState();

        // 更新和管理动画状态
        manageAnimationState(entity, tickDelta);

        matrices.push();
        try {
            matrices.translate(0.5, MODEL_Y_TRANSLATION, 0.5);
            float facing = state.get(PotteryTableBlock.FACING).asRotation();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing + 90));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MODEL_X_ROTATION));

            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));

            this.base.render(matrices, vertexConsumer, light, overlay);
            this.tableTop.render(matrices, vertexConsumer, light, overlay);

            // 渲染陶球或输出物品
            renderClayBallOrOutputItem(entity, tickDelta, matrices, vertexConsumer,
                    vertexConsumers, light, overlay, state);
        } finally {
            matrices.pop();
        }
    }

    /**
     * 渲染陶球或输出物品
     */
    private void renderClayBallOrOutputItem(PotteryTableBlockEntity entity, float tickDelta,
                                            MatrixStack matrices, VertexConsumer vertexConsumer,
                                            VertexConsumerProvider vertexConsumers, int light,
                                            int overlay, BlockState state) {
        ItemStack inputStack = entity.getStack(PotteryTableBlockEntity.INPUT_SLOT);
        ItemStack outputStack = entity.getStack(PotteryTableBlockEntity.OUTPUT_SLOT);

        if (!outputStack.isEmpty()) {
            renderOutputItem(entity, outputStack, matrices, vertexConsumers,
                    light, overlay, tickDelta, state);
        } else if (!inputStack.isEmpty()) {
            this.clayBall.render(matrices, vertexConsumer, light, overlay);
        }
    }

    /**
     * 管理动画状态
     */
    private void manageAnimationState(PotteryTableBlockEntity entity, float tickDelta) {
        resetAllModelParts();

        // 当输出槽有物品时，重置动画时间
        if (!entity.getStack(PotteryTableBlockEntity.OUTPUT_SLOT).isEmpty()) {
            entity.workSurfaceAnimationState.resetRunningTime();
            entity.clayBallAnimationState.resetRunningTime();
        }

        // 管理工作台面和陶球动画状态
        if (entity.workSurfaceAnimationState.isRunning) {
            entity.workSurfaceAnimationState.startIfNotRunning(entity.getAge());
        } else if (entity.workSurfaceAnimationState.isRunning()){
            entity.workSurfaceAnimationState.stop();
        }

        if (entity.clayBallAnimationState.isRunning) {
            entity.clayBallAnimationState.startIfNotRunning(entity.getAge());
        } else if (entity.clayBallAnimationState.isRunning()){
            entity.clayBallAnimationState.stop();
        }

        // 获取实体年龄和动画进度
        int age = entity.getAge();
        float animationProgress = getAnimationProgress(age, tickDelta);

        // 更新工作台面动画
        alwaysUpdateAnimation(
                entity.workSurfaceAnimationState,
                BlockAnimations.POTTERY_TABLE_WORK_SURFACE_SPIN,
                animationProgress,
                1.0F,
                1.0F
        );

        // 更新陶球动画
        updateAnimation(
                entity.clayBallAnimationState,
                BlockAnimations.POTTERY_TABLE_CLAY_SPIN,
                animationProgress,
                1.0F,
                1.0F
        );
    }

    /**
     * 渲染输出槽物品
     */
    private boolean renderOutputItem(PotteryTableBlockEntity entity, ItemStack outputStack,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, int overlay, float tickDelta, BlockState state) {
        World world = entity.getWorld();
        if (world == null) return false;

        BlockState blockState = entity.getCachedState();
        if (!(blockState.getBlock() instanceof PotteryTableBlock)) return false;

        Direction facing = blockState.get(PotteryTableBlock.FACING);
        BlockState itemBlockState = FoodCraftUtils.createCountBlockstate(outputStack, facing);

        if (itemBlockState.isAir()) {
            return false;
        }

        matrices.push();
        try {
            matrices.translate(-0.5, OUTPUT_ITEM_Y, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MODEL_X_ROTATION));

            this.blockRenderManager.renderBlockAsEntity(
                    itemBlockState,
                    matrices,
                    vertexConsumers,
                    light,
                    overlay
            );

            return true;
        } finally {
            matrices.pop();
        }
    }
}