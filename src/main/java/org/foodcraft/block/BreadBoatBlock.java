package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.dfood.block.SimpleFoodBlock;
import org.dfood.util.IntPropertyManager;
import org.foodcraft.FoodCraft;
import org.foodcraft.contentsystem.container.BreadBoatContainer;
import org.foodcraft.item.BreadBoatItem;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class BreadBoatBlock extends SimpleFoodBlock {
    public static final EnumProperty<BreadBoatContainer.BreadBoatSoupType> SOUP_TYPE = EnumProperty.of("soup_type", BreadBoatContainer.BreadBoatSoupType.class);
    /** 表示当前已食用次数 */
    public final IntProperty BITES;

    public final int maxUse;

    public BreadBoatBlock(Settings settings, VoxelShape shape, int maxUse, @Nullable EnforceAsItem cItem) {
        super(settings, true, shape, false, cItem);
        this.maxUse = maxUse;
        this.BITES = IntPropertyManager.create("bites", 0, maxUse);

        this.setDefaultState(this.getDefaultState().with(BITES, 0));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        // 如果玩家可以吃东西，尝试喝汤
        if (player.canConsume(false)) {
            return tryDrinkSoup(world, pos, state, player);
        }

        // 如果还没被食用，执行父类逻辑
        if (state.get(BITES) == 0) {
            return super.onUse(state, world, pos, player, hand, hit);
        }

        return ActionResult.PASS;
    }

    @Override
    public ItemStack createStack(int count, BlockState state, @Nullable BlockEntity blockEntity) {
        return BreadBoatItem.serveSoup(super.createStack(count, state, blockEntity), state.get(SOUP_TYPE));
    }

    /**
     * 计算总体的食物属性。
     * @param containerFood 容器的食物属性
     * @param soupFood 汤的食物属性
     */
    public static SimpleFoodComponent computeFoodComponent(FoodComponent containerFood, FoodComponent soupFood) {
        int hunger = soupFood.getHunger() + containerFood.getHunger();
        float saturationModifier = (soupFood.getSaturationModifier() + containerFood.getSaturationModifier()) / 2;

        return new SimpleFoodComponent(hunger, saturationModifier);
    }

    /**
     * 直接获取当前的食物属性。
     * @param state 当前的方块状态
     * @return 当前的食物属性，如果对应的物品不是食物则返回空。
     */
    @Nullable
    protected static SimpleFoodComponent getFoodComponent(BlockState state) {
        BreadBoatContainer.BreadBoatSoupType soupType = state.get(SOUP_TYPE);
        FoodComponent containerFood = state.getBlock().asItem().getFoodComponent();
        FoodComponent soupFood = soupType.getFoodComponent();

        if (containerFood == null) {
            FoodCraft.LOGGER.warn("EdibleContainer must have food component!");
            return null;
        }

        return computeFoodComponent(containerFood, soupFood);
    }

    /**
     * 尝试喝一口汤。
     * @param player 喝汤的玩家
     */
    protected ActionResult tryDrinkSoup(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player) {
        int currentBites = state.get(BITES);

        // 播放喝汤声音
        if (currentBites < 3) {
            world.playSound(player, pos, SoundEvents.ENTITY_GENERIC_DRINK, player.getSoundCategory(), 1.0F, 1.0F);
        } else {
            world.playSound(player, pos, SoundEvents.ENTITY_GENERIC_EAT, player.getSoundCategory(), 1.0F, 1.0F);
        }

        SimpleFoodComponent food = getFoodComponent(state);
        if (food == null) {
            return ActionResult.FAIL;
        }

        // 恢复饥饿值和饱和度（每次喝汤恢复1/maxUse的比例）
        player.getHungerManager().add(
                food.Hunger / maxUse,
                food.SaturationModifier / (float) maxUse
        );

        world.emitGameEvent(player, GameEvent.EAT, pos);

        // 更新喝汤次数
        if (currentBites < maxUse) {
            // 还有剩余次数，增加喝汤次数
            world.setBlockState(pos, state.with(BITES, currentBites + 1), Block.NOTIFY_ALL);
        } else {
            // 喝完了，面包船被吃掉，不留下任何物品
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        int currentBites = state.get(BITES);
        SimpleFoodComponent food = getFoodComponent(state);

        // 如果已经被吃过，强制返还剩余饱食度
        if (!world.isClient && currentBites > 0 && food != null) {
            int remainingBites = maxUse - currentBites;
            int hunger = (food.Hunger / maxUse) * remainingBites;
            float saturation = (food.SaturationModifier / (float) maxUse) * remainingBites;

            player.getHungerManager().add(hunger, saturation);
            world.emitGameEvent(player, GameEvent.EAT, pos);
        }

        super.onBreak(world, pos, state, player);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        int bites = state.get(BITES);
        if (bites == 0){
            List<ItemStack> droppedStacks = super.getDroppedStacks(state, builder);
            droppedStacks.forEach(stack -> BreadBoatItem.serveSoup(stack, state.get(BreadBoatBlock.SOUP_TYPE)));

            return droppedStacks;
        }

        // 如果已经被食用，则不掉落任何物品
        return Collections.emptyList();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(IntPropertyManager.take(), SOUP_TYPE);
    }

    /**
     * 将食物属性简单的封装起来供临时使用。
     * @param Hunger 饱和度
     * @param SaturationModifier 食物属性修饰
     */
    public record SimpleFoodComponent(int Hunger, float SaturationModifier) {}
}