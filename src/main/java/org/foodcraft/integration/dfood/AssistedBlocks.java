package org.foodcraft.integration.dfood;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.FoodComponents;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.dfood.block.FoodBlocks;
import org.dfood.sound.ModSoundGroups;
import org.dfood.util.IntPropertyManager;
import org.foodcraft.FoodCraft;
import org.foodcraft.item.ModPotions;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class AssistedBlocks {
    public static Set<Block> assistedBlocks = new HashSet<>();

    public static final Block CRIPPLED_RABBIT_STEW = registerAssistedStewBlock("crippled_rabbit_stew",
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
            settings -> new CrippledStewBlock(settings, FoodComponents.RABBIT_STEW, FoodBlocks.RABBIT_STEW));

    public static final Block CRIPPLED_MUSHROOM_STEW = registerAssistedStewBlock("crippled_mushroom_stew",
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
            settings -> new CrippledStewBlock(settings, FoodComponents.MUSHROOM_STEW, FoodBlocks.MUSHROOM_STEW));

    public static final Block CRIPPLED_BEETROOT_SOUP = registerAssistedStewBlock("crippled_beetroot_soup",
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
            settings -> new CrippledStewBlock(settings, FoodComponents.BEETROOT_SOUP, FoodBlocks.BEETROOT_SOUP));

    public static final Block CRIPPLED_SUSPICIOUS_STEW = registerAssistedStewBlock("crippled_suspicious_stew",
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BROWN).strength(0.1F, 0.1F).nonOpaque()
                    .sounds(BlockSoundGroup.DECORATED_POT).pistonBehavior(PistonBehavior.DESTROY),
            settings -> new CrippledSuspiciousStewBlock(settings, FoodComponents.SUSPICIOUS_STEW, FoodBlocks.SUSPICIOUS_STEW));

    public static final Block CRIPPLED_WATER_BUCKET = registerAssistedBlock("crippled_water_bucket",
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLUE).strength(0.2F, 0.2F).nonOpaque()
                    .sounds(ModSoundGroups.WATER_BUCKET).pistonBehavior(PistonBehavior.DESTROY),
            settings -> new CrippledWaterBucketBlock(settings, FoodBlocks.WATER_BUCKET, Potions.WATER), 3);
    public static final Block CRIPPLED_MILK_BUCKET = registerAssistedBlock("crippled_milk_bucket",
            AbstractBlock.Settings.create()
                    .mapColor(MapColor.BLUE).strength(0.2F, 0.2F).nonOpaque()
                    .sounds(ModSoundGroups.WATER_BUCKET).pistonBehavior(PistonBehavior.DESTROY),
            settings -> new CrippledWaterBucketBlock(settings, FoodBlocks.MILK_BUCKET, ModPotions.MILK), 3);

    public static Block registerAssistedStewBlock(String name, AbstractBlock.Settings settings,
                                                 Function<AbstractBlock.Settings, Block> blockCreator) {
        return registerAssistedBlock(name, settings, blockCreator, 4);
    }

    public static Block registerAssistedBlock(String name, AbstractBlock.Settings settings,
                                              Function<AbstractBlock.Settings, Block> blockCreator, int maxUse) {
        IntPropertyManager.preCache("number_of_use", maxUse);
        Block block = blockCreator.apply(settings);
        assistedBlocks.add(block);
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static void registerAssistedBlocks() {}
}