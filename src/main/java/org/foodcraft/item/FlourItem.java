package org.foodcraft.item;

import net.minecraft.item.Item;

public class FlourItem extends Item {
    protected final int color;

    public FlourItem(Settings settings, int color) {
        super(settings);
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
