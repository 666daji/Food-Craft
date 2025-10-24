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
import org.foodcraft.util.FoodCraftUtils;

public class ModItemGroups {
    public static final RegistryKey<ItemGroup> DEMO_GROUP = register("demogroup");

    // 注册物品组
    private static RegistryKey<ItemGroup> register(String id) {
        return RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(FoodCraft.MOD_ID, id));
    }

    // 自定义物品组
    private static void ModItemGroup(){
        Registry.register(
                Registries.ITEM_GROUP,
                DEMO_GROUP,
                ItemGroup.create(ItemGroup.Row.TOP, -1)
                        // 设置物品组名称
                        .displayName(Text.translatable("itemgroup.foodcraft"))
                        // 设置物品组图标
                        .icon(() -> new ItemStack(ModItems.GRINDING_STONE))
                        // 设置物品组物品
                        .entries(((displayContext, entries) -> {
                            // 工作方块
                            entries.add(ModItems.BRACKET);
                            entries.add(ModItems.GRINDING_STONE);
                            entries.add(ModItems.HEAT_RESISTANT_SLATE);
                            entries.add(ModItems.FIREWOOD);

                            // 工具
                            entries.add(ModItems.IRON_DISHES);
                            entries.add(ModItems.WOODEN_SHELF);
                            entries.add(ModItems.CAKE_EMBRYO_MOLD);

                            // 粉尘
                            entries.add(ModItems.WHEAT_FLOUR);
                            entries.add(ModItems.LAPIS_LAZULI_FLOUR);
                            entries.add(ModItems.COCOA_FLOUR);
                            entries.add(ModItems.AMETHYST_FLOUR);

                            // 粉尘袋
                            entries.add(ModItems.FLOUR_SACK);

                            // 面食
                            entries.add(ModItems.DOUGH);
                            entries.add(ModItems.HARD_BREAD);
                            entries.add(ModItems.SMALL_BREAD_EMBRYO);
                            entries.add(ModItems.SMALL_BREAD);
                            entries.add(ModItems.BAGUETTE);
                            entries.add(ModItems.BAGUETTE_EMBRYO);
                            entries.add(ModItems.CAKE_EMBRYO);

                            // 调味料
                            entries.add(ModItems.SALT_CUBES);
                            entries.add(ModItems.SALT_SHAKER);

                            // 矿物
                            entries.add(ModItems.SALT_ORE);
                            entries.add(ModItems.DEEPSLATE_SALT_ORE);

                            entries.add(FoodCraftUtils.getMilkPotion());
                        }))
                        .build()
        );
    }

    public static void RegistryModItemGroups(){
        ModItemGroup();
    }
}