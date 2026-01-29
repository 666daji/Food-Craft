package org.foodcraft.contentsystem.content;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShapedDoughContent extends AbstractContent{
    private static final Table<Item, Block, ShapedDoughContent> CACHES = HashBasedTable.create();

    protected final Item originalDough;
    protected final Block baseMold;

    public ShapedDoughContent(Identifier id, Item originalDough, Block baseMold) {
        super(id);
        this.originalDough = originalDough;
        this.baseMold = baseMold;

        CACHES.put(originalDough, baseMold, this);
    }

    @Override
    public @NotNull String getCategory() {
        return ContentCategories.SHAPED_DOUGH;
    }

    public Block getBaseMold() {
        return baseMold;
    }

    public Item getOriginalDough() {
        return originalDough;
    }

    @Nullable
    public static ShapedDoughContent fromBaseGet(ItemStack originalDough, BlockState baseMold) {
        return CACHES.get(originalDough.getItem(), baseMold.getBlock());
    }
}
