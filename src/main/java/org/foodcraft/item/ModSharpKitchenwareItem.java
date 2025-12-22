package org.foodcraft.item;

import net.fabricmc.yarn.constants.MiningLevels;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Lazy;
import net.minecraft.world.World;
import org.dfood.item.HaveBlock;
import org.dfood.util.DFoodUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class ModSharpKitchenwareItem extends SwordItem implements HaveBlock {
    protected final Block block;

    public ModSharpKitchenwareItem(Block block, Settings settings, SpatulaMaterials materials) {
        super(materials, materials.attackDamage, materials.miningSpeed, settings);
        this.block = block;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        Item item = context.getStack().getItem();
        // 仅当父类方法失败时才尝试放置方块
        if (super.useOnBlock(context) != ActionResult.PASS || (player != null && !player.isSneaking() && DFoodUtils.isModFoodItem(item))){
            return ActionResult.PASS;
        }
        ActionResult actionResult = this.place(new ItemPlacementContext(context));
        if (!actionResult.isAccepted() && this.isFood()) {
            ActionResult actionResult2 = this.use(context.getWorld(), context.getPlayer(), context.getHand()).getResult();
            return actionResult2 == ActionResult.CONSUME ? ActionResult.CONSUME_PARTIAL : actionResult2;
        } else {
            return actionResult;
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        this.getBlock().appendTooltip(stack, world, tooltip, context);
    }

    @Override
    public Block getBlock() {
        return this.block;
    }

    @Override
    public boolean canBeNested() {
        return !(this.block instanceof ShulkerBoxBlock);
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        if (this.block instanceof ShulkerBoxBlock) {
            ItemStack itemStack = entity.getStack();
            NbtCompound nbtCompound = HaveBlock.getBlockEntityNbt(itemStack);
            if (nbtCompound != null && nbtCompound.contains("Items", NbtElement.LIST_TYPE)) {
                NbtList nbtList = nbtCompound.getList("Items", NbtElement.COMPOUND_TYPE);
                ItemUsage.spawnItemContents(entity, nbtList.stream().map(NbtCompound.class::cast).map(ItemStack::fromNbt));
            }
        }
    }

    @Override
    public FeatureSet getRequiredFeatures() {
        return this.getBlock().getRequiredFeatures();
    }

    public enum SpatulaMaterials implements ToolMaterial {
        WOOD(MiningLevels.WOOD, 59, 2.0F, 0, 15, () -> Ingredient.fromTag(ItemTags.PLANKS)),
        STONE(MiningLevels.STONE, 130, 1.0F, 1, 5, () -> Ingredient.fromTag(ItemTags.STONE_TOOL_MATERIALS)),
        IRON(MiningLevels.IRON, 100, -3.5F, 4, 14, () -> Ingredient.ofItems(Items.IRON_INGOT)),
        DIAMOND(MiningLevels.DIAMOND, 1561, 8.0F, 3, 10, () -> Ingredient.ofItems(Items.DIAMOND)),
        GOLD(MiningLevels.WOOD, 32, 12.0F, 0, 22, () -> Ingredient.ofItems(Items.GOLD_INGOT));

        private final int miningLevel;
        private final int itemDurability;
        private final float miningSpeed;
        private final int attackDamage;
        private final int enchantability;
        private final Lazy<Ingredient> repairIngredient;

        SpatulaMaterials(int miningLevel, int itemDurability, float miningSpeed, int attackDamage, int enchantability, Supplier<Ingredient> repairIngredient) {
            this.miningLevel = miningLevel;
            this.itemDurability = itemDurability;
            this.miningSpeed = miningSpeed;
            this.attackDamage = attackDamage;
            this.enchantability = enchantability;
            this.repairIngredient = new Lazy<>(repairIngredient);
        }

        @Override
        public int getDurability() {
            return this.itemDurability;
        }

        @Override
        public float getMiningSpeedMultiplier() {
            return this.miningSpeed;
        }

        @Override
        public float getAttackDamage() {
            return this.attackDamage;
        }

        @Override
        public int getMiningLevel() {
            return this.miningLevel;
        }

        @Override
        public int getEnchantability() {
            return this.enchantability;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return this.repairIngredient.get();
        }
    }
}
