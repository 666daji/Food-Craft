package org.foodcraft.registry;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.dfood.block.foodBlock;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.*;

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
    public static final Block WHEAT_FLOUR_SACK = registerBlock("wheat_flour_sack",
            new FlourSackBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),2));
    public static final Block LAPIS_LAZULI_FLOUR_SACK = registerBlock("lapis_lazuli_flour_sack",
            new FlourSackBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),2));
    public static final Block COCOA_FLOUR_SACK = registerBlock("cocoa_flour_sack",
            new FlourSackBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),2));
    public static final Block AMETHYST_FLOUR_SACK = registerBlock("amethyst_flour_sack",
            new FlourSackBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),2));
    public static final Block SUGAR_SACK = registerBlock("sugar_sack",
            new FlourSackBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),2));

    // 面食
    public static final Block DOUGH = registerBlock("dough",
            new foodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),1));
    public static final Block CAKE_EMBRYO = registerBlock("cake_embryo",
            new foodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY), 1));
    public static final Block CAKE_EMBRYO_MOLD = registerBlock("cake_embryo_mold",
            new MoldBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));

    // 调味料
    public static final Block SALT_SHAKER = registerBlock("salt_shaker",
            new foodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(0.2F)
                        .nonOpaque().pistonBehavior(PistonBehavior.DESTROY), 1));

    public static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static void registerModBlocks() {}
}