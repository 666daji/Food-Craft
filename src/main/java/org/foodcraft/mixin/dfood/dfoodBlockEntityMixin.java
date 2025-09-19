package org.foodcraft.mixin.dfood;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import org.dfood.block.entity.ModBlockEntityTypes;
import org.dfood.block.entity.SuspiciousStewBlockEntity;
import org.dfood.block.FoodBlocks;
import org.foodcraft.integration.dfood.AssistedBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ModBlockEntityTypes.class)
public class dfoodBlockEntityMixin {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lorg/dfood/block/entity/ModBlockEntityTypes;create(Ljava/lang/String;Lnet/minecraft/block/entity/BlockEntityType$Builder;)Lnet/minecraft/block/entity/BlockEntityType;",
            ordinal = 1), index = 1)
    private static <T extends BlockEntity> BlockEntityType.Builder<T> registerBlockEntities(BlockEntityType.Builder<T> builder) {
        return (BlockEntityType.Builder<T>) BlockEntityType.Builder.create(SuspiciousStewBlockEntity::new, FoodBlocks.SUSPICIOUS_STEW, AssistedBlocks.CRIPPLED_SUSPICIOUS_STEW);
    }
}
