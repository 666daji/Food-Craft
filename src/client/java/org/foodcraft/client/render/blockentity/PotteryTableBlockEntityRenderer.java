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

    public PotteryTableBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.blockRenderManager = ctx.getRenderManager();

        this.root = ctx.getLayerModelPart(ModModelLayers.POTTERY_TABLE);
        this.base = root.getChild("base");
        this.tableTop = root.getChild("table_top");
        this.clayBall = root.getChild("clay_ball");
        this.workSurface = tableTop.getChild("work_surface");

        // 填充模型部件映射
        modelParts.put("clay_ball", clayBall);

        // 保存模型部件的初始状态
        saveInitialState("clay_ball", clayBall);
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
    public void render(PotteryTableBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = entity.getWorld();
        BlockState state = world != null?
                entity.getCachedState():
                ModBlocks.POTTERY_TABLE.getDefaultState();

        try {
            matrices.push();

            // 将模型平移到方块中心（默认原点在方块左下角）
            matrices.translate(0.5, 1.5, 0.5);
            float facing = state.get(PotteryTableBlock.FACING).asRotation();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing + 90));
            // 如果模型是倒着建的，绕 X 轴旋转 180 度
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));

            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));

            // 渲染底座和桌面（始终渲染）
            this.base.render(matrices, vertexConsumer, light, overlay);
            this.tableTop.render(matrices, vertexConsumer, light, overlay);

            // 获取输入槽和输出槽的物品
            ItemStack inputStack = entity.getStack(PotteryTableBlockEntity.INPUT_SLOT);
            ItemStack outputStack = entity.getStack(PotteryTableBlockEntity.OUTPUT_SLOT);

            // 检查是否需要渲染物品
            if (!outputStack.isEmpty()) {
                // 尝试渲染输出槽物品
                renderOutputItem(entity, outputStack, matrices, vertexConsumers, light, overlay, tickDelta);
            } else if (!inputStack.isEmpty()) {
                // 尝试渲染陶球
                this.clayBall.render(matrices, vertexConsumer, light, overlay);
            }
        } finally {
            matrices.pop();
        }
    }

    /**
     * 渲染输出槽物品
     * @return 是否成功渲染
     */
    private boolean renderOutputItem(PotteryTableBlockEntity entity, ItemStack outputStack,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, int overlay, float tickDelta) {

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

        try {
            // 渲染方块状态
            matrices.push();

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