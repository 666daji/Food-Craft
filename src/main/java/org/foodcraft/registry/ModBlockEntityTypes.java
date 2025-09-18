package org.foodcraft.registry;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.entity.BracketBlockEntity;
import org.foodcraft.block.entity.DishesBlockEntity;
import org.foodcraft.block.entity.GrindingStoneBlockEntity;
import org.foodcraft.block.entity.ShelfBlockEntity;

public class ModBlockEntityTypes {
    public static final BlockEntityType<BracketBlockEntity> BRACKET = create("bracket",
            BlockEntityType.Builder.create(
                    BracketBlockEntity::new,
                    ModBlocks.BRACKET
            )
    );
    public static final BlockEntityType<GrindingStoneBlockEntity> GRINDING_STONE = create("grinding_stone",
            BlockEntityType.Builder.create(
                    GrindingStoneBlockEntity::new,
                    ModBlocks.GRINDING_STONE
            )
    );
    public static final BlockEntityType<DishesBlockEntity> DISHES = create("dishes",
            BlockEntityType.Builder.create(
                    DishesBlockEntity::new,
                    ModBlocks.IRON_DISHES
            )
    );

    public static final BlockEntityType<ShelfBlockEntity> SHELF = create("shelf",
            BlockEntityType.Builder.create(
                    ShelfBlockEntity::new,
                    ModBlocks.WOODEN_SHELF
            )
    );

    private static <T extends BlockEntity> BlockEntityType<T> create(String id, BlockEntityType.Builder<T> builder) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(FoodCraft.MOD_ID, id), builder.build(null));
    }

    public static void registerBlockEntityTypes() {}
}
