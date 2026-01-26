package org.foodcraft.client.render.block.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
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
 * 用于渲染放置在{@link UpPlaceBlock}中的物品堆栈的渲染器接口。
 *
 * <p>这是一个函数式接口，支持通过{@link #register(Item, UpPlaceStackRenderer)}方法为特定物品注册自定义渲染器。
 * 当未找到自定义渲染器时，将使用{@link #DEFAULT_RENDERER}进行默认渲染。</p>
 *
 * <p>渲染器的主要作用是根据物品的不同，在UpPlaceBlock中展示不同的模型或方块状态。</p>
 *
 * @see #register(Item, UpPlaceStackRenderer)
 * @see #get(Item)
 * @see RenderContext
 */
@FunctionalInterface
public interface UpPlaceStackRenderer {

    /**
     * 物品到自定义渲染器的注册表。
     *
     * <p>使用{@link #register(Item, UpPlaceStackRenderer)}方法添加自定义渲染器。</p>
     */
    Map<Item, UpPlaceStackRenderer> RENDERERS = new HashMap<>();

    /**
     * 默认渲染器实例。
     *
     * <p>当未找到物品对应的自定义渲染器时使用此渲染器。</p>
     *
     * <p>渲染逻辑：
     * 1. 如果物品有对应的方块状态，则渲染该方块
     * 2. 否则，以物品形式渲染（旋转90度并朝向方块方向）</p>
     */
    UpPlaceStackRenderer DEFAULT_RENDERER = createDefaultRenderer();

    /**
     * 创建默认渲染器。
     *
     * <p>私有方法，用于初始化{@link #DEFAULT_RENDERER}。</p>
     *
     * @return 默认渲染器实例
     */
    private static UpPlaceStackRenderer createDefaultRenderer() {
        return context -> context.renderBlockStateOrItem(context.getDefaultBlockState());
    }

    /**
     * 渲染物品堆栈在{@link UpPlaceBlock}中的效果。
     *
     * <p>这是接口的唯一抽象方法，每个渲染器必须实现此方法。</p>
     *
     * @param context 渲染上下文，包含所有渲染所需的信息
     */
    void fromStackRender(RenderContext context);

    // ==================== 工厂方法 ====================

    /**
     * 创建一个使用自定义方块状态的渲染器。
     *
     * <p>适用于那些需要固定显示特定方块状态的物品。</p>
     *
     * @param blockState 要渲染的方块状态
     * @return 使用指定方块状态的渲染器
     */
    static UpPlaceStackRenderer withCustomBlockState(BlockState blockState) {
        return context -> context.renderBlockStateOrItem(blockState);
    }

    /**
     * 创建一个使用方块状态提供器的渲染器。
     *
     * <p>适用于需要根据渲染上下文动态决定方块状态的物品。</p>
     *
     * @param stateProvider 根据渲染上下文返回方块状态的函数
     * @return 动态方块状态渲染器
     */
    static UpPlaceStackRenderer withBlockStateProvider(Function<RenderContext, BlockState> stateProvider) {
        return context -> context.renderBlockStateOrItem(stateProvider.apply(context));
    }

    // ==================== 注册表管理方法 ====================

    /**
     * 为指定物品注册自定义渲染器。
     *
     * <p>如果该物品已有渲染器，将被新的渲染器替换。</p>
     *
     * @param item 要注册渲染器的物品
     * @param renderer 自定义渲染器
     */
    static void register(Item item, UpPlaceStackRenderer renderer) {
        RENDERERS.put(item, renderer);
    }

    /**
     * 获取指定物品的渲染器。
     *
     * <p>如果该物品没有注册自定义渲染器，返回{@link #DEFAULT_RENDERER}。</p>
     *
     * @param item 要获取渲染器的物品
     * @return 对应的渲染器
     */
    static UpPlaceStackRenderer get(Item item) {
        return RENDERERS.getOrDefault(item, DEFAULT_RENDERER);
    }

    /**
     * 检查指定物品是否有自定义渲染器。
     *
     * @param item 要检查的物品
     * @return 如果该物品有自定义渲染器返回true，否则返回false
     */
    static boolean hasCustomRenderer(Item item) {
        return RENDERERS.containsKey(item);
    }

    /**
     * 清除所有已注册的自定义渲染器。
     */
    static void clearAll() {
        RENDERERS.clear();
    }

    /**
     * 移除指定物品的自定义渲染器。
     *
     * @param item 要移除渲染器的物品
     */
    static void remove(Item item) {
        RENDERERS.remove(item);
    }

    // ==================== RenderContext 记录类 ====================

    /**
     * 渲染上下文记录类，包含渲染所需的所有信息。
     *
     * <p>这是一个不可变的记录类，包含了渲染过程中需要的所有数据。</p>
     *
     * <p>除了存储数据外，还提供了多个辅助方法来简化渲染逻辑。</p>
     *
     * @param stack 要渲染的物品堆栈
     * @param entity 当前方块实体
     * @param entityContext 方块实体渲染器上下文
     * @param tickDelta 帧间插值
     * @param matrices 变换矩阵栈
     * @param vertexConsumers 顶点消费者提供器
     * @param light 光照等级
     * @param overlay 覆盖纹理
     */
    record RenderContext(
            ItemStack stack,
            BlockEntity entity,
            BlockEntityRendererFactory.Context entityContext,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        // ============ 方向 ============

        /**
         * 获取当前方块实体的朝向。
         *
         * <p>从方块状态中获取水平朝向属性，如果没有该属性则返回默认的EAST方向。</p>
         *
         * @return 方块的朝向
         */
        public Direction getFacing() {
            if (entity.getCachedState().contains(Properties.HORIZONTAL_FACING)) {
                return entity.getCachedState().get(Properties.HORIZONTAL_FACING);
            }
            return Direction.EAST;
        }

        // ============ 方块状态 ============

        /**
         * 获取当前物品堆栈的默认方块状态。
         *
         * <p>通过{@link FoodCraftUtils}根据物品堆栈和朝向创建方块状态。</p>
         *
         * @return 默认方块状态
         */
        public BlockState getDefaultBlockState() {
            return FoodCraftUtils.createCountBlockstate(stack, getFacing());
        }

        // ============ 渲染 ============

        /**
         * 渲染指定的方块状态。
         *
         * <p>如果方块状态不是空气方块，则使用Minecraft的方块渲染系统进行渲染。</p>
         *
         * @param blockState 要渲染的方块状态
         */
        public void renderBlockState(BlockState blockState) {
            if (blockState.getBlock() != Blocks.AIR) {
                entityContext.getRenderManager().renderBlock(
                        blockState,
                        entity.getPos(),
                        entity.getWorld(),
                        matrices,
                        vertexConsumers.getBuffer(RenderLayers.getBlockLayer(blockState)),
                        true,
                        Random.create()
                );
            }
        }

        /**
         * 渲染自定义模型。
         *
         * @param model 要渲染的模型
         * @param state 占位的方块状态
         */
        public void renderCustomModel(BakedModel model, BlockState state) {
            entityContext().getRenderManager().getModelRenderer().render(
                    entity().getWorld(),
                    model,
                    state,
                    entity().getPos(),
                    matrices(),
                    vertexConsumers().getBuffer(RenderLayer.getCutout()),
                    true,
                    Random.create(),
                    state.getRenderingSeed(entity().getPos()),
                    OverlayTexture.DEFAULT_UV
            );
        }

        /**
         * 以物品形式渲染当前物品堆栈。
         */
        public void renderItem() {
            matrices.push();

            // 将物品放置在方块中心上方
            matrices.translate(0.5, 0, 0.5);

            // 缩放物品
            matrices.scale(0.7f, 0.7f, 0.7f);

            // 沿X轴旋转90度，使物品平放
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));

            // 根据方块朝向旋转物品
            Direction facing = getFacing();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing.asRotation()));

            // 渲染物品
            entityContext.getItemRenderer().renderItem(
                    stack,
                    ModelTransformationMode.FIXED,
                    light,
                    overlay,
                    matrices,
                    vertexConsumers,
                    entity.getWorld(),
                    0
            );

            matrices.pop();
        }

        /**
         * 智能渲染方块状态或物品。
         *
         * <p>如果方块状态不是空气方块，则渲染方块状态；否则渲染物品。</p>
         *
         * @param blockState 要渲染的方块状态
         */
        public void renderBlockStateOrItem(BlockState blockState) {
            if (blockState.getBlock() != Blocks.AIR) {
                renderBlockState(blockState);
            } else {
                renderItem();
            }
        }

        /**
         * 默认渲染方法。
         */
        public void defaultRender() {
            renderBlockStateOrItem(getDefaultBlockState());
        }

        // ============ 类型转换 ============

        /**
         * 将当前方块实体转换为指定类型。
         *
         * <p>如果实体是指定类型的实例，则返回转换后的实体；否则返回null。</p>
         *
         * @param <T> 目标实体类型
         * @param clazz 目标实体类的Class对象
         * @return 转换后的实体，如果类型不匹配则返回null
         */
        public <T extends BlockEntity> T getEntityAs(Class<T> clazz) {
            return clazz.isInstance(entity) ? clazz.cast(entity) : null;
        }

        // ============ 物品堆栈 ============

        /**
         * 检查当前物品堆栈是否为空。
         *
         * @return 如果物品堆栈不为空返回true，否则返回false
         */
        public boolean hasContent() {
            return !stack.isEmpty();
        }

        /**
         * 获取当前物品堆栈的数量。
         *
         * @return 物品堆栈的数量
         */
        public int getStackCount() {
            return stack.getCount();
        }

        /**
         * 获取当前物品堆栈的物品。
         *
         * @return 物品堆栈的物品
         */
        public Item getItem() {
            return stack.getItem();
        }

        // ============ 获取器 ============

        /**
         * 获取模型管理器。
         *
         * @return 模型管理器
         */
        public BakedModelManager getModelManager() {
            return entityContext()
                    .getRenderManager()
                    .getModels()
                    .getModelManager();
        }
    }
}