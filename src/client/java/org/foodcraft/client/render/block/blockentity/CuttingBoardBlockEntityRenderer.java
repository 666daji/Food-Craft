package org.foodcraft.client.render.block.blockentity;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
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
import org.foodcraft.client.util.RenderUtils;

import java.util.HashMap;
import java.util.Map;

public class CuttingBoardBlockEntityRenderer extends UpPlaceBlockEntityRenderer<CuttingBoardBlockEntity> {
    public static final Map<Item, Float> ITEM_ROTATIONS = new HashMap<>();

    static {
        ITEM_ROTATIONS.put(Items.COD, -90f);
        ITEM_ROTATIONS.put(Items.COOKED_COD, -90f);
        ITEM_ROTATIONS.put(Items.SALMON, -90f);
        ITEM_ROTATIONS.put(Items.COOKED_SALMON, -90f);
    }

    private final BakedModelManager modelManager;
    private final BlockModelRenderer modelRenderer;

    public CuttingBoardBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
        this.modelManager = ctx.getRenderManager().getModels().getModelManager();
        this.modelRenderer = ctx.getRenderManager().getModelRenderer();
    }

    @Override
    public void render(CuttingBoardBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        CuttingProcess<CuttingBoardBlockEntity> cuttingProcess = entity.getCuttingProcess();
        ItemStack currentStack = cuttingProcess.isActive()?
                cuttingProcess.getState().inputStack():
                entity.getStack(0);

        matrices.push();
        matrices.translate(0, 0.1, 0);

        // 物品特定旋转
        if (ITEM_ROTATIONS.containsKey(currentStack.getItem())) {
            matrices.translate(0.5, 0, 0.5);
            if (cuttingProcess.isActive()) {
                Direction facing = entity.getCachedState().get(CuttingBoardBlock.FACING);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
            }
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ITEM_ROTATIONS.get(currentStack.getItem())));
            matrices.translate(-0.5, 0, -0.5);
        }

        // 优先尝试渲染切割模型
        if (!renderCuttingModel(entity, matrices, vertexConsumers)) {
            // 如果没有切割模型，渲染默认物品
            fromStackRender(currentStack, entity, tickDelta, matrices, vertexConsumers, light, overlay);
        }

        matrices.pop();
    }

    /**
     * 渲染切割模型
     */
    private boolean renderCuttingModel(CuttingBoardBlockEntity entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        CuttingProcess<CuttingBoardBlockEntity> process = entity.getCuttingProcess();

        if (process == null || !process.isActive()) {
            return false;
        }

        CuttingProcess.CuttingState state = process.getState();
        if (state == null || state.inputStack() == null) {
            return false;
        }

        int currentCut = state.currentCut();
        if (currentCut < 1) {
            return false;
        }

        BakedModel model = RenderUtils.getCuttingModel(state.inputStack(), currentCut, modelManager);

        if (model == null || model == modelManager.getMissingModel()) {
            return false;
        }

        // 渲染切割模型
        modelRenderer.render(
                entity.getWorld(),
                model,
                entity.getCachedState(),
                entity.getPos(),
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getCutout()),
                true,
                Random.create(),
                entity.getCachedState().getRenderingSeed(entity.getPos()),
                OverlayTexture.DEFAULT_UV
        );

        return true;
    }
}