package org.foodcraft.contentsystem.content;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class BaseLiquidContent extends AbstractContent{
    public static final String CATEGORY = "base_liquid";

    public BaseLiquidContent(Identifier id) {
        super(id);
    }

    @Override
    public @NotNull String getCategory() {
        return CATEGORY;
    }
}
