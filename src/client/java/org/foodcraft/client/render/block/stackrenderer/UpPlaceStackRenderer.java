package org.foodcraft.client.render.block.stackrenderer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.foodcraft.block.UpPlaceBlock;
import org.foodcraft.util.FoodCraftUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 表示物品堆栈放置在{@link UpPlaceBlock}中时的渲染器，
 */
@FunctionalInterface
public interface UpPlaceStackRenderer {
    Map<Item, UpPlaceStackRenderer> RENDERERS = new HashMap<>();

    // 默认渲染器实例
    UpPlaceStackRenderer DEFAULT_RENDERER = createDefaultRenderer();

    // 核心渲染逻辑
    static void renderCoreLogic(RenderContext context, BlockState blockState) {
        if (blockState.getBlock() != Blocks.AIR) {
            context.entityContext().getRenderManager().renderBlock(
                    blockState,
                    context.entity().getPos(),
                    context.entity().getWorld(),
                    context.matrices(),
                    context.vertexConsumers().getBuffer(RenderLayers.getBlockLayer(blockState)),
                    true,
                    Random.create()
            );
        } else {
            // 如果是普通物品，渲染物品模型
            context.matrices().push();
            context.matrices().translate(0.5, 0, 0.5);
            context.matrices().scale(0.7f, 0.7f, 0.7f);
            context.matrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));

            Direction facing = getFacing(context);
            context.matrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing.asRotation()));

            context.entityContext().getItemRenderer().renderItem(
                    context.stack(),
                    ModelTransformationMode.FIXED,
                    context.light(),
                    context.overlay(),
                    context.matrices(),
                    context.vertexConsumers(),
                    context.entity().getWorld(),
                    0
            );
            context.matrices().pop();
        }
    }

    // 获取朝向
    static Direction getFacing(RenderContext context) {
        if (context.entity().getCachedState().contains(Properties.HORIZONTAL_FACING)) {
            return context.entity().getCachedState().get(Properties.HORIZONTAL_FACING);
        }
        return Direction.EAST;
    }

    // 获取默认方块状态
    static BlockState getDefaultBlockState(RenderContext context) {
        return FoodCraftUtils.createCountBlockstate(context.stack(), getFacing(context));
    }

    /**
     * 注册渲染器。
     * @param item 要替换模型的物品
     * @param replace 替换逻辑
     */
    static void registry(Item item, UpPlaceStackRenderer replace) {
        RENDERERS.put(item, replace);
    }

    /**
     * 获取渲染器。
     * @param item 物品
     * @return 对应的替换器，如果没有则返回默认渲染器
     */
    static UpPlaceStackRenderer get(Item item) {
        return RENDERERS.getOrDefault(item, DEFAULT_RENDERER);
    }

    /**
     * 检查物品是否有自定义渲染器。
     * @param item 物品
     * @return 是否有自定义渲染器
     */
    static boolean hasReplace(Item item) {
        return RENDERERS.containsKey(item);
    }

    /**
     * 清除所有注册的渲染器
     */
    static void clearAll() {
        RENDERERS.clear();
    }

    /**
     * 移除指定物品的渲染器
     * @param item 物品
     */
    static void remove(Item item) {
        RENDERERS.remove(item);
    }

    /**
     * 创建一个渲染器，使用自定义方块状态
     * @param blockState 自定义方块状态
     * @return 渲染器
     */
    static UpPlaceStackRenderer withCustomBlockState(BlockState blockState) {
        return context -> renderCoreLogic(context, blockState);
    }

    /**
     * 创建一个渲染器，使用自定义方块状态提供器
     * @param stateProvider 方块状态提供器
     * @return 渲染器
     */
    static UpPlaceStackRenderer withBlockStateProvider(Function<RenderContext, BlockState> stateProvider) {
        return context -> {
            BlockState blockState = stateProvider.apply(context);
            renderCoreLogic(context, blockState);
        };
    }

    /**
     * 渲染物品堆栈在{@link UpPlaceBlock}中的效果
     *
     * @param context 渲染上下文
     */
    void fromStackRender(RenderContext context);

    /**
     * 创建默认渲染器
     */
    private static UpPlaceStackRenderer createDefaultRenderer() {
        return context -> {
            BlockState blockState = getDefaultBlockState(context);
            renderCoreLogic(context, blockState);
        };
    }

    record RenderContext(ItemStack stack,
                         BlockEntity entity,
                         BlockEntityRendererFactory.Context entityContext,
                         float tickDelta,
                         MatrixStack matrices,
                         VertexConsumerProvider vertexConsumers,
                         int light,
                         int overlay) {
    }
}