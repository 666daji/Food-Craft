package org.foodcraft.recipe.serializer;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.item.ItemStack;
import com.google.gson.JsonObject;
import org.foodcraft.recipe.GrindingRecipe;

public class GrindingRecipeSerializer extends SimpleCraftRecipeSerializer<GrindingRecipe> {

    public GrindingRecipeSerializer() {
        super(GrindingRecipeSerializer::createRecipe);
    }

    private static GrindingRecipe createRecipe(Identifier id, Ingredient input, ItemStack output, Object extraData) {
        GrindingExtraData data = (GrindingExtraData) extraData;
        return new GrindingRecipe(id, input, data.inputCount, output, data.grindingTime);
    }

    @Override
    protected Object readExtraData(JsonObject json) {
        int inputCount = JsonHelper.getInt(json, "inputCount", 1);
        int grindingTime = JsonHelper.getInt(json, "grindingTime", 200);
        return new GrindingExtraData(inputCount, grindingTime);
    }

    @Override
    protected Object readExtraData(PacketByteBuf buf) {
        int inputCount = buf.readVarInt();
        int grindingTime = buf.readVarInt();
        return new GrindingExtraData(inputCount, grindingTime);
    }

    @Override
    protected void writeExtraData(PacketByteBuf buf, GrindingRecipe recipe) {
        buf.writeVarInt(recipe.getInputCount());
        buf.writeVarInt(recipe.getGrindingTime());
    }

    /**
     * 封装额外数据
     * @param inputCount 成分数量
     * @param grindingTime 研磨时间
     */
    private record GrindingExtraData(int inputCount, int grindingTime) {}
}