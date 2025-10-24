package org.foodcraft.client.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.entity.FlourSackBlockEntity;
import org.foodcraft.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockColors.class)
public class BlockColorsMixin {

    @Unique
    private static final Logger LOGGER = FoodCraft.LOGGER;

    @Inject(method = "create", at = @At("RETURN"))
    private static void registerFlourSackColor(CallbackInfoReturnable<BlockColors> cir) {
        BlockColors blockColors = cir.getReturnValue();
        blockColors.registerColorProvider(BlockColorsMixin::getFlourSackColor, ModBlocks.FLOUR_SACK);
    }

    @Unique
    private static int getFlourSackColor(BlockState state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex) {
        // 记录输入的tintIndex
        LOGGER.info("getFlourSackColor被调用 - tintIndex: {}, world: {}, pos: {}",
                tintIndex, world != null ? "非空" : "空", pos != null ? pos.toString() : "空");

        // 检查必要的参数
        if (world == null || pos == null) {
            LOGGER.info("参数检查失败: world或pos为null，返回默认颜色-1");
            return -1;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof FlourSackBlockEntity flourSackBlockEntity)) {
            LOGGER.info("在位置{}未找到FlourSackBlockEntity，返回默认颜色-1", pos);
            return -1;
        }

        // 获取堆叠数并记录
        int sackCount = flourSackBlockEntity.getSackCount();
        LOGGER.info("粉尘袋堆叠数: {}, 请求的tintIndex: {}", sackCount, tintIndex);

        // 根据tintIndex获取对应位置的粉尘颜色
        if (tintIndex >= 0 && tintIndex < sackCount) {
            int color = flourSackBlockEntity.getFlourColor(tintIndex);
            LOGGER.info("成功获取索引{}的颜色: #{}", tintIndex, Integer.toHexString(color).toUpperCase());
            return color;
        }

        // 如果tintIndex超出范围
        LOGGER.info("tintIndex {}超出有效范围(0-{})，返回默认颜色-1", tintIndex, sackCount - 1);
        return -1;
    }
}