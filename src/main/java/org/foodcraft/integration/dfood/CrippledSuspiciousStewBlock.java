package org.foodcraft.integration.dfood;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.dfood.block.entity.SuspiciousStewBlockEntity;
import org.jetbrains.annotations.Nullable;

public class CrippledSuspiciousStewBlock extends CrippledStewBlock implements BlockEntityProvider {
    public CrippledSuspiciousStewBlock(Settings settings, FoodComponent foodComponent, Block baseBlock) {
        super(settings, foodComponent, baseBlock);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SuspiciousStewBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        // 将物品NBT中的效果数据传递给方块实体
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SuspiciousStewBlockEntity suspiciousStewBlockEntity) {
            NbtCompound stackNbt = itemStack.getNbt();
            if (stackNbt != null) {
                suspiciousStewBlockEntity.readCustomDataFromItem(stackNbt);
            }
        }
    }

    @Override
    protected ActionResult tryUse(WorldAccess world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!player.canConsume(false)) {
            return ActionResult.PASS;
        }
        // 应用迷之炖菜的效果
        if (world instanceof World) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SuspiciousStewBlockEntity suspiciousStewBlockEntity) {
                // 应用所有存储的效果
                suspiciousStewBlockEntity.getEffectMap().forEach((effectId, duration) -> {
                    StatusEffect effect = StatusEffect.byRawId(effectId);
                    if (effect != null) {
                        player.addStatusEffect(new StatusEffectInstance(effect, (duration / 4) + 1));
                    }
                });
            }
        }
        // 调用父类的食用逻辑
        return super.tryUse(world, pos, state, player);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // 在破坏方块时也应用效果
        if (!world.isClient && state.get(NUMBER_OF_USE) > 0) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SuspiciousStewBlockEntity suspiciousStewBlockEntity) {
                // 应用所有存储的效果
                suspiciousStewBlockEntity.getEffectMap().forEach((effectId, duration) -> {
                    StatusEffect effect = StatusEffect.byRawId(effectId);
                    if (effect != null) {
                        int Duration = duration / 4;
                        int numberOfEat = state.get(NUMBER_OF_USE);
                        player.addStatusEffect(new StatusEffectInstance(effect, Duration * numberOfEat));
                    }
                });
            }
        }

        super.onBreak(world, pos, state, player);
    }
}