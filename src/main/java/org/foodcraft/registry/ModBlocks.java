package org.foodcraft.registry;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.dfood.block.FoodBlock;
import org.dfood.util.DFoodUtils;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.*;

import java.util.function.Function;

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

    // 工具
    public static final Block IRON_DISHES = registerBlock("iron_dishes",
            new DishesBlock(AbstractBlock.Settings.create()
                .sounds(BlockSoundGroup.WOOD).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block WOODEN_SHELF = registerBlock("wooden_shelf",
            new ShelfBlock(AbstractBlock.Settings.create()
                .sounds(BlockSoundGroup.WOOD).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));

    // 粉尘袋
    public static final Block WHEAT_FLOUR_SACK = registerFoodBlock("wheat_flour_sack", 2,
            AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            settings -> new FlourSackBlock(settings, 2));
    public static final Block LAPIS_LAZULI_FLOUR_SACK = registerFoodBlock("lapis_lazuli_flour_sack", 2,
            AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            settings -> new FlourSackBlock(settings, 2));
    public static final Block COCOA_FLOUR_SACK = registerFoodBlock("cocoa_flour_sack", 2,
            AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            settings -> new FlourSackBlock(settings, 2));
    public static final Block AMETHYST_FLOUR_SACK = registerFoodBlock("amethyst_flour_sack", 2,
            AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            settings -> new FlourSackBlock(settings, 2));
    public static final Block SUGAR_SACK = registerFoodBlock("sugar_sack", 2,
            AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            settings -> new FlourSackBlock(settings, 2));

    // 面食
    public static final Block DOUGH = registerFoodBlock("dough", 1,
            AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY), settings -> new FoodBlock(settings,1));
    public static final Block CAKE_EMBRYO = registerFoodBlock("cake_embryo", 1,
            AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY), settings -> new FoodBlock(settings, 1));
    public static final Block CAKE_EMBRYO_MOLD = registerBlock("cake_embryo_mold",
            new MoldBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));

    // 调味料
    public static final Block SALT_SHAKER = registerFoodBlock("salt_shaker", 1,
            AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY), settings -> new FoodBlock(settings, 1));

    public static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static Block registerFoodBlock(String name, int foodValue, AbstractBlock.Settings settings,
                                          Function<AbstractBlock.Settings, Block> blockCreator) {
        Block block = DFoodUtils.createFoodBlock(foodValue, settings, blockCreator);
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static void registerModBlocks() {}
}