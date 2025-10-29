package org.foodcraft.registry;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import org.foodcraft.FoodCraft;

public class ModOreGeneration {

    public static void registerOres() {
        // 注册盐矿生成
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                        new Identifier(FoodCraft.MOD_ID, "ore_salt_middle"))
        );

        // 注册小型盐矿生成
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                        new Identifier(FoodCraft.MOD_ID, "ore_salt_small"))
        );
    }
}