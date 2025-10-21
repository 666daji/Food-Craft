package org.foodcraft.recipe.serializer;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.item.ItemStack;
import com.google.gson.JsonObject;
import org.foodcraft.recipe.StoveRecipe;

public class StoveRecipeSerializer extends SimpleCraftRecipeSerializer<StoveRecipe> {

    public StoveRecipeSerializer() {
        super(StoveRecipeSerializer::createRecipe);
    }

    private static StoveRecipe createRecipe(Identifier id, Ingredient input, ItemStack output, Object extraData) {
        StoveExtraData data = (StoveExtraData) extraData;
        return new StoveRecipe(id, input, output, data);
    }

    @Override
    protected Object readExtraData(JsonObject json) {
        // 读取烘烤时间，默认为200
        int stoveTime = JsonHelper.getInt(json, "stoveTime", 200);

        // 读取模具信息（可以为null）
        ItemStack mold = ItemStack.EMPTY;
        if (JsonHelper.hasString(json, "mold")) {
            String moldId = JsonHelper.getString(json, "mold");
            mold = new ItemStack(Registries.ITEM.getOrEmpty(new Identifier(moldId))
                    .orElseThrow(() -> new IllegalStateException("Item: " + moldId + " does not exist")));
        }
        return new StoveExtraData(stoveTime, mold);
    }

    @Override
    protected Object readExtraData(PacketByteBuf buf) {
        int stoveTime = buf.readVarInt();
        ItemStack mold = buf.readItemStack();
        return new StoveExtraData(stoveTime, mold);
    }

    @Override
    protected void writeExtraData(PacketByteBuf buf, StoveRecipe recipe) {
        buf.writeVarInt(recipe.getBakingTime());
        if (recipe.getMold() == null) {
            buf.writeItemStack(ItemStack.EMPTY);
            return;
        }
        buf.writeItemStack(recipe.getMold());
    }

    /**
     * 封装额外数据
     * @param stoveTime 烘烤时间
     * @param mold 基础模具
     */
    public record StoveExtraData(int stoveTime, ItemStack mold) {}
}