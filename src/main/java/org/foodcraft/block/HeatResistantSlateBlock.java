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
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
import org.foodcraft.registry.ModBlockEntityTypes;
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
        // 处理多方块逻辑
        if (!state.isOf(newState.getBlock())) {
            // 方块被替换或破坏
            if (!world.isClient) {
                // 处理核心方块破坏
                MultiBlockHelper.onCoreBlockBroken(world, pos, this);
            }
        }

        // 处理模具掉落
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof HeatResistantSlateBlockEntity heatResistantSlateBlockEntity) {
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
            if (random.nextInt(10) == 0) {
                world.playSound(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SoundEvents.BLOCK_CAMPFIRE_CRACKLE,
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
        return blockEntity.isValidItem(handStack) || HeatResistantSlateBlockEntity.isCanPlaceMold(handStack);
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