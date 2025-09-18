package org.foodcraft.client.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import org.foodcraft.client.blockentity.DishesBlockEntityRenderer;
import org.foodcraft.client.blockentity.ShelfBlockEntityRenderer;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.foodcraft.client.blockentity.BracketBlockEntityRenderer;
import org.foodcraft.client.blockentity.GrindingStoneBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRendererFactories.class)
public abstract class BlockEntityRendererFactoriesMixin {
    @Shadow
    public static <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererFactory<T> factory) {
    }

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void registerBlockEntityRenderers(CallbackInfo ci) {
        register(ModBlockEntityTypes.BRACKET, BracketBlockEntityRenderer::new);
        register(ModBlockEntityTypes.GRINDING_STONE, GrindingStoneBlockEntityRenderer::new);
        register(ModBlockEntityTypes.DISHES, DishesBlockEntityRenderer::new);
        register(ModBlockEntityTypes.SHELF, ShelfBlockEntityRenderer::new);
    }
}
