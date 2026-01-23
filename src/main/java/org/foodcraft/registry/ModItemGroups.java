package org.foodcraft.registry;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.item.BreadBoatItem;

public class ModItemGroups {
    public static final RegistryKey<ItemGroup> DEMO_GROUP = register("foodcraft");

    private static RegistryKey<ItemGroup> register(String id) {
        return RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(FoodCraft.MOD_ID, id));
    }

    private static void ModItemGroup(){
        Registry.register(
                Registries.ITEM_GROUP,
                DEMO_GROUP,
                ItemGroup.create(ItemGroup.Row.TOP, -1)
                        .displayName(Text.translatable("itemgroup.foodcraft"))
                        .icon(() -> new ItemStack(ModItems.GRINDING_STONE))
                        .entries(((displayContext, entries) -> {
                            // 工作方块
                            entries.add(ModItems.GRINDING_STONE);
                            entries.add(ModItems.HEAT_RESISTANT_SLATE);
                            entries.add(ModItems.FIREWOOD);
                            entries.add(ModItems.POTTERY_TABLE);
                            entries.add(ModItems.IRON_PLATE);

                            // 工具
                            entries.add(ModItems.IRON_GARNISH_DISHES);
                            entries.add(ModItems.WOODEN_SHELF);
                            entries.add(ModItems.CUTTING_BOARD);
                            entries.add(ModItems.IRON_POTS);
                            entries.add(ModItems.PLATE_LID);

                            // 厨具
                            entries.add(ModItems.BREAD_SPATULA);
                            entries.add(ModItems.KITCHEN_KNIFE);

                            // 粉尘
                            entries.add(ModItems.WHEAT_FLOUR);
                            entries.add(ModItems.LAPIS_LAZULI_FLOUR);
                            entries.add(ModItems.COCOA_FLOUR);
                            entries.add(ModItems.AMETHYST_FLOUR);
                            entries.add(ModItems.SUGAR_FLOUR);
                            entries.add(ModItems.SALT_FLOUR);

                            // 粉尘袋
                            entries.add(ModItems.FLOUR_SACK);

                            // 奶制品
                            entries.add(ModItems.MILK_POTION);

                            // 面食
                            entries.add(ModItems.DOUGH);
                            entries.add(ModItems.HARD_BREAD);
                            entries.add(ModItems.SMALL_BREAD_EMBRYO);
                            entries.add(ModItems.SMALL_BREAD);
                            entries.add(ModItems.BAGUETTE);
                            entries.add(ModItems.BAGUETTE_EMBRYO);
                            entries.add(ModItems.TOAST_DOUGH);
                            entries.add(ModItems.TOAST);
                            entries.add(ModItems.CAKE_DOUGH);
                            entries.add(ModItems.BAKED_CAKE_EMBRYO);
                            entries.add(ModItems.SALTY_DOUGH);
                            entries.add(ModItems.HARD_BREAD_BOAT);
                            entries.addAll(BreadBoatItem.getAll((BreadBoatItem) ModItems.HARD_BREAD_BOAT));
                            entries.add(ModItems.CARROT_SLICES);
                            entries.add(ModItems.CARROT_HEAD);
                            entries.add(ModItems.POTATO_CUBES);
                            entries.add(ModItems.BAKED_POTATO_CUBES);
                            entries.add(ModItems.APPLE_SLICES);
                            entries.add(ModItems.COD_CUBES);
                            entries.add(ModItems.COD_HEAD);
                            entries.add(ModItems.COOKED_COD_CUBES);
                            entries.add(ModItems.COOKED_COD_HEAD);
                            entries.add(ModItems.SALMON_CUBES);
                            entries.add(ModItems.COOKED_SALMON_CUBES);
                            entries.add(ModItems.KITCHEN_WASTE);

                            //模具
                            entries.add(ModItems.CAKE_EMBRYO_MOLD);
                            entries.add(ModItems.TOAST_EMBRYO_MOLD);
                            entries.add(ModItems.CAKE_DOUGH_CAKE_EMBRYO_MOLD);
                            entries.add(ModItems.TOAST_DOUGH_TOAST_EMBRYO_MOLD);

                            // 调味料
                            entries.add(ModItems.SALT_CUBES);

                            // 矿物
                            entries.add(ModItems.SALT_ORE);
                            entries.add(ModItems.DEEPSLATE_SALT_ORE);

                            // 陶制品
                            entries.add(ModItems.CLAY_POTS_EMBRYO);
                            entries.add(ModItems.CLAY_POTS);
                            entries.add(ModItems.FLOWER_POT_EMBRYO);
                        }))
                        .build()
        );
    }

    public static void RegistryModItemGroups(){
        ModItemGroup();
    }
}