package org.foodcraft.recipe.serializer;

import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.foodcraft.recipe.MoldRecipe;

public class MoldRecipeSerializer extends SimpleCraftRecipeSerializer<MoldRecipe> {
    public MoldRecipeSerializer() {
        super(MoldRecipeSerializer::createRecipe);
    }

    private static MoldRecipe createRecipe(Identifier id, Ingredient input, ItemStack output, Object extraData) {
        Item baseMoldItem = (Item) extraData;
        return new MoldRecipe(id, input, output, baseMoldItem);
    }

    @Override
    protected Object readExtraData(JsonObject json) {
        // 读取配方属于的基础模具
        String moldId = JsonHelper.getString(json, "baseMold");
        return Registries.ITEM.getOrEmpty(new Identifier(moldId))
                .orElseThrow(() -> new IllegalStateException("Item: " + moldId + " does not exist"));
    }

    @Override
    protected Object readExtraData(PacketByteBuf buf) {
        return buf.readItemStack().getItem();
    }

    @Override
    protected void writeExtraData(PacketByteBuf buf, MoldRecipe recipe) {
        buf.writeItemStack(new ItemStack(recipe.getBaseMoldItem()));
    }
}
