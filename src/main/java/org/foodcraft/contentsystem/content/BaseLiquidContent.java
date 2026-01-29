package org.foodcraft.contentsystem.content;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class BaseLiquidContent extends AbstractContent{
    protected final int color;

    public BaseLiquidContent(Identifier id, int color) {
        super(id);
        this.color = color;
    }

    @Override
    public @NotNull String getCategory() {
        return ContentCategories.BASE_LIQUID;
    }

    public int getColor() {
        return color;
    }
}
