package org.foodcraft.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.StewItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.dfood.block.SimpleFoodBlock;
import org.foodcraft.util.enums.SoupType;

public class EmptyEdibleContainerBlock extends SimpleFoodBlock {
    /** 对应的装了内容物的方块 */
    protected final EdibleContainerBlock targetBlock;

    public EmptyEdibleContainerBlock(Settings settings, EdibleContainerBlock targetBlock) {
        super(settings, true, targetBlock.simpleShape, targetBlock.useItemTranslationKey, null);
        this.targetBlock = targetBlock;
    }

    /**
     * 将空的容器替换为对应的盛满的汤的容器
     * @param originalState 原本的容器状态
     * @param soupType 盛入的汤
     * @return 对应的盛满汤的容器状态，如果原容器状态的基础方块不是当前实例，则直接返回原方块状态
     */
    public static BlockState asTargetState(BlockState originalState, SoupType soupType) {
        if (originalState.getBlock() instanceof EmptyEdibleContainerBlock edibleContainerBlock) {
            return edibleContainerBlock.targetBlock.getDefaultState()
                    .with(FACING, originalState.get(FACING))
                    .with(EdibleContainerBlock.SOUP_TYPE, soupType)
                    .with(edibleContainerBlock.targetBlock.BITES, 0);
        }

        return originalState;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);
        SoupType type = SoupType.fromStack(handStack);

        if (type != null) {
            // 尝试清空容器
            if (handStack.getItem() instanceof StewItem && !player.isCreative()) {
                player.setStackInHand(hand, new ItemStack(Items.BOWL));
            }
            world.playSound(player, pos, SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.BLOCKS, 1.0F, 1.0F);

            // 将汤盛进来
            if (world.setBlockState(pos, asTargetState(state, type))) {
                return ActionResult.SUCCESS;
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }
}
