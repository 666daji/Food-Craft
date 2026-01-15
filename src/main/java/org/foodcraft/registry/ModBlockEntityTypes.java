package org.foodcraft.registry;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.entity.*;

public class ModBlockEntityTypes {
    // 工作方块
    public static final BlockEntityType<GrindingStoneBlockEntity> GRINDING_STONE = create("grinding_stone",
            BlockEntityType.Builder.create(
                    GrindingStoneBlockEntity::new,
                    ModBlocks.GRINDING_STONE
            )
    );
    public static final BlockEntityType<PotteryTableBlockEntity> POTTERY_TABLE = create("pottery_table",
            BlockEntityType.Builder.create(
                    PotteryTableBlockEntity::new,
                    ModBlocks.POTTERY_TABLE
            )
    );
    public static final BlockEntityType<PotsBlockEntity> POTS = create("pots",
            BlockEntityType.Builder.create(
                    PotsBlockEntity::new,
                    ModBlocks.IRON_POTS,
                    ModBlocks.CLAY_POTS
            )
    );
    public static final BlockEntityType<PlateBlockEntity> PLATE = create("plate",
            BlockEntityType.Builder.create(
                    PlateBlockEntity::new,
                    ModBlocks.IRON_PLATE
            )
    );

    // UpPlaceBlock
    public static final BlockEntityType<HeatResistantSlateBlockEntity> HEAT_RESISTANT_SLATE = create("heat_resistant_slate",
            BlockEntityType.Builder.create(
                    HeatResistantSlateBlockEntity::new,
                    ModBlocks.HEAT_RESISTANT_SLATE
            )
    );
    public static final BlockEntityType<DishesBlockEntity> GARNISH_DISHES = create("dishes",
            BlockEntityType.Builder.create(
                    DishesBlockEntity::new,
                    ModBlocks.IRON_GARNISH_DISHES
            )
    );
    public static final BlockEntityType<ShelfBlockEntity> SHELF = create("shelf",
            BlockEntityType.Builder.create(
                    ShelfBlockEntity::new,
                    ModBlocks.WOODEN_SHELF
            )
    );
    public static final BlockEntityType<MoldBlockEntity> MOLD = create("mold",
            BlockEntityType.Builder.create(
                    MoldBlockEntity::new,
                    ModBlocks.CAKE_EMBRYO_MOLD,
                    ModBlocks.TOAST_EMBRYO_MOLD
            )
    );
    public static final BlockEntityType<CuttingBoardBlockEntity> CUTTING_BOARD = create("cutting_board",
            BlockEntityType.Builder.create(
                    CuttingBoardBlockEntity::new,
                    ModBlocks.CUTTING_BOARD
            )
    );

    // FoodBlock
    public static final BlockEntityType<FlourSackBlockEntity> FLOUR_SACK = create("flour_sack",
            BlockEntityType.Builder.create(
                    FlourSackBlockEntity::new,
                    ModBlocks.FLOUR_SACK
            )
    );

    // 其他
    public static final BlockEntityType<CombustionFirewoodBlockEntity> COMBUSTION_FIREWOOD = create("combustion_firewood",
            BlockEntityType.Builder.create(
                    CombustionFirewoodBlockEntity::new,
                    ModBlocks.COMBUSTION_FIREWOOD
            )
    );
    public static final BlockEntityType<BracketBlockEntity> BRACKET = create("bracket",
            BlockEntityType.Builder.create(
                    BracketBlockEntity::new,
                    ModBlocks.BRACKET
            )
    );

    private static <T extends BlockEntity> BlockEntityType<T> create(String id, BlockEntityType.Builder<T> builder) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(FoodCraft.MOD_ID, id), builder.build(null));
    }

    public static void registerBlockEntityTypes() {}
}
