package org.foodcraft.item;

import net.minecraft.item.Item;
import net.minecraft.util.StringIdentifiable;

import java.util.HashSet;
import java.util.Set;

/**
 * 面粉物品基类，所有面粉物品都应继承此类
 */
public class FlourItem extends Item {
    public static final Set<FlourItem> FLOURS = new HashSet<>();

    protected final int color;
    protected final FlourType flourType;

    public FlourItem(Settings settings, int color, FlourType flourType) {
        super(settings);
        this.color = color;
        this.flourType = flourType;
        FLOURS.add(this);
    }

    public int getColor() {
        return color;
    }

    public FlourType getFlourType() {
        return flourType;
    }

    public enum FlourType implements StringIdentifiable {
        WHEAT("wheat"),
        LAPIS_LAZULI("lapis_lazuli"),
        AMETHYST("amethyst"),
        COCOA("cocoa"),
        SUGAR("sugar"),
        SALT("salt");

        private final String id;

        FlourType(String id) {
            this.id = id;
        }

        @Override
        public String asString() {
            return id;
        }

        public static FlourType fromId(String id) {
            for (FlourType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return WHEAT;
        }
    }
}