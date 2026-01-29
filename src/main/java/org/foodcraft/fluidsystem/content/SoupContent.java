package org.foodcraft.fluidsystem.content;

import net.minecraft.item.FoodComponent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class SoupContent extends AbstractContent{
    public final FoodComponent foodComponent;

    public SoupContent(Identifier id, FoodComponent foodComponent) {
        super(id);
        this.foodComponent = foodComponent;
    }

    @Override
    public @NotNull String getCategory() {
        return "soup";
    }
}
