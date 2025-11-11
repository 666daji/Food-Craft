package org.foodcraft.item;

import net.minecraft.item.Item;

import java.util.HashSet;
import java.util.Set;

public class FlourItem extends Item {
    public static final Set<FlourItem> FLOURS = new HashSet<>();

    protected final int color;

    public FlourItem(Settings settings, int color) {
        super(settings);
        this.color = color;
        FLOURS.add(this);
    }

    public int getColor() {
        return color;
    }
}
