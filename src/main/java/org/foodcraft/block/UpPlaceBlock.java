package org.foodcraft.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.foodcraft.block.entity.UpPlaceBlockEntity;

/**
 * 可以在其中放置物品的方块基类，
 * 物品通过{@link Inventory}接口进行管理，
 * 因此实现该类的方块必须拥有一个实现{@link Inventory}接口的方块实体
 * @see UpPlaceBlockEntity
 */
public abstract class UpPlaceBlock extends BlockWithEntity {
    public UpPlaceBlock(Settings settings) {
        super(settings);
    }

    /**
     * 当方块被替换或破环时将方块实体中的库存物品掉落出来
     */
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory inventory) {
                ItemScatterer.spawn(world, pos, inventory);
                world.updateComparators(pos, this);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    /**
     * 合并方块的基础形状与容器中的物品形状
     */
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof UpPlaceBlockEntity blockEntity && !blockEntity.isEmpty()) {
            return VoxelShapes.union(getBaseShape(), blockEntity.getContentShape(state, world, pos, context));
        }
        return getBaseShape();
    }

    public abstract VoxelShape getBaseShape();

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (blockEntity instanceof UpPlaceBlockEntity upPlaceBlockEntity) {
            // 尝试取出物品
            if (canFetched(upPlaceBlockEntity, handStack)) {
                ActionResult fetchResult = upPlaceBlockEntity.tryFetchItem(player);
                if (fetchResult.isAccepted()) {
                    onFetch(state, world, pos, player, hand, hit);
                    return fetchResult;
                }
            }

            // 尝试放置物品
            if (canPlace(upPlaceBlockEntity, handStack)) {
                ActionResult placeResult = upPlaceBlockEntity.tryAddItem(handStack);
                if (placeResult.isAccepted()) {
                    if (!player.isCreative()) {
                        handStack.decrement(1);
                    }
                    onPlace(state, world, pos, player, hand, hit);
                    return placeResult;
                }
            }
        }

        return ActionResult.FAIL;
    }

    /**
     * 在取出物品时调用
     */
    private void onFetch(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
    }

    /**
     * 在放置物品时调用
     */
    private void onPlace(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
    }

    public abstract boolean canFetched(UpPlaceBlockEntity blockEntity, ItemStack handStack);

    public abstract boolean canPlace(UpPlaceBlockEntity blockEntity, ItemStack handStack);
}
