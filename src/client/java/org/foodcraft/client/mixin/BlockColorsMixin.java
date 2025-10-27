package org.foodcraft.client.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.foodcraft.block.FlourSackBlock;
import org.foodcraft.block.entity.FlourSackBlockEntity;
import org.foodcraft.block.entity.ShelfBlockEntity;
import org.foodcraft.item.FlourItem;
import org.foodcraft.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockColors.class)
public class BlockColorsMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void registerFlourSackColor(CallbackInfoReturnable<BlockColors> cir) {
        BlockColors blockColors = cir.getReturnValue();
        blockColors.registerColorProvider(BlockColorsMixin::getFlourSackColor, ModBlocks.FLOUR_SACK);
    }

    @Unique
    private static int getFlourSackColor(BlockState state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex) {

        // 检查必要的参数
        if (world == null || pos == null) {
            return -1;
        }

        // 检查是否是放在木架子上的粉尘袋
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof ShelfBlockEntity shelfBlockEntity) {
            // 处理架子上的粉尘袋染色
            return getShelfFlourSackColor(shelfBlockEntity, state, tintIndex);
        }
        else if (blockEntity instanceof FlourSackBlockEntity flourSackBlockEntity) {
            // 直接放置的粉尘袋
            return getDirectFlourSackColor(flourSackBlockEntity, tintIndex);
        }
        else {
            // 未知的方块实体类型
            return -1;
        }
    }

    @Unique
    private static int getShelfFlourSackColor(ShelfBlockEntity shelfBlockEntity, BlockState state, int tintIndex) {
        // 获取架子索引
        int shelfIndex = 0;
        if (state.contains(FlourSackBlock.SHELF_INDEX)) {
            shelfIndex = state.get(FlourSackBlock.SHELF_INDEX);
        }

        // 检查该槽位是否有粉尘袋
        if (shelfIndex < 0 || shelfIndex >= shelfBlockEntity.size()) {
            return -1;
        }

        ItemStack shelfStack = shelfBlockEntity.getStack(shelfIndex);
        if (shelfStack.isEmpty() || !(shelfStack.getItem() instanceof BlockItem blockItem) ||
                !(blockItem.getBlock() instanceof FlourSackBlock)) {
            return -1;
        }

        // 从架子的内容数据中获取粉尘颜色
        return getFlourColorFromShelf(shelfBlockEntity, shelfIndex, tintIndex);
    }

    @Unique
    private static int getDirectFlourSackColor(FlourSackBlockEntity flourSackBlockEntity, int tintIndex) {
        int sackCount = flourSackBlockEntity.getSackCount();

        // 根据tintIndex获取对应位置的粉尘颜色
        if (tintIndex >= 0 && tintIndex < sackCount) {
            return flourSackBlockEntity.getFlourColor(tintIndex);
        }

        // 如果tintIndex超出范围
        return -1;
    }

    @Unique
    private static int getFlourColorFromShelf(ShelfBlockEntity shelfBlockEntity, int shelfIndex, int tintIndex) {
        try {
            NbtList contentData = getContentDataFromShelf(shelfBlockEntity);
            if (contentData == null || shelfIndex >= contentData.size()) {
                return FlourSackBlockEntity.DEFAULT_FLOUR_COLOR;
            }

            NbtCompound content = contentData.getCompound(shelfIndex);
            if (content.getByte("Type") == 2) { // CONTENT_TYPE_FLOUR
                if (content.contains("flour")) {
                    NbtCompound flourNbt = content.getCompound("flour");
                    ItemStack flourStack = ItemStack.fromNbt(flourNbt);

                    if (!flourStack.isEmpty() && flourStack.getItem() instanceof FlourItem flourItem) {
                        return flourItem.getColor();
                    }
                }
            }

            return FlourSackBlockEntity.DEFAULT_FLOUR_COLOR;

        } catch (Exception e) {
            return FlourSackBlockEntity.DEFAULT_FLOUR_COLOR;
        }
    }

    @Unique
    private static NbtList getContentDataFromShelf(ShelfBlockEntity shelfBlockEntity) {
        return shelfBlockEntity.getContentData();
    }
}