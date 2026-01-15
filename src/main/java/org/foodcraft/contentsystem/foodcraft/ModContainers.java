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
            createModId("bowl"),
            new ContainerType.ContainerSettings(Items.BOWL)
                    .setUseSound(ModSounds.SOUP_FILL)
    );
    // 硬面包船
    public static final BreadBoatContainer HARD_BREAD_BOAT = new BreadBoatContainer(
            createModId("hard_bread_boat"),
            new ContainerType.ContainerSettings(ModItems.HARD_BREAD_BOAT)
                    .setUseSound(ModSounds.SOUP_FILL)
    );
    // 瓶子
    public static final PotionContainer POTION = new PotionContainer(
            createModId("potion"),
            new ContainerType.ContainerSettings(Items.GLASS_BOTTLE)
    );
    // 桶
    public static final BucketContainer BUCKET = new BucketContainer(
            createModId("bucket"),
            new ContainerType.ContainerSettings(Items.BUCKET)
                    .setBaseCapacity(3)
                    .setUseSound(SoundEvents.ITEM_BUCKET_EMPTY)
    );
    //铁盘
    public static final DishesContainer IRON_PLATE = new DishesContainer(
            createModId("iron_plate"),
            new ContainerType.ContainerSettings(ModItems.IRON_PLATE)
                    .setUseSound(SoundEvents.ITEM_BUCKET_EMPTY)
    );

    private static Identifier createModId(String path) {
        return new Identifier(FoodCraft.MOD_ID, path);
    }

    public static void registryContainers() {
        initializeBowl();
        initializeBucket();
        initializePotion();
    }

    /**
     * 初始化汤映射
     */
    private static void initializeBowl() {
        // 蘑菇煲
        BOWL.registerContentMapping(ModContents.MUSHROOM_STEW, Items.MUSHROOM_STEW);

        // 甜菜汤
        BOWL.registerContentMapping(ModContents.BEETROOT_SOUP, Items.BEETROOT_SOUP);

        // 兔肉煲
        BOWL.registerContentMapping(ModContents.RABBIT_STEW, Items.RABBIT_STEW);
    }

    /**
     * 初始化桶的映射。
     */
    private static void initializeBucket() {
        // 水桶
        BUCKET.registerContentMapping(ModContents.WATER, Items.WATER_BUCKET);

        // 牛奶桶
        BUCKET.registerContentMapping(ModContents.MILK, Items.MILK_BUCKET);
    }

    /**
     * 初始化瓶子映射。
     */
    private static void initializePotion() {
        // 牛奶瓶
        POTION.registerContentMapping(ModContents.MILK, ModItems.MILK_POTION);
    }
}