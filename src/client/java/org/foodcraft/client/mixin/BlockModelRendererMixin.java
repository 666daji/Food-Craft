package org.foodcraft.client.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.BlockRenderView;
import org.dfood.block.FoodBlock;
import org.foodcraft.block.entity.HeatResistantSlateBlockEntity;
import org.foodcraft.client.util.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V",
            at = @At("HEAD"),
            argsOnly = true)
    public BakedModel renderCookingModel(BakedModel model, BlockRenderView world, BakedModel bakedModel, BlockState state, BlockPos pos, MatrixStack matrices) {
        if (state.getBlock() instanceof FoodBlock foodBlock &&
                world.getBlockEntity(pos) instanceof HeatResistantSlateBlockEntity){
            int foodValue = state.get(foodBlock.NUMBER_OF_FOOD);

            if (foodValue > 1){
                // 手动旋转模型
                matrices.translate(0.5, 0.5, 0.5);
                float facing = state.get(FoodBlock.FACING).asRotation();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing));
                matrices.translate(-0.5, -0.5, -0.5);

                BakedModelManager manager = MinecraftClient.getInstance().getBakedModelManager();
                BakedModel model1 = RenderUtils.getCookingModel(state, foodValue, manager);
                return model1 == null || model1 == manager.getMissingModel()?
                        model : model1;
            }
        }

        return model;
    }
}
