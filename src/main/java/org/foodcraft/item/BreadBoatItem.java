package org.foodcraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.foodcraft.block.EmptyBreadBoatBlock;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.container.BreadBoatContainer;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.FoodContent;
import org.jetbrains.annotations.Nullable;

/**
 * 表示面包船容器的物品。
 * <p>构造函数中的物品设置中必须拥有食物组件。</p>
 */
public class BreadBoatItem extends BlockItem {

    public BreadBoatItem(Block block, Settings settings) {
        super(block, settings);
    }

    /**
     * 获取一个所有汤类型的对应物品堆栈列表
     * @param item 基础物品
     * @return 所有汤类型的物品堆栈列表
     */
    public static DefaultedList<ItemStack> getAll(BreadBoatItem item) {
        DefaultedList<ItemStack> result = DefaultedList.of();

        for (BreadBoatContainer.BreadBoatSoupType soupType : BreadBoatContainer.BreadBoatSoupType.values()) {
            ItemStack stack = new ItemStack(item);
            ContainerUtil.replaceContent(stack, soupType.getContent());
            result.add(stack);
        }

        return result;
    }

    @Override
    protected @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        AbstractContent content = ContainerUtil.extractContent(context.getStack());
        BlockState originalState = super.getPlacementState(context);

        if (content != null) {
            BreadBoatContainer.BreadBoatSoupType soupType =
                    BreadBoatContainer.BreadBoatSoupType.fromContent(content);

            // 尝试返回盛有对应汤的方块状态
            if (originalState != null && soupType != null && originalState.getBlock()
                    instanceof EmptyBreadBoatBlock) {
                return EmptyBreadBoatBlock.asTargetState(originalState, soupType);
            }
        }

        return originalState;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        if (ContainerUtil.extractContent(stack) != null) {
            return super.getTranslationKey() + ".soup";
        }

        return super.getTranslationKey(stack);
    }

    @Override
    public Text getName(ItemStack stack) {
        AbstractContent content = ContainerUtil.extractContent(stack);

        if (content instanceof FoodContent soup) {
            Text soupName = soup.getDisplayName();
            return Text.translatable(this.getTranslationKey(stack), soupName);
        }

        return super.getName(stack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        AbstractContent soupType = ContainerUtil.extractContent(stack);

        // 如果容器中有汤并且使用物品的为玩家则喝汤
        if (soupType instanceof FoodContent soupContent && user instanceof PlayerEntity player) {
            FoodComponent soup = soupContent.getFoodComponent();
            player.getHungerManager().add(soup.getHunger(), soup.getSaturationModifier());
        }

        return super.finishUsing(stack, world, user);
    }
}
