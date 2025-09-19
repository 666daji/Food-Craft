package org.foodcraft.integration.dfood;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.FoodComponents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.dfood.block.FoodBlocks;
import org.foodcraft.FoodCraft;

import java.util.HashSet;
import java.util.Set;

public class AssistedBlocks {
    public static Set<Block> assistedBlocks = new HashSet<>();

    public static final Block CRIPPLED_RABBIT_STEW = registerBlock("crippled_rabbit_stew",
            new CrippledStewBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
                    FoodComponents.RABBIT_STEW, FoodBlocks.RABBIT_STEW));

    public static final Block CRIPPLED_MUSHROOM_STEW = registerBlock("crippled_mushroom_stew",
            new CrippledStewBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
                    FoodComponents.MUSHROOM_STEW, FoodBlocks.MUSHROOM_STEW));

    public static final Block CRIPPLED_BEETROOT_SOUP = registerBlock("crippled_beetroot_soup",
            new CrippledStewBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
                    FoodComponents.BEETROOT_SOUP, FoodBlocks.BEETROOT_SOUP));

    public static final Block CRIPPLED_SUSPICIOUS_STEW = registerBlock("crippled_suspicious_stew",
            new CrippledSuspiciousStewBlock(AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
                    FoodComponents.SUSPICIOUS_STEW, FoodBlocks.SUSPICIOUS_STEW));

    public static Block registerBlock(String name, Block block) {
        assistedBlocks.add(block);
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static void registerAssistedBlocks() {

    }
}
