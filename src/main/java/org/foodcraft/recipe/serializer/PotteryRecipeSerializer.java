package org.foodcraft.recipe.serializer;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.foodcraft.recipe.PotteryRecipe;

public class PotteryRecipeSerializer extends SimpleCraftRecipeSerializer<PotteryRecipe> {
    public PotteryRecipeSerializer() {
        super(PotteryRecipeSerializer::createRecipe);
    }

    private static PotteryRecipe createRecipe(Identifier id, Ingredient input, ItemStack output, Object extraData) {
        PotteryExtraData data = (PotteryExtraData) extraData;
        return new PotteryRecipe(id, input, output, data.inputCount, data.craftTime);
    }

    @Override
    protected Object readExtraData(JsonObject json) {
        int inputCount = JsonHelper.getInt(json, "inputCount", 1);
        int grindingTime = JsonHelper.getInt(json, "craftTime", 200);
        return new PotteryExtraData(inputCount, grindingTime);
    }

    @Override
    protected Object readExtraData(PacketByteBuf buf) {
        int inputCount = buf.readVarInt();
        int grindingTime = buf.readVarInt();
        return new PotteryExtraData(inputCount, grindingTime);
    }

    @Override
    protected void writeExtraData(PacketByteBuf buf, PotteryRecipe recipe) {
        buf.writeVarInt(recipe.getInputCount());
        buf.writeVarInt(recipe.getCraftTime());
    }

    /**
     * 封装额外数据
     * @param inputCount 成分数量
     * @param craftTime 研磨时间
     */
    private record PotteryExtraData(int inputCount, int craftTime) {}
}
