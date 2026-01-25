package org.foodcraft.client.render.block.stackrenderer;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.dfood.block.FoodBlock;
import org.foodcraft.block.CuttingBoardBlock;
import org.foodcraft.block.entity.HeatResistantSlateBlockEntity;
import org.foodcraft.client.render.model.ModModelLoader;
import org.foodcraft.registry.ModBlocks;
import org.foodcraft.registry.ModItems;
import org.foodcraft.util.FoodCraftUtils;

public class UpPlaceStackRenderers {
    public static void registerAll() {
        // 花盆的烹饪状态渲染
        UpPlaceStackRenderer.registry(Items.FLOWER_POT,
                UpPlaceStackRenderer.withBlockStateProvider(context -> {
                    if (context.stack().getCount() <= 1 || !(context.entity() instanceof HeatResistantSlateBlockEntity blockEntity)) {
                        return UpPlaceStackRenderer.getDefaultBlockState(context);
                    }

                    Direction resultDirection = blockEntity.getResultDirection();
                    return ModBlocks.FLOWER_POT_COOKING.getDefaultState()
                            .with(FoodBlock.FACING, resultDirection != null ? resultDirection : Direction.EAST)
                            .with(FoodCraftUtils.getFoodBlockProperty(ModBlocks.FLOWER_POT_COOKING),
                                    blockEntity.getStack(0).getCount());
                })
        );

        // 厨房刀渲染器
        UpPlaceStackRenderer.registry(ModItems.KITCHEN_KNIFE, createKitchenKnifeRenderer());
    }

    /**
     * 创建厨房刀渲染器
     */
    private static UpPlaceStackRenderer createKitchenKnifeRenderer() {
        return context -> {
            BakedModel model = context.entityContext()
                    .getRenderManager()
                    .getModels()
                    .getModelManager()
                    .getModel(ModModelLoader.BOARD_KITCHEN_KNIFE);

            if (model == null) {
                UpPlaceStackRenderer.DEFAULT_RENDERER.fromStackRender(context);
                return;
            }

            Direction facing = context.entity().getCachedState().get(CuttingBoardBlock.FACING);
            BlockState state = UpPlaceStackRenderer.getDefaultBlockState(context);

            MatrixStack matrices = context.matrices();
            matrices.push();

            // 应用厨房刀特定的变换
            matrices.translate(0.5, 0.0, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
            matrices.translate(-0.5, -0.1, -0.5);

            // 渲染厨房刀模型
            context.entityContext().getRenderManager().getModelRenderer().render(
                    context.entity().getWorld(),
                    model,
                    state,
                    context.entity().getPos(),
                    matrices,
                    context.vertexConsumers().getBuffer(RenderLayer.getCutout()),
                    true,
                    Random.create(),
                    state.getRenderingSeed(context.entity().getPos()),
                    OverlayTexture.DEFAULT_UV
            );

            matrices.pop();
        };
    }
}