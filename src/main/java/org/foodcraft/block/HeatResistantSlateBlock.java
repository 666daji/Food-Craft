package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.block.entity.CombustionFirewoodBlockEntity;
import org.foodcraft.block.entity.HeatResistantSlateBlockEntity;
import org.foodcraft.block.entity.UpPlaceBlockEntity;
import org.foodcraft.block.multi.MultiBlockHelper;
import org.foodcraft.item.MoldContentItem;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.registry.ModSounds;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class HeatResistantSlateBlock extends UpPlaceBlock {
    protected static final VoxelShape BASE_SHAPE = Block.createCuboidShape(0,0,0,16,2,16);

    public static final BlockPattern stove1x1;
    public static final BlockPattern stove1x2;
    public static final BlockPattern stove2x2;
    public static final BlockPattern stove2x3;

    public HeatResistantSlateBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        if (!world.isClient) {
            // 处理核心方块放置
            MultiBlockHelper.onCoreBlockPlaced(world, pos, this);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof HeatResistantSlateBlockEntity heatResistantSlateBlockEntity) {
                // 处理定型物品的掉落
                if (!heatResistantSlateBlockEntity.originalInputStack.isEmpty()) {
                    // 掉落原始输入物品
                    DefaultedList<ItemStack> originalInputList = DefaultedList.ofSize(1, ItemStack.EMPTY);
                    originalInputList.add(heatResistantSlateBlockEntity.originalInputStack);
                    ItemScatterer.spawn(world, pos, originalInputList);
                } else {
                    // 正常掉落主库存物品
                    ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
                }

                // 掉落额外库存物品（模具）
                DefaultedList<ItemStack> otherStack = heatResistantSlateBlockEntity.getOtherStacks();
                ItemScatterer.spawn(world, pos, otherStack);

                world.updateComparators(pos, this);
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        // 调用父类交互方法
        ActionResult result = super.onUse(state, world, pos, player, hand, hit);

        // 如果交互失败，则尝试交互绑定的篝火
        if (!result.isAccepted() && blockEntity instanceof HeatResistantSlateBlockEntity heatResistantSlateBlockEntity) {
            for (CombustionFirewoodBlockEntity firewoodEntity : heatResistantSlateBlockEntity.getFirewoodEntities()) {
                BlockState firewoodState = firewoodEntity.getCachedState();
                ActionResult firewoodResult = firewoodState.getBlock().onUse(firewoodState, world, firewoodEntity.getPos(), player, hand, hit);
                if (firewoodResult.isAccepted()) {
                    return firewoodResult;
                }
            }
        }

        return result;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof HeatResistantSlateBlockEntity heatResistantSlateBlockEntity
                    && heatResistantSlateBlockEntity.isBaking()) {
            if (random.nextInt(5) == 0) {
                for(int i = 0; i < random.nextInt(1) + 1; ++i) {
                    world.addParticle(ParticleTypes.CLOUD,
                            pos.getX() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
                            pos.getY() + random.nextDouble() + random.nextDouble(),
                            pos.getZ() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
                            0.0, 0.07, 0.0);
                }
            }
            if (random.nextInt(3) == 0) {
                world.playSound(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        ModSounds.COOKING_SOUND,
                        SoundCategory.BLOCKS,
                        0.5F + random.nextFloat(),
                        random.nextFloat() * 0.7F + 0.6F,
                        false
                );
            }
        }
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return BASE_SHAPE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof HeatResistantSlateBlockEntity blockEntity && !blockEntity.isOtherEmpty()) {
            return VoxelShapes.union(getBaseShape(state, world, pos, context), blockEntity.getContentShape(state, world, pos, context));
        }
        return super.getOutlineShape(state, world, pos, context);
    }

    @Override
    public boolean canFetched(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        boolean moldFetched = blockEntity.isEmpty() && !(blockEntity instanceof HeatResistantSlateBlockEntity heatResistantSlateBlockEntity
                && heatResistantSlateBlockEntity.isOtherEmpty()) && !blockEntity.isValidItem(handStack);
        return !blockEntity.isEmpty() || moldFetched;
    }

    @Override
    public boolean canPlace(UpPlaceBlockEntity blockEntity, ItemStack handStack) {
        if (blockEntity instanceof HeatResistantSlateBlockEntity slateEntity) {
            // 检查是否是装有内容的模具
            if (handStack.getItem() instanceof MoldContentItem moldContentItem && moldContentItem.hasContent(handStack)) {
                return true;
            }

            // 检查是否是空模具
            if (HeatResistantSlateBlockEntity.isCanPlaceMold(handStack)) {
                return true;
            }

            // 检查是否可以作为有效输入
            return blockEntity.isValidItem(handStack);
        }
        return false;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos fromPos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, fromPos, notify);

        if (!world.isClient) {
            // 处理相邻方块更新，检查多方块结构完整性
            MultiBlockHelper.onNeighborUpdate(world, pos, this);
        }
    }

    @Nullable
    public BlockPattern getStovePattern(int index){
        switch (index) {
            case 1 -> {
                return stove1x1;
            }
            case 2 -> {
                return stove1x2;
            }
            case 3 -> {
                return stove2x2;
            }
            case 4 -> {
                return stove2x3;
            }
        }
        return null;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HeatResistantSlateBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, (BlockEntityType<? extends HeatResistantSlateBlockEntity>) ModBlockEntityTypes.HEAT_RESISTANT_SLATE, HeatResistantSlateBlockEntity::tick);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    static {
        Predicate<CachedBlockPosition> heatResistantSlatePredicate = cachedBlockPosition -> cachedBlockPosition.getBlockState().getBlock() instanceof HeatResistantSlateBlock;
        Predicate<CachedBlockPosition> firewoodPredicate = cachedBlockPosition -> cachedBlockPosition.getBlockState().isAir() ||
                cachedBlockPosition.getBlockState().getBlock() instanceof FirewoodBlock ||
                cachedBlockPosition.getBlockState().getBlock() instanceof CombustionFirewoodBlock;

        stove1x1 = BlockPatternBuilder.start()
                .aisle("?#?", "#|#")
                .aisle("#^#","#~#")
                .aisle("?#?", "?#?")
                .where('^', cachedBlockPosition -> cachedBlockPosition.getBlockState().isAir())
                .where('#', cachedBlockPosition -> !cachedBlockPosition.getBlockState().isAir())
                .where('|', heatResistantSlatePredicate)
                .where('~', firewoodPredicate)
                .where('?', cachedBlockPosition -> true)
                .build();
        stove1x2 = BlockPatternBuilder.start()
                .aisle("?#?", "#|#")
                .aisle("?#?", "#|#")
                .aisle("#^#","#~#")
                .aisle("?#?", "?#?")
                .where('^', cachedBlockPosition -> cachedBlockPosition.getBlockState().isAir())
                .where('#', cachedBlockPosition -> !cachedBlockPosition.getBlockState().isAir())
                .where('|', heatResistantSlatePredicate)
                .where('~', firewoodPredicate)
                .where('?', cachedBlockPosition -> true)
                .build();
        stove2x2 = BlockPatternBuilder.start()
                .aisle("?##?", "#||#")
                .aisle("?##?", "#||#")
                .aisle("#^^#","#~~#")
                .aisle("?##?", "?##?")
                .where('^', cachedBlockPosition -> cachedBlockPosition.getBlockState().isAir())
                .where('#', cachedBlockPosition -> !cachedBlockPosition.getBlockState().isAir())
                .where('|', heatResistantSlatePredicate)
                .where('~', firewoodPredicate)
                .where('?', cachedBlockPosition -> true)
                .build();
        stove2x3 = BlockPatternBuilder.start()
                .aisle("?##?", "#||#")
                .aisle("?##?", "#||#")
                .aisle("?##?", "#||#")
                .aisle("#^^#","#~~#")
                .aisle("?##?", "?##?")
                .where('^', cachedBlockPosition -> cachedBlockPosition.getBlockState().isAir())
                .where('#', cachedBlockPosition -> !cachedBlockPosition.getBlockState().isAir())
                .where('|', heatResistantSlatePredicate)
                .where('~', firewoodPredicate)
                .where('?', cachedBlockPosition -> true)
                .build();
    }
}