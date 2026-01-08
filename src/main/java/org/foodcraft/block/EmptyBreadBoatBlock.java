package org.foodcraft.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.dfood.block.SimpleFoodBlock;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.container.BreadBoatContainer;
import org.foodcraft.contentsystem.container.ContainerType;
import org.foodcraft.contentsystem.content.AbstractContent;

public class EmptyBreadBoatBlock extends SimpleFoodBlock {
    /** 对应的装了内容物的方块 */
    protected final BreadBoatBlock targetBlock;

    public EmptyBreadBoatBlock(Settings settings, BreadBoatBlock targetBlock) {
        super(settings, true, targetBlock.simpleShape, targetBlock.useItemTranslationKey, null);
        this.targetBlock = targetBlock;
    }

    /**
     * 将空的容器替换为对应的盛满的汤的容器
     * @param originalState 原本的容器状态
     * @param soupType 盛入的汤
     * @return 对应的盛满汤的容器状态，如果原容器状态的基础方块不是当前实例，则直接返回原方块状态
     */
    public static BlockState asTargetState(BlockState originalState, BreadBoatContainer.BreadBoatSoupType soupType) {
        if (originalState.getBlock() instanceof EmptyBreadBoatBlock edibleContainerBlock) {
            return edibleContainerBlock.targetBlock.getDefaultState()
                    .with(FACING, originalState.get(FACING))
                    .with(BreadBoatBlock.SOUP_TYPE, soupType)
                    .with(edibleContainerBlock.targetBlock.BITES, 0);
        }

        return originalState;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);
        AbstractContent content = ContainerUtil.extractContent(handStack);
        BreadBoatContainer.BreadBoatSoupType soupType = BreadBoatContainer.BreadBoatSoupType.fromContent(content);

        if (soupType != null) {
            ContainerType containerType = ContainerUtil.getContainerType(handStack);
            if (containerType != null) {
                // 尝试清空容器
                handStack.decrement(1);
                if (handStack.isEmpty()) {
                    player.setStackInHand(hand, containerType.remainder());
                } else {
                    player.giveItemStack(containerType.remainder());
                }

                // 播放使用声音
                world.playSound(player, pos, containerType.getUseSound(), SoundCategory.PLAYERS, 1.0F, 1.0F);
            }

            // 将汤盛进来
            if (world.setBlockState(pos, asTargetState(state, soupType))) {
                return ActionResult.SUCCESS;
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }
}
