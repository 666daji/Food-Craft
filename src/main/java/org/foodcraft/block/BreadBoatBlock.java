package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import net.minecraft.util.StringIdentifiable;
import org.dfood.util.IntPropertyManager;

public class BreadBoatBlock extends SimpleFoodBlock {
    public static final EnumProperty<SoupType> SOUP_TYPE = EnumProperty.of("soup_type", SoupType.class);
    public final IntProperty BITES;
    public final int maxUse; // 最大使用次数

    public BreadBoatBlock(Settings settings, int maxUse) {
        super(settings);
        this.maxUse = maxUse;
        this.BITES = IntPropertyManager.create("bites", 0, maxUse);

        this.setDefaultState(this.getDefaultState()
                .with(SOUP_TYPE, SoupType.EMPTY)
                .with(BITES, 0));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);
        SoupType currentSoupType = state.get(SOUP_TYPE);

        // 如果面包船是空的，并且玩家手持的是可盛入的汤物品
        if (currentSoupType == SoupType.EMPTY) {
            for (SoupType soupType : SoupType.values()) {
                if (soupType != SoupType.EMPTY && handStack.getItem() == soupType.getSourceItem()) {
                    // 设置面包船为对应的汤状态，重置喝汤次数
                    world.setBlockState(pos, state.with(SOUP_TYPE, soupType).with(BITES, 0));

                    // 将玩家手中的物品更改为使用后的物品（碗）
                    if (!player.getAbilities().creativeMode) {
                        handStack.decrement(1);
                        ItemStack bowlStack = new ItemStack(Items.BOWL);
                        if (handStack.isEmpty()) {
                            player.setStackInHand(hand, bowlStack);
                        } else if (!player.getInventory().insertStack(bowlStack)) {
                            player.dropItem(bowlStack, false);
                        }
                    }

                    return ActionResult.SUCCESS;
                }
            }
        }
        // 如果面包船中有汤，并且玩家可以吃东西，则喝汤
        else if (player.canConsume(false)) {
            return tryDrinkSoup(world, pos, state, player);
        }

        // 调用父类方法处理其他交互（如食用空的面包船）
        return super.onUse(state, world, pos, player, hand, hit);
    }

    /**
     * 喝汤逻辑，参考CrippledStewBlock的实现
     */
    protected ActionResult tryDrinkSoup(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player) {
        SoupType soupType = state.get(SOUP_TYPE);
        int currentBites = state.get(BITES);

        if (!player.canConsume(false)) {
            return ActionResult.PASS;
        }

        // 播放喝汤声音
        world.playSound(player, pos, SoundEvents.ENTITY_GENERIC_DRINK, player.getSoundCategory(), 1.0F, 1.0F);

        // 恢复饥饿值和饱和度（每次喝汤恢复1/maxUse的比例）
        FoodComponent foodComponent = soupType.getFoodComponent();
        player.getHungerManager().add(foodComponent.getHunger() / maxUse, foodComponent.getSaturationModifier() / (float) maxUse);

        world.emitGameEvent(player, GameEvent.EAT, pos);

        // 更新喝汤次数
        if (currentBites < maxUse) {
            // 还有剩余次数，增加喝汤次数
            world.setBlockState(pos, state.with(BITES, currentBites + 1), Block.NOTIFY_ALL);
        } else {
            // 喝完了，面包船被吃掉，不留下任何物品
            world.breakBlock(pos, false);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // 如果面包船中有汤且已被使用过，强制玩家一次性食用完剩余饱食度
        SoupType soupType = state.get(SOUP_TYPE);
        if (!world.isClient && soupType != SoupType.EMPTY && state.get(BITES) > 0) {
            FoodComponent foodComponent = soupType.getFoodComponent();
            int remainingBites = maxUse - state.get(BITES);
            int hunger = (foodComponent.getHunger() / maxUse) * remainingBites;
            float saturation = (foodComponent.getSaturationModifier() / (float) maxUse) * remainingBites;
            player.getHungerManager().add(hunger, saturation);
            world.emitGameEvent(player, GameEvent.EAT, pos);
        }

        super.onBreak(world, pos, state, player);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(SOUP_TYPE, IntPropertyManager.take());
    }

    public enum SoupType implements StringIdentifiable {
        EMPTY("empty", null, null),
        BEETROOT_SOUP("beetroot_soup",
                new FoodComponent.Builder().hunger(6).saturationModifier(0.6f).build(),
                Items.BEETROOT_SOUP),
        MUSHROOM_STEW("mushroom_stew",
                new FoodComponent.Builder().hunger(6).saturationModifier(0.6f).build(),
                Items.MUSHROOM_STEW);

        private final String name;
        private final FoodComponent foodComponent;
        private final Item sourceItem;

        SoupType(String name, FoodComponent foodComponent, Item sourceItem) {
            this.name = name;
            this.foodComponent = foodComponent;
            this.sourceItem = sourceItem;
        }

        @Override
        public String asString() {
            return this.name;
        }

        public FoodComponent getFoodComponent() {
            return foodComponent;
        }

        public Item getSourceItem() {
            return sourceItem;
        }

        public static SoupType fromString(String name) {
            for (SoupType soupType : SoupType.values()) {
                if (soupType.name.equals(name)) {
                    return soupType;
                }
            }
            return EMPTY;
        }

        public boolean hasFoodComponent() {
            return this.foodComponent != null;
        }
    }
}