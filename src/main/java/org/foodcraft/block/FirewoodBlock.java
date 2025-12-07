package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.dfood.block.FoodBlock;
import org.dfood.shape.FoodShapeHandle;
import org.foodcraft.block.entity.CombustionFirewoodBlockEntity;

public class FirewoodBlock extends FoodBlock {
    public static final DirectionProperty HORIZONTAL_FACING = Properties.HORIZONTAL_FACING;
    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 7.0, 16.0);

    /** 被点燃后变成的方块 */
    protected final Block targetBlock;

    public FirewoodBlock(Settings settings, int max_food, Block targetBlock) {
        super(settings, max_food, false);
        this.targetBlock = targetBlock;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE_HANDLE.getShape(state, NUMBER_OF_FOOD, Shapes.class);
    }

    /**
     * 使用打火石点燃柴火堆
     */
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (state.getBlock() instanceof FirewoodBlock firewoodBlock && player.getStackInHand(hand).getItem() == Items.FLINT_AND_STEEL){
            boolean bl = firewoodBlock.tryIgnite(state, world, pos, player);
            return bl? ActionResult.SUCCESS: ActionResult.PASS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    /**
     * 尝试点燃柴火堆,只有当柴火的堆叠数为2时才能成功点燃
     * @param state 当前方块状态
     * @param world 世界
     * @param pos 方块位置
     * @param player 触发点燃的玩家
     * @return 是否成功点燃
     */
    public boolean tryIgnite(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        int currentCount = state.get(NUMBER_OF_FOOD);
        if (currentCount != 2) {
            return false;
        }

        // 检查上方空间
        if (!hasClearSpaceAbove(world, pos)) {
            // 播放失败音效或给玩家提示
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 1.0f);
            return false;
        }

        world.playSound(player, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.4F + 0.8F);

        // 设置燃烧柴火方块状态为首次点燃
        world.setBlockState(pos, targetBlock.getDefaultState()
                .with(HORIZONTAL_FACING, state.get(HORIZONTAL_FACING))
                .with(CombustionFirewoodBlock.COMBUSTION_STATE, CombustionFirewoodBlock.CombustionState.FIRST_IGNITED));

        // 设置方块实体的初始能量
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CombustionFirewoodBlockEntity firewoodEntity) {
            firewoodEntity.setEnergy(CombustionFirewoodBlockEntity.getMaxEnergy());
            firewoodEntity.setFirstCycle(true);
            firewoodEntity.setCycleCount(0);
        }

        world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return true;
    }

    /**
     * 检查柴火堆上方6格内是否为空气
     * @param world 世界
     * @param pos 柴火堆位置
     * @return 上方6格内是否全部为空气
     */
    public static boolean hasClearSpaceAbove(World world, BlockPos pos) {
        for (int i = 1; i <= 6; i++) {
            BlockPos checkPos = pos.up(i);
            BlockState state = world.getBlockState(checkPos);

            // 检查方块是否为空气或可替换方块
            if (!state.isAir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否可以点燃（包括上方空间检查）
     */
    public boolean canIgnite(World world, BlockPos pos) {
        return hasClearSpaceAbove(world, pos);
    }

    public enum Shapes implements FoodShapeHandle.ShapeConvertible {
        SHAPE_A(1, Block.createCuboidShape(0,0,0,16,4,16)),
        SHAPE_B(2, Block.createCuboidShape(0,0,0,16,8,16)),
        SHAPE_C(3, Block.createCuboidShape(0,0,0,16,9,16)),
        SHAPE_D(4, Block.createCuboidShape(0,0,0,16,13,16)),
        SHAPE_E(5, Block.createCuboidShape(0,0,0,16,16,16));

        private final VoxelShape shape;
        private final int id;

        Shapes(int id, VoxelShape shape) {
            this.shape = shape;
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public VoxelShape getShape() {
            return shape;
        }

        public static VoxelShape getShape(int id) {
            for (Shapes s : values()) {
                if (s.id == id) {
                    return s.shape;
                }
            }
            return SHAPE_A.shape;
        }
    }
}
