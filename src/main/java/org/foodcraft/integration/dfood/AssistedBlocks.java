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
import org.dfood.util.IntPropertyManager;
import org.foodcraft.FoodCraft;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class AssistedBlocks {
    public static Set<Block> assistedBlocks = new HashSet<>();

    public static final Block CRIPPLED_RABBIT_STEW = registerAssistedBlock("crippled_rabbit_stew",
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
            settings -> new CrippledStewBlock(settings, FoodComponents.RABBIT_STEW, FoodBlocks.RABBIT_STEW));

    public static final Block CRIPPLED_MUSHROOM_STEW = registerAssistedBlock("crippled_mushroom_stew",
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
            settings -> new CrippledStewBlock(settings, FoodComponents.MUSHROOM_STEW, FoodBlocks.MUSHROOM_STEW));

    public static final Block CRIPPLED_BEETROOT_SOUP = registerAssistedBlock("crippled_beetroot_soup",
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
            settings -> new CrippledStewBlock(settings, FoodComponents.BEETROOT_SOUP, FoodBlocks.BEETROOT_SOUP));

    public static final Block CRIPPLED_SUSPICIOUS_STEW = registerAssistedBlock("crippled_suspicious_stew",
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
            settings -> new CrippledSuspiciousStewBlock(settings, FoodComponents.SUSPICIOUS_STEW, FoodBlocks.SUSPICIOUS_STEW));

    public static Block registerAssistedBlock(String name, AbstractBlock.Settings settings,
                                              Function<AbstractBlock.Settings, Block> blockCreator) {
        IntPropertyManager.preCache("number_of_use", 4);
        Block block = blockCreator.apply(settings);
        assistedBlocks.add(block);
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static void registerAssistedBlocks() {}
}