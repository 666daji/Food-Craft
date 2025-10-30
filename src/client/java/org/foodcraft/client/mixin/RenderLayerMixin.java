package org.foodcraft.client.mixin;

import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import org.foodcraft.integration.dfood.AssistedBlocks;
import org.foodcraft.registry.ModBlocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RenderLayers.class)
public class RenderLayerMixin {
    @Shadow @Final private static Map<Block, RenderLayer> BLOCKS;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void init(CallbackInfo ci) {
        BLOCKS.put(AssistedBlocks.CRIPPLED_SUSPICIOUS_STEW, RenderLayer.getCutout());
        BLOCKS.put(ModBlocks.COMBUSTION_FIREWOOD, RenderLayer.getCutout());
        BLOCKS.put(ModBlocks.SALT_SHAKER, RenderLayer.getCutout());
        BLOCKS.put(ModBlocks.MILK_POTION, RenderLayer.getCutout());
    }
}
