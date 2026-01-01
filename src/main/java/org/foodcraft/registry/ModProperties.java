package org.foodcraft.registry;

import net.minecraft.state.property.EnumProperty;
import org.foodcraft.util.enums.SoupType;

public class ModProperties {
    public static final EnumProperty<SoupType> SOUP_TYPE = EnumProperty.of("soup_type", SoupType.class);
}
