package org.foodcraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.foodcraft.block.EmptyBreadBoatBlock;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.container.BreadBoatContainer;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.SoupContent;
import org.jetbrains.annotations.Nullable;

/**
 * 表示面包船容器的物品。
 * <p>构造函数中的物品设置中必须拥有食物组件。</p>
 * <h2>对应物品堆栈的Nbt数据</h2>
 * 当对应的堆栈nbt存在字符串类型的键{@linkplain #SOUP_KEY}时，
 * 其值则为盛入的汤的类型。
 */
public class BreadBoatItem extends BlockItem {
    public static final String SOUP_KEY = "soup_type";

    public BreadBoatItem(Block block, Settings settings) {
        super(block, settings);
    }

    /**
     * 尝试将空可食用容器盛入汤，或者替换盛入的汤。
     * @param stack 原物品堆栈
     * @param soupType 要盛入的汤
     * @return 盛汤后的可食用容器，如果原物品堆栈不是合法可食用容器，则不进行任何操作
     */
    public static ItemStack serveSoup(ItemStack stack, BreadBoatContainer.BreadBoatSoupType soupType) {
        if (stack.getItem() instanceof BreadBoatItem && soupType != null) {
            NbtCompound Nbt = stack.getOrCreateNbt();
            Nbt.putString(BreadBoatItem.SOUP_KEY, soupType.asString());
        }

        return stack;
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
            serveSoup(stack, soupType);
            result.add(stack);
        }

        return result;
    }

    @Override
    protected @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        NbtCompound stackNbt = context.getStack().getNbt();
        BlockState originalState = super.getPlacementState(context);

        if (stackNbt != null && stackNbt.contains(SOUP_KEY)) {
            BreadBoatContainer.BreadBoatSoupType soupType = BreadBoatContainer.BreadBoatSoupType.fromSting(stackNbt.getString(SOUP_KEY));

            // 尝试返回盛有对应汤的方块状态
            if (originalState != null && originalState.getBlock()
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

        if (content instanceof SoupContent soup) {
            Text soupName = soup.getDisplayName();
            return Text.translatable(this.getTranslationKey(stack), soupName);
        }

        return super.getName(stack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        AbstractContent soupType = ContainerUtil.extractContent(stack);

        // 如果容器中有汤并且使用物品的为玩家则喝汤
        if (soupType instanceof SoupContent soupContent && user instanceof PlayerEntity player) {
            FoodComponent soup = soupContent.getFoodComponent();
            player.getHungerManager().add(soup.getHunger(), soup.getSaturationModifier());
        }

        return super.finishUsing(stack, world, user);
    }
}
