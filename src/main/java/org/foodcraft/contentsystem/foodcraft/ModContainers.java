package org.foodcraft.contentsystem.foodcraft;

import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.contentsystem.container.*;
import org.foodcraft.registry.ModItems;
import org.foodcraft.registry.ModSounds;

public class ModContainers {
    // 碗
    public static final BowlContainer BOWL = new BowlContainer(
            new Identifier(FoodCraft.MOD_ID, "bowl"),
            new ContainerType.ContainerSettings(Items.BOWL)
                    .setUseSound(ModSounds.SOUP_FILL)
    );
    // 硬面包船
    public static final BreadBoatContainer HARD_BREAD_BOAT = new BreadBoatContainer(
            new Identifier(FoodCraft.MOD_ID, "hard_bread_boat"),
            new ContainerType.ContainerSettings(ModItems.HARD_BREAD_BOAT)
                    .setUseSound(ModSounds.SOUP_FILL)
    );
    // 瓶子
    public static final PotionContainer POTION = new PotionContainer(
            new Identifier(FoodCraft.MOD_ID, "potion"),
            new ContainerType.ContainerSettings(Items.GLASS_BOTTLE)
    );
    // 桶
    public static final BucketContainer BUCKET = new BucketContainer(
            new Identifier(FoodCraft.MOD_ID, "bucket"),
            new ContainerType.ContainerSettings(Items.BUCKET)
                    .setBaseCapacity(3)
                    .setUseSound(SoundEvents.ITEM_BUCKET_EMPTY)
    );

    public static void registryContainers() {}

}