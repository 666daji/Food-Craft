package org.foodcraft.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;

public class ModBlocks {
    public static final Block BRACKET = registerBlock("bracket_block",
            new BracketBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOD)
                    .strength(0.2F)
                    .nonOpaque()
                    .pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block GRINDING_STONE = registerBlock("grinding_stone",
            new GrindingStoneBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE)
                    .strength(1.5F, 6.0F)
                    .nonOpaque()
                    .pistonBehavior(PistonBehavior.BLOCK)));

    public static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static void registerModBlocks() {

    }
}