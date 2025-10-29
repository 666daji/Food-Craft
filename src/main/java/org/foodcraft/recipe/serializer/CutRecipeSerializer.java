package org.foodcraft.recipe.serializer;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.foodcraft.recipe.CutRecipe;

public class CutRecipeSerializer extends SimpleCraftRecipeSerializer<CutRecipe>{
    public CutRecipeSerializer() {
        super(CutRecipeSerializer::createRecipe);
    }

    private static CutRecipe createRecipe(Identifier id, Ingredient input, ItemStack output, Object extraData) {
        int outputCount = (int) extraData;
        return new CutRecipe(id, input, output, outputCount);
    }

    @Override
    protected Object readExtraData(JsonObject json) {
        return JsonHelper.getInt(json, "outputCount", 1);
    }

    @Override
    protected Object readExtraData(PacketByteBuf buf) {
        return buf.readInt();
    }

    @Override
    protected void writeExtraData(PacketByteBuf buf, CutRecipe recipe) {
        buf.writeInt(recipe.getOutputCount());
    }
}
