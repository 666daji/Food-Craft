package org.foodcraft.registry;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.dfood.block.ComplexFoodBlock;
import org.dfood.block.FoodBlock;
import org.dfood.shape.FoodShapeHandle;
import org.dfood.sound.ModSoundGroups;
import org.dfood.util.IntPropertyManager;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.*;

import java.util.function.BiFunction;

public class ModBlocks {
    // 工作方块
    public static final Block BRACKET = registerBlock("bracket_block",
            new BracketBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOD).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block GRINDING_STONE = registerBlock("grinding_stone",
            new GrindingStoneBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(1.5F, 6.0F).requiresTool()
                    .nonOpaque().pistonBehavior(PistonBehavior.BLOCK)));
    public static final Block HEAT_RESISTANT_SLATE = registerBlock("heat_resistant_slate",
            new HeatResistantSlateBlock(AbstractBlock.Settings.create().requiresTool()
                    .sounds(BlockSoundGroup.STONE).strength(1.2F, 6.0F)));
    public static final Block COMBUSTION_FIREWOOD = registerBlock("combustion_firewood",
            new CombustionFirewoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(0.5F, 0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.BLOCK)
                    .luminance(state -> state.get(CombustionFirewoodBlock.COMBUSTION_STATE).isBurning()? 15: 0)));
    public static final Block FIREWOOD = registerBlock("firewood",
            FirewoodBlock.Builder.create()
                    .maxFood(6)
                    .targetBlock(COMBUSTION_FIREWOOD)
                    .settings(AbstractBlock.Settings.create()
                            .sounds(BlockSoundGroup.STONE).strength(0.5F, 2.0F)
                            .nonOpaque().requiresTool().pistonBehavior(PistonBehavior.BLOCK))
                    .build());
    public static final Block POTTERY_TABLE = registerBlock("pottery_table",
            new PotteryTableBlock(AbstractBlock.Settings.create().requiresTool()
                    .sounds(BlockSoundGroup.STONE).strength(1.2F, 6.0F)
                    .nonOpaque().pistonBehavior(PistonBehavior.BLOCK)));

    // 工具
    public static final Block IRON_DISHES = registerBlock("iron_dishes",
            new DishesBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOD).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block WOODEN_SHELF = registerBlock("wooden_shelf",
            new ShelfBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOD).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block CUTTING_BOARD = registerBlock("cutting_board",
            new CuttingBoardBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block IRON_POTS = registerBlock("iron_pots",
            new PotsBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(0.5F, 0.6F).nonOpaque()));
    public static final Block BREAD_SPATULA = registerBlock("bread_spatula",
            ComplexFoodBlock.Builder.create()
                    .maxFood(1)
                    .useItemTranslationKey(false)
                    .isFood(false)
                    .simpleShape(Block.createCuboidShape(0, 0, 0, 16, 1, 16))
                    .settings(AbstractBlock.Settings.create()
                            .sounds(BlockSoundGroup.STONE).strength(0.5F, 0.2F)
                            .nonOpaque().requiresTool().pistonBehavior(PistonBehavior.BLOCK))
                    .build());

    // 粉尘袋
    public static final Block FLOUR_SACK = registerBlock("flour_sack",
            FlourSackBlock.Builder.create()
                    .maxFood(2)
                    .settings(AbstractBlock.Settings.create()
                            .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                            .nonOpaque().pistonBehavior(PistonBehavior.DESTROY))
                    .build());

    // 奶制品
    public static final Block MILK_POTION = registerBlock("milk_potion",
            FoodBlock.Builder.create()
                    .maxFood(3)
                    .settings(AbstractBlock.Settings.create()
                            .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                            .nonOpaque().pistonBehavior(PistonBehavior.DESTROY))
                    .build());

    // 面食
    public static final Block DOUGH = registerBlock("dough",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block HARD_BREAD = registerBlock("hard_bread",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block SMALL_BREAD_EMBRYO = registerBlock("small_bread_embryo",
            FoodBlock.Builder.create()
                    .maxFood(3)
                    .useItemTranslationKey(false)
                    .settings(AbstractBlock.Settings.create()
                            .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                            .nonOpaque().pistonBehavior(PistonBehavior.DESTROY))
                    .build());
    public static final Block SMALL_BREAD = registerBlock("small_bread",
            FoodBlock.Builder.create()
                    .maxFood(2)
                    .useItemTranslationKey(false)
                    .settings(AbstractBlock.Settings.create()
                            .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                            .nonOpaque().pistonBehavior(PistonBehavior.DESTROY))
                    .build());
    public static final Block BAGUETTE = registerBlock("baguette",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block BAGUETTE_EMBRYO = registerBlock("baguette_embryo",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block TOAST_EMBRYO = registerBlock("toast_embryo",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block TOAST = registerBlock("toast",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block CAKE_EMBRYO = registerBlock("cake_embryo",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block BAKED_CAKE_EMBRYO = registerBlock("baked_cake_embryo",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block HARD_BREAD_BOAT = registerBreadBoatBlock("hard_bread_boat",
            AbstractBlock.Settings.create().sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            ((settings, integer) -> new BreadBoatBlock(settings, FoodShapeHandle.shapes.getShape(8), integer,
                    BreadBoatBlock.SoupType.EMPTY)), 4);
    public static final Block MUSHROOM_STEW_HARD_BREAD_BOAT = registerBreadBoatBlock("mushroom_stew_hard_bread_boat",
            AbstractBlock.Settings.create().sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            ((settings, integer) -> new BreadBoatBlock(settings, FoodShapeHandle.shapes.getShape(8),
                    integer, BreadBoatBlock.SoupType.MUSHROOM_STEW)), 4);
    public static final Block BEETROOT_SOUP_HARD_BREAD_BOAT = registerBreadBoatBlock("beetroot_soup_hard_bread_boat",
            AbstractBlock.Settings.create().sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            ((settings, integer) -> new BreadBoatBlock(settings, FoodShapeHandle.shapes.getShape(8),
                    integer, BreadBoatBlock.SoupType.BEETROOT_SOUP)), 4);

    // 模具
    public static final Block CAKE_EMBRYO_MOLD = registerBlock("cake_embryo_mold",
            new MoldBlock(AbstractBlock.Settings.create()
                    .sounds(ModSoundGroups.BUCKET).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY), true));
    public static final Block TOAST_EMBRYO_MOLD = registerBlock("toast_embryo_mold",
            new MoldBlock(AbstractBlock.Settings.create()
                    .sounds(ModSoundGroups.BUCKET).sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY), true));

    // 调味料
    public static final Block SALT_SHAKER = registerBlock("salt_shaker",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
                    true, null, false));

    // 矿物
    public static final Block SALT_ORE = registerBlock("salt_ore",
            new ExperienceDroppingBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(1.5F, 6.0F).requiresTool()));
    public static final Block DEEPSLATE_SALT_ORE = registerBlock("deepslate_salt_ore",
            new ExperienceDroppingBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(2.0F, 6.0F).requiresTool()));

    // 陶制品
    public static final Block CLAY_POTS_EMBRYO = registerBlock("clay_pots_embryo",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(0.5F, 0.6F).nonOpaque(),
                    false, PotsBlock.SHAPE));
    public static final Block CLAY_POTS = registerBlock("clay_pots",
            new PotsBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(0.5F, 0.6F).nonOpaque()));
    public static final Block FLOWER_POT_EMBRYO = registerBlock("flower_pot_embryo",
            FoodBlock.Builder.create()
                    .maxFood(4)
                    .useItemTranslationKey(false)
                    .settings(AbstractBlock.Settings.create()
                            .sounds(BlockSoundGroup.STONE).strength(0.5F, 0.6F).nonOpaque())
                    .build());
    public static final Block FLOWER_POT_COOKING = registerBlock("flower_pot_cooking",
            FoodBlock.Builder.create()
                .maxFood(4)
                .useItemTranslationKey(false)
                .settings(AbstractBlock.Settings.create()
                        .sounds(BlockSoundGroup.STONE).strength(0.5F, 0.6F).nonOpaque())
                .build());

    /**
     * 注册一般方块
     * @param name 方块id
     * @param block 方块
     * @return 成功注册的方块
     */
    public static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static Block registerBreadBoatBlock(String name, AbstractBlock.Settings settings,
                                               BiFunction<AbstractBlock.Settings, Integer, Block> blockCreator, int maxUse) {
        IntPropertyManager.preCache("bites", 0, maxUse);
        Block block = blockCreator.apply(settings, maxUse);
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static void registerModBlocks() {}
}