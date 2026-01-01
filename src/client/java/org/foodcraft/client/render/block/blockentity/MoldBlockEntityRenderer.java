package org.foodcraft.client.render.block.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.foodcraft.block.entity.MoldBlockEntity;

public class MoldBlockEntityRenderer extends SimpleUpPlaceBlockEntityRenderer<MoldBlockEntity> {
    public MoldBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    protected BlockState getInventoryBlockState(MoldBlockEntity entity) {
        return entity.getInventoryBlockState();
    }

    @Override
    protected void ApplyTransformations(MoldBlockEntity entity, MatrixStack matrices) {
        matrices.translate(0.0, 0.1, 0.0);
    }
}
