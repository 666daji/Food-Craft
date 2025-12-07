package org.foodcraft.client.render.blockentity;

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
import org.foodcraft.registry.ModBlocks;
import org.foodcraft.util.FoodCraftUtils;

public class PotteryTableBlockEntityRenderer extends WithAnimationBlockEntityRenderer<PotteryTableBlockEntity> {
    private final ModelPart root;
    private final ModelPart base;
    private final ModelPart tableTop;
    private final ModelPart clayBall;
    private final ModelPart workSurface;

    private static final Identifier TEXTURE = new Identifier(FoodCraft.MOD_ID, "textures/blockentity/pottery_table.png");
    private final BlockRenderManager blockRenderManager;

    /**
     * 创建陶艺工作台渲染器
     *
     * @param ctx 渲染上下文
     */
    public PotteryTableBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.blockRenderManager = ctx.getRenderManager();

        // 加载模型部件
        this.root = ctx.getLayerModelPart(ModModelLayers.POTTERY_TABLE);
        this.base = root.getChild("base");
        this.tableTop = root.getChild("table_top");
        this.clayBall = root.getChild("clay_ball");
        this.workSurface = tableTop.getChild("work_surface");

        // 注册需要动画的模型部件
        registerModelPart("clay_ball", clayBall);
        registerModelPart("work_surface", workSurface);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        // 底座部分
        ModelPartData base = modelPartData.addChild("base", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-8.0F, 7.0F, -8.0F, 16.0F, 1.0F, 16.0F, new Dilation(0.0F))
                        .uv(32, 30).cuboid(-3.0F, 4.0F, -3.0F, 6.0F, 2.0F, 6.0F, new Dilation(0.0F))
                        .uv(0, 30).cuboid(-4.0F, 6.0F, -4.0F, 8.0F, 1.0F, 8.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 16.0F, 0.0F));

        // 桌面部分
        ModelPartData tableTop = modelPartData.addChild("table_top", ModelPartBuilder.create(),
                ModelTransform.pivot(0.0F, 16.0F, 0.0F));

        // 工作台面
        ModelPartData workSurface = tableTop.addChild("work_surface", ModelPartBuilder.create()
                        .uv(0, 17).cuboid(-6.0F, 3.0F, -6.0F, 12.0F, 1.0F, 12.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        // 陶球部分
        ModelPartData clayBall = modelPartData.addChild("clay_ball", ModelPartBuilder.create()
                        .uv(1, 51).cuboid(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 17.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void render(PotteryTableBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        // 获取世界和方块状态
        World world = entity.getWorld();
        BlockState state = world != null ?
                entity.getCachedState() :
                ModBlocks.POTTERY_TABLE.getDefaultState();

        // 更新和管理动画状态
        manageAnimationState(entity, tickDelta);

        // 开始渲染
        matrices.push();
        try {
            // 将模型平移到方块中心（默认原点在方块左下角）
            matrices.translate(0.5, 1.5, 0.5);
            float facing = state.get(PotteryTableBlock.FACING).asRotation();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing + 90));
            // 绕 X 轴旋转 180 度
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));

            // 获取顶点消费者
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
     *
     * @param entity          方块实体
     * @param tickDelta       部分时间
     * @param matrices        矩阵栈
     * @param vertexConsumer  顶点消费者
     * @param vertexConsumers 顶点消费者提供者
     * @param light           光照值
     * @param overlay         覆盖层
     * @param state           方块状态
     */
    private void renderClayBallOrOutputItem(PotteryTableBlockEntity entity, float tickDelta,
                                            MatrixStack matrices, VertexConsumer vertexConsumer,
                                            VertexConsumerProvider vertexConsumers, int light,
                                            int overlay, BlockState state) {

        // 获取输入槽和输出槽的物品
        ItemStack inputStack = entity.getStack(PotteryTableBlockEntity.INPUT_SLOT);
        ItemStack outputStack = entity.getStack(PotteryTableBlockEntity.OUTPUT_SLOT);

        // 检查是否需要渲染物品
        if (!outputStack.isEmpty()) {
            // 渲染输出槽物品
            renderOutputItem(entity, outputStack, matrices, vertexConsumers,
                    light, overlay, tickDelta, state);
        } else if (!inputStack.isEmpty()) {
            // 渲染陶球
            this.clayBall.render(matrices, vertexConsumer, light, overlay);
        }
    }

    /**
     * 管理动画状态
     *
     * @param entity     方块实体
     * @param tickDelta  部分时间
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
     *
     * @param entity          方块实体
     * @param outputStack     输出物品堆栈
     * @param matrices        矩阵栈
     * @param vertexConsumers 顶点消费者提供者
     * @param light           光照值
     * @param overlay         覆盖层
     * @param tickDelta       部分时间
     * @param state           方块状态
     * @return 是否成功渲染
     */
    private boolean renderOutputItem(PotteryTableBlockEntity entity, ItemStack outputStack,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, int overlay, float tickDelta, BlockState state) {

        World world = entity.getWorld();
        if (world == null) return false;

        BlockState blockState = entity.getCachedState();
        if (!(blockState.getBlock() instanceof PotteryTableBlock)) return false;

        // 获取方块朝向
        Direction facing = blockState.get(PotteryTableBlock.FACING);

        // 获取方块状态
        BlockState itemBlockState = FoodCraftUtils.createCountBlockstate(outputStack, facing);

        // 如果方块状态为空（即返回空气），则不渲染
        if (itemBlockState.isAir()) {
            return false;
        }

        // 渲染方块状态
        matrices.push();
        try {
            // 调整方块位置和大小
            matrices.translate(-0.5, 1.2, 0.5); // 居中并调整高度
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));

            // 渲染方块
            this.blockRenderManager.renderBlockAsEntity(
                    itemBlockState,
                    matrices,
                    vertexConsumers,
                    light,
                    overlay
            );

            return true; // 成功渲染
        } finally {
            matrices.pop();
        }
    }
}