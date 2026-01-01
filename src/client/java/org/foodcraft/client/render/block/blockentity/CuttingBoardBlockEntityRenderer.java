package org.foodcraft.client.render.block.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.foodcraft.block.CuttingBoardBlock;
import org.foodcraft.block.entity.CuttingBoardBlockEntity;
import org.foodcraft.block.process.CuttingProcess;
import org.foodcraft.client.render.model.FoodCraftModelLoader;
import org.foodcraft.client.util.RenderUtils;
import org.foodcraft.registry.ModItems;
import org.foodcraft.util.FoodCraftUtils;

import java.util.HashMap;
import java.util.Map;

public class CuttingBoardBlockEntityRenderer extends SimpleUpPlaceBlockEntityRenderer<CuttingBoardBlockEntity> {
    private static final int FIRST_CUT = 1;
    public static final Map<Item, Float> ITEM_TRANS = new HashMap<>();

    private final BakedModelManager modelManager;
    private final BlockModelRenderer modelRenderer;

    static {
        ITEM_TRANS.put(Items.COD, -90f);
        ITEM_TRANS.put(Items.COOKED_COD, -90f);
        ITEM_TRANS.put(Items.SALMON, -90f);
        ITEM_TRANS.put(Items.COOKED_SALMON, -90f);
    }

    public CuttingBoardBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
        this.modelManager = ctx.getRenderManager().getModels().getModelManager();
        this.modelRenderer = ctx.getRenderManager().getModelRenderer();
    }

    @Override
    public void render(CuttingBoardBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // 优先处理切割流程渲染
        CuttingProcess<CuttingBoardBlockEntity> cuttingProcess = entity.getCuttingProcess();

        if (cuttingProcess != null && cuttingProcess.isActive()) {
            if (tryRenderCuttingProcessModel(entity, cuttingProcess, matrices, vertexConsumers, light, overlay)) {
                return; // 成功渲染切割模型，跳过后续渲染
            }
        }

        // 渲染厨房刀
        if (entity.getStack(0).getItem().equals(ModItems.KITCHEN_KNIFE)) {
            renderKitchenKnife(entity, matrices, vertexConsumers, light);
        }

        // 渲染基础物品
        super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);
    }

    /**
     * 尝试渲染切割流程模型
     * @return 是否成功渲染了切割模型
     */
    private boolean tryRenderCuttingProcessModel(CuttingBoardBlockEntity entity,
                                                 CuttingProcess<CuttingBoardBlockEntity> cuttingProcess,
                                                 MatrixStack matrices,
                                                 VertexConsumerProvider vertexConsumers,
                                                 int light, int overlay) {
        CuttingProcess.CuttingState processState = cuttingProcess.getState();

        // 验证输入物品
        if (!isValidCuttingState(processState)) {
            return false;
        }

        // 验证切割次数
        int currentCut = processState.currentCut();
        if (currentCut < FIRST_CUT) {
            return false;
        }

        // 获取切割模型
        ItemStack inputStack = new ItemStack(processState.inputItemType());
        BakedModel model = RenderUtils.getCuttingModel(inputStack, currentCut, modelManager);

        if (isInvalidModel(model)) {
            return false;
        }

        // 渲染切割模型
        renderCuttingModel(entity, inputStack, model, matrices, vertexConsumers, light);
        return true;
    }

    /**
     * 验证切割状态是否有效
     */
    private boolean isValidCuttingState(CuttingProcess.CuttingState state) {
        return state != null && state.hasInputItem() && state.inputItemType() != null;
    }

    /**
     * 验证模型是否无效
     */
    private boolean isInvalidModel(BakedModel model) {
        return model == null || model == modelManager.getMissingModel();
    }

    /**
     * 渲染厨房刀
     */
    private void renderKitchenKnife(CuttingBoardBlockEntity entity, MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers, int light) {
        BakedModel model = modelManager.getModel(FoodCraftModelLoader.BOARD_KITCHEN_KNIFE);
        Direction facing = entity.getCachedState().get(CuttingBoardBlock.FACING);
        BlockState state = FoodCraftUtils.createCountBlockstate(entity.getStack(0), facing);

        renderTransformedModel(entity, model, state, facing, matrices, vertexConsumers, light);
    }

    /**
     * 渲染切割模型
     */
    private void renderCuttingModel(CuttingBoardBlockEntity entity, ItemStack itemStack,
                                    BakedModel model, MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers, int light) {
        Direction facing = entity.getCachedState().get(CuttingBoardBlock.FACING);
        BlockState state = FoodCraftUtils.createCountBlockstate(itemStack, facing);

        matrices.push();
        ApplyTransformations(entity, matrices);
        renderTransformedModel(entity, model, state, facing, matrices, vertexConsumers, light);
        matrices.pop();
    }

    /**
     * 渲染变换后的模型（通用方法）
     */
    private void renderTransformedModel(CuttingBoardBlockEntity entity, BakedModel model,
                                        BlockState state, Direction facing,
                                        MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        matrices.translate(-0.5, -0.5, -0.5);

        RenderLayer renderLayer = getRenderLayer(state, entity.getCachedState());
        if (entity.getStack(0).getItem().equals(ModItems.KITCHEN_KNIFE)){
            renderLayer = RenderLayer.getCutout();
        }

        modelRenderer.render(
                entity.getWorld(),
                model,
                state,
                entity.getPos(),
                matrices,
                vertexConsumers.getBuffer(renderLayer),
                true,
                Random.create(),
                state.getRenderingSeed(entity.getPos()),
                OverlayTexture.DEFAULT_UV
        );

        matrices.pop();
    }

    /**
     * 获取渲染层
     */
    private RenderLayer getRenderLayer(BlockState state, BlockState cachedState) {
        if (!state.isAir()) {
            return RenderLayers.getBlockLayer(state);
        }
        return RenderLayers.getBlockLayer(cachedState);
    }

    @Override
    protected BlockState getInventoryBlockState(CuttingBoardBlockEntity entity) {
        return entity.getInventoryBlockState();
    }

    @Override
    protected void ApplyTransformations(CuttingBoardBlockEntity entity, MatrixStack matrices) {
        matrices.translate(0.0, 0.1, 0.0);

        CuttingProcess.CuttingState state = entity.getCuttingProcess().getState();
        float rotationAngle = 0.0f;

        // 检查两种情况，获取对应的旋转角度
        if (state.isActive() && ITEM_TRANS.containsKey(state.inputItemType())) {
            rotationAngle = ITEM_TRANS.get(state.inputItemType());
        } else if (ITEM_TRANS.containsKey(entity.getStack(0).getItem())) {
            rotationAngle = ITEM_TRANS.get(entity.getStack(0).getItem());
        }

        // 如果找到旋转角度，应用旋转
        if (rotationAngle != 0.0f) {
            matrices.translate(0.5, 0.5, 0.5);  // 移动到中心点
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));
            matrices.translate(-0.5, -0.5, -0.5);  // 移回原位
        }
    }
}