package org.foodcraft.block.entity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.ModBlocks;

public class ModBlockEntityTypes {
    public static final BlockEntityType<BracketBlockEntity> BRACKET = create("bracket",
            BlockEntityType.Builder.create(
                    BracketBlockEntity::new,
                    ModBlocks.BRACKET
            )
    );
    public static final BlockEntityType<GrindingStoneBlockEntity> GrindingStone = create("grinding_stone",
            BlockEntityType.Builder.create(
                    GrindingStoneBlockEntity::new,
                    ModBlocks.GRINDING_STONE
            )
    );

    private static <T extends BlockEntity> BlockEntityType<T> create(String id, BlockEntityType.Builder<T> builder) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(FoodCraft.MOD_ID, id), builder.build(null));
    }

    public static void registerAllBlockEntityTypes() {

    }
}
