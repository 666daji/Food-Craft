package org.foodcraft.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.entity.MoldBlockEntity;

import java.util.HashMap;
import java.util.Map;

public class MoldContentItem extends BlockItem {
    public final Item contentItem;

    /**
     * 内容物对应含有内容物的模具的映射
     * <p>第一键的方块为对应的模具方块，二级键中的物品键为模具中的内容物，物品值为含有对应内容物的对应模具物品</p>
     */
    public static final Map<Block, Map<Item, Item>> MOLD_CONTENTS = new HashMap<>();

    public MoldContentItem(Block block, Settings settings, Item contentItem) {
        super(block, settings);
        this.contentItem = contentItem;
        if (MOLD_CONTENTS.containsKey(block)) {
            MOLD_CONTENTS.get(block).put(contentItem, this);
        }else {
            Map<Item, Item> contentsItem = new HashMap<>();
            contentsItem.put(contentItem, this);
            MOLD_CONTENTS.put(block, contentsItem);
        }
    }

    public static ItemStack getTargetStack(Block moldBlock, ItemStack contentStack){
        if (MOLD_CONTENTS.containsKey(moldBlock)){
            Map<Item, Item> contentItems = MOLD_CONTENTS.get(moldBlock);
            Item content = contentItems.get(contentStack.getItem());

            if (content instanceof MoldContentItem moldContentItem){
                return moldContentItem.createMoldItemStack(contentStack);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 将内容物堆栈转换为对应的含有该内容物的模具
     * @param contentStack 内容物堆栈
     * @return 对应的含有该内容物的堆栈，如果输入的内容物与{@link MoldContentItem#contentItem}不符则返回一个空堆栈
     */
    public ItemStack createMoldItemStack(ItemStack contentStack){
        if (contentStack.getItem() == contentItem){
            int count = contentStack.getCount();
            NbtCompound nbt = contentStack.getNbt();
            ItemStack newMoldStack = new ItemStack(this, count);

            if (nbt != null){
                newMoldStack.setNbt(nbt);
            }

            return newMoldStack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 将装有对应内容物的模具物品写入{@link MoldBlockEntity}
     * @param blockEntity 要写入的模具方块实体
     * @param placeStack 待写入的物品堆栈
     */
    public void toMoldBlock(MoldBlockEntity blockEntity, ItemStack placeStack) {
        if (placeStack.getItem() != this){
            return;
        }

        NbtCompound nbt = placeStack.getNbt();
        ItemStack stack = new ItemStack(contentItem);
        if (nbt != null){
            stack.setNbt(nbt);
        }

        blockEntity.tryAddItem(stack);
        blockEntity.tryCraft();
    }

    /**
     * 从模具物品堆栈中获取内容物
     */
    public ItemStack getContentStack(ItemStack moldStack) {
        if (contentItem == null || contentItem.equals(Items.AIR)) {
            return ItemStack.EMPTY;
        }

        ItemStack content = new ItemStack(contentItem);
        // 复制NBT数据（如果有）
        if (moldStack.hasNbt()) {
            if (moldStack.getNbt() != null) {
                content.setNbt(moldStack.getNbt().copy());
            }
        }
        return content;
    }

    /**
     * 检查模具物品是否装有内容物
     */
    public boolean hasContent(ItemStack stack) {
        return contentItem != null && !contentItem.equals(Items.AIR);
    }

    @Override
    public String getTranslationKey() {
        // 如果有内容物，返回包含内容物名称的翻译键
        if (contentItem != null && !contentItem.equals(Items.AIR)) {
            String contentName = Registries.ITEM.getId(contentItem).getPath();
            String moldName = Registries.BLOCK.getId(this.getBlock()).getPath();
            return "item." + FoodCraft.MOD_ID + "." + contentName + "_" + moldName;
        }

        // 没有内容物，返回默认的方块翻译键
        return super.getTranslationKey();
    }
}
