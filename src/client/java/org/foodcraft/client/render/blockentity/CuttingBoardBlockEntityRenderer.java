package org.foodcraft.client.render.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.foodcraft.block.entity.CuttingBoardBlockEntity;

public class CuttingBoardBlockEntityRenderer extends SimpleUpPlaceBlockEntityRenderer<CuttingBoardBlockEntity>{
    public CuttingBoardBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    protected BlockState getInventoryBlockState(CuttingBoardBlockEntity entity) {
        return entity.getInventoryBlockState();
    }

    @Override
    protected void ApplyTransformations(MatrixStack matrices) {
        matrices.translate(0.0, 0.1, 0.0);
    }
}
