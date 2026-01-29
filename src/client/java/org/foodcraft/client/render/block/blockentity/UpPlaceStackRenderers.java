package org.foodcraft.client.render.block.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.dfood.block.FoodBlock;
import org.foodcraft.block.EmptyBreadBoatBlock;
import org.foodcraft.block.PlateBlock;
import org.foodcraft.block.entity.HeatResistantSlateBlockEntity;
import org.foodcraft.client.render.item.renderer.MoldItemRenderer;
import org.foodcraft.client.render.model.ModModelLoader;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.container.BreadBoatContainer;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.registry.ModBlocks;
import org.foodcraft.registry.ModItems;
import org.foodcraft.util.FoodCraftUtils;

public class UpPlaceStackRenderers {
    public static void registerAll() {
        // 花盆
        UpPlaceStackRenderer.register(Items.FLOWER_POT, context -> {
            if (context.getStackCount() <= 1) {
                context.renderBlockStateOrItem(context.getDefaultBlockState());
                return;
            }

            HeatResistantSlateBlockEntity blockEntity = context.getEntityAs(HeatResistantSlateBlockEntity.class);
            if (blockEntity == null) {
                context.renderBlockStateOrItem(context.getDefaultBlockState());
                return;
            }

            Direction resultDirection = blockEntity.getResultDirection();
            BlockState state = ModBlocks.FLOWER_POT_COOKING.getDefaultState()
                    .with(FoodBlock.FACING, resultDirection != null ? resultDirection : Direction.EAST)
                    .with(FoodCraftUtils.getFoodBlockProperty(ModBlocks.FLOWER_POT_COOKING),
                            blockEntity.getStack(0).getCount());

            context.renderBlockState(state);
        });

        // 菜刀
        UpPlaceStackRenderer.register(ModItems.KITCHEN_KNIFE, createKitchenKnifeRenderer());

        // 铁盘
        UpPlaceStackRenderer.register(ModItems.IRON_PLATE, context -> {
            BlockState state = context.getDefaultBlockState();
            AbstractContent content = ContainerUtil.extractContent(context.stack());

            if (content != null && state.getBlock() instanceof PlateBlock) {
                state = state.with(PlateBlock.IS_COVERED, true);
            }

            context.renderBlockStateOrItem(state);
        });

        // 面包船
        UpPlaceStackRenderer.register(ModItems.HARD_BREAD_BOAT, context -> {
            BlockState state = context.getDefaultBlockState();
            AbstractContent content = ContainerUtil.extractContent(context.stack());

            if (content != null && state.getBlock() instanceof EmptyBreadBoatBlock) {
                BreadBoatContainer.BreadBoatSoupType soupType =
                        BreadBoatContainer.BreadBoatSoupType.fromContent(content);
                state = EmptyBreadBoatBlock.asTargetState(state, soupType);
            }

            context.renderBlockStateOrItem(state);
        });

        // 模具
        UpPlaceStackRenderer.register(ModItems.TOAST_EMBRYO_MOLD, context -> {
            MoldItemRenderer.renderMold(context.stack(), ModelTransformationMode.GUI, context.matrices(),
                    context.vertexConsumers(), context.light(), context.overlay());
        });
        UpPlaceStackRenderer.register(ModItems.CAKE_EMBRYO_MOLD, context -> {
            MoldItemRenderer.renderMold(context.stack(), ModelTransformationMode.GUI,
                    context.matrices(), context.vertexConsumers(), context.light(), context.overlay());
        });
    }

    private static UpPlaceStackRenderer createKitchenKnifeRenderer() {
        return context -> {
            BakedModel model = context.getModelManager()
                    .getModel(ModModelLoader.BOARD_KITCHEN_KNIFE);

            if (model == null) {
                context.defaultRender();
                return;
            }

            Direction facing = context.getFacing();
            BlockState state = context.getDefaultBlockState();

            context.matrices().push();

            // 应用菜刀特定的变换
            context.matrices().translate(0.5, 0.0, 0.5);
            context.matrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
            context.matrices().translate(-0.5, -0.1, -0.5);

            // 渲染菜刀模型
            context.renderCustomModel(model, state);

            context.matrices().pop();
        };
    }
}