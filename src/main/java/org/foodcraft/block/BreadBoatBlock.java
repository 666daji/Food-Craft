package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import net.minecraft.util.StringIdentifiable;
import org.dfood.util.IntPropertyManager;
import org.foodcraft.component.ModFoodComponents;
import org.foodcraft.registry.ModBlocks;

import java.util.Collections;
import java.util.List;

public class BreadBoatBlock extends SimpleFoodBlock {
    public final IntProperty BITES;
    public final int maxUse;
    public final SoupType soupType;

    public BreadBoatBlock(Settings settings, VoxelShape shape, int maxUse, SoupType soupType) {
        super(settings, true, shape, false);
        this.maxUse = maxUse;
        this.soupType = soupType;

        this.BITES = IntPropertyManager.create("bites", 0, maxUse);

        this.setDefaultState(this.getDefaultState().with(BITES, 0));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack handStack = player.getStackInHand(hand);

        // 如果是空面包船，可以盛汤
        if (this.soupType == SoupType.EMPTY) {
            for (SoupType potentialSoupType : SoupType.values()) {
                if (potentialSoupType != SoupType.EMPTY && handStack.getItem() == potentialSoupType.getSourceItem()) {
                    // 获取对应的装满汤的面包船方块
                    Block fullBoatBlock = getFullBoatBlock(potentialSoupType);
                    if (fullBoatBlock != null) {
                        // 替换为装满汤的面包船方块
                        world.setBlockState(pos, fullBoatBlock.getDefaultState());

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
        }
        // 如果是装满汤的面包船
        else {
            int currentBites = state.get(BITES);

            // 如果玩家不能食用，返还对应的物品并破坏方块
            if (!player.canConsume(false)) {
                if (!world.isClient) {
                    // 掉落对应的装满汤的面包船物品
                    ItemStack itemStack = new ItemStack(asItem());
                    player.getInventory().offerOrDrop(itemStack);
                    // 破坏方块
                    world.breakBlock(pos, false);
                }
                return ActionResult.SUCCESS;
            }

            // 玩家可以食用，执行喝汤逻辑
            return tryDrinkSoup(world, pos, state, player);
        }

        // 调用父类方法处理其他交互（如食用空的面包船）
        return super.onUse(state, world, pos, player, hand, hit);
    }

    /**
     * 喝汤逻辑
     */
    protected ActionResult tryDrinkSoup(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player) {
        int currentBites = state.get(BITES);

        // 播放喝汤声音
        world.playSound(player, pos, SoundEvents.ENTITY_GENERIC_DRINK, player.getSoundCategory(), 1.0F, 1.0F);

        // 恢复饥饿值和饱和度（每次喝汤恢复1/maxUse的比例）
        player.getHungerManager().add(
                soupType.getFoodComponent().getHunger() / maxUse,
                soupType.getFoodComponent().getSaturationModifier() / (float) maxUse
        );

        world.emitGameEvent(player, GameEvent.EAT, pos);

        // 更新喝汤次数
        if (currentBites < maxUse) {
            // 还有剩余次数，增加喝汤次数
            world.setBlockState(pos, state.with(BITES, currentBites + 1), Block.NOTIFY_ALL);
        } else {
            // 喝完了，面包船被吃掉，不留下任何物品
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        int currentBites = state.get(BITES);

        // 如果已经被吃过，强制返还剩余饱食度
        if (!world.isClient && currentBites > 0 && this.soupType != SoupType.EMPTY) {
            int remainingBites = maxUse - currentBites;
            int hunger = (soupType.getFoodComponent().getHunger() / maxUse) * remainingBites;
            float saturation = (soupType.getFoodComponent().getSaturationModifier() / (float) maxUse) * remainingBites;
            player.getHungerManager().add(hunger, saturation);
            world.emitGameEvent(player, GameEvent.EAT, pos);
        }

        super.onBreak(world, pos, state, player);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        int bites = state.get(BITES);
        if (bites == 0){
            return super.getDroppedStacks(state, builder);
        }
        return Collections.emptyList();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(IntPropertyManager.take());
    }

    /**
     * 根据汤类型获取对应的装满汤的面包船方块
     */
    private Block getFullBoatBlock(SoupType soupType) {
        return switch (soupType) {
            case MUSHROOM_STEW -> ModBlocks.MUSHROOM_STEW_HARD_BREAD_BOAT;
            case BEETROOT_SOUP -> ModBlocks.BEETROOT_SOUP_HARD_BREAD_BOAT;
            default -> null;
        };
    }

    public enum SoupType implements StringIdentifiable {
        EMPTY("empty", null, null),
        BEETROOT_SOUP("beetroot_soup",
                ModFoodComponents.BEETROOT_SOUP_HARD_BREAD_BOAT,
                Items.BEETROOT_SOUP),
        MUSHROOM_STEW("mushroom_stew",
                ModFoodComponents.MUSHROOM_STEW_HARD_BREAD_BOAT,
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