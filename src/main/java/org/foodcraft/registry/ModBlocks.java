package org.foodcraft.registry;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.dfood.util.DFoodUtils;
import org.dfood.util.IntPropertyManager;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.*;
import org.foodcraft.block.entity.FlourSackBlockEntity;
import org.foodcraft.component.ModFoodComponents;

import java.util.function.BiFunction;

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
    public static final Block HEAT_RESISTANT_SLATE = registerBlock("heat_resistant_slate",
            new HeatResistantSlateBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(1.5F, 6.0F)));
    public static final Block COMBUSTION_FIREWOOD = registerBlock("combustion_firewood",
            new CombustionFirewoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(1.5F, 6.0F)
                    .nonOpaque().requiresTool().pistonBehavior(PistonBehavior.BLOCK)
                    .luminance(state -> state.get(CombustionFirewoodBlock.COMBUSTION_STATE).isBurning()? 15: 0)));
    public static final Block FIREWOOD = registerFoodBlock("firewood", 6,
            AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(1.5F, 6.0F)
                    .nonOpaque().requiresTool().pistonBehavior(PistonBehavior.BLOCK),
            (settings, integer) -> new FirewoodBlock(settings, integer, ModBlocks.COMBUSTION_FIREWOOD));

    // 工具
    public static final Block IRON_DISHES = registerBlock("iron_dishes",
            new DishesBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOD).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block WOODEN_SHELF = registerBlock("wooden_shelf",
            new ShelfBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOD).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block CAKE_EMBRYO_MOLD = registerBlock("cake_embryo_mold",
            new MoldBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY), true));
    public static final Block TOAST_MOLD = registerBlock("toast_mold",
            new MoldBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY), false));

    // 粉尘袋
    public static final Block FLOUR_SACK = registerFoodBlock("flour_sack", FlourSackBlockEntity.MAX_SACK_STACK,
            AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.5F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            FlourSackBlock::new);

    // 面食
    public static final Block DOUGH = registerBlock("dough",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block HARD_BREAD = registerBlock("hard_bread",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block SMALL_BREAD_EMBRYO = registerBlock("small_bread_embryo",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block SMALL_BREAD = registerBlock("small_bread",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block BAGUETTE = registerBlock("baguette",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block BAGUETTE_EMBRYO = registerBlock("baguette_embryo",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block FLUFFY_BREAD_EMBRYO = registerBlock("fluffy_bread_embryo",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block TOAST = registerBlock("toast",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block CAKE_EMBRYO = registerBlock("cake_embryo",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block HARD_BREAD_BOAT = registerBlock("hard_bread_boat",
            new SimpleFoodBlock(AbstractBlock.Settings.create().sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));

    // 过渡方块
    public static final Block BEETROOT_SOUP_HARD_BREAD_BOAT = registerAssistedBlock("crippled_beetroot_hard_bread_boat",
            AbstractBlock.Settings.create().sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            ((settings, maxUse) -> new CrippledHardBreadBoatBlock(settings, maxUse, ModFoodComponents.BEETROOT_SOUP_HARD_BREAD_BOAT, ModBlocks.HARD_BREAD_BOAT)), 4);
    public static final Block MUSHROOM_STEW_HARD_BREAD_BOAT = registerAssistedBlock("crippled_mushroom_hard_bread_boat",
            AbstractBlock.Settings.create().sounds(BlockSoundGroup.WOOL).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
            ((settings, maxUse) -> new CrippledHardBreadBoatBlock(settings, maxUse, ModFoodComponents.MUSHROOM_STEW_HARD_BREAD_BOAT, ModBlocks.HARD_BREAD_BOAT)), 4);

    // 调味料
    public static final Block SALT_SHAKER = registerBlock("salt_shaker",
            new SimpleFoodBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(0.2F)
                    .nonOpaque().pistonBehavior(PistonBehavior.DESTROY)));

    // 矿物
    public static final Block SALT_ORE = registerBlock("salt_ore",
            new ExperienceDroppingBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(1.5F, 6.0F).requiresTool()));
    public static final Block DEEPSLATE_SALT_ORE = registerBlock("deepslate_salt_ore",
            new ExperienceDroppingBlock(AbstractBlock.Settings.create()
                    .sounds(BlockSoundGroup.STONE).strength(2.0F, 6.0F).requiresTool()));

    public static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    /**
     * 注册食物方块
     * @param name 方块的id
     * @param foodValue 食物方块的最大堆叠数量
     * @param settings 方块设置
     * @param blockCreator 食物方块的构造函数
     * @return 注册后的方块
     */
    public static Block registerFoodBlock(String name, int foodValue, AbstractBlock.Settings settings,
                                          BiFunction<AbstractBlock.Settings, Integer, Block> blockCreator) {
        Block block = DFoodUtils.createFoodBlock(foodValue, settings, blockCreator);
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static Block registerAssistedBlock(String name, AbstractBlock.Settings settings,
                                              BiFunction<AbstractBlock.Settings, Integer, Block> blockCreator, int maxUse) {
        IntPropertyManager.preCache("number_of_use", maxUse);
        Block block = blockCreator.apply(settings, maxUse);
        return Registry.register(Registries.BLOCK, new Identifier(FoodCraft.MOD_ID, name), block);
    }

    public static void registerModBlocks() {}
}