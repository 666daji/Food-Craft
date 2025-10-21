package org.foodcraft.recipe.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.foodcraft.recipe.SimpleCraftRecipe;

public abstract class SimpleCraftRecipeSerializer<T extends SimpleCraftRecipe> implements RecipeSerializer<T> {
    private final RecipeFactory<T> recipeFactory;

    public SimpleCraftRecipeSerializer(RecipeFactory<T> recipeFactory) {
        this.recipeFactory = recipeFactory;
    }

    @Override
    public T read(Identifier id, JsonObject json) {
        JsonElement ingredientElement = JsonHelper.hasArray(json, "ingredient")
                ? JsonHelper.getArray(json, "ingredient")
                : JsonHelper.getObject(json, "ingredient");
        Ingredient ingredient = Ingredient.fromJson(ingredientElement, false);

        String resultId = JsonHelper.getString(json, "result");
        ItemStack result = new ItemStack(
                Registries.ITEM.getOrEmpty(new Identifier(resultId))
                        .orElseThrow(() -> new IllegalStateException("Item: " + resultId + " does not exist"))
        );

        // 调用子类方法读取额外数据
        Object extraData = readExtraData(json);

        return this.recipeFactory.create(id, ingredient, result, extraData);
    }

    @Override
    public T read(Identifier id, PacketByteBuf buf) {
        Ingredient ingredient = Ingredient.fromPacket(buf);
        ItemStack result = buf.readItemStack();

        // 调用子类方法从网络读取额外数据
        Object extraData = readExtraData(buf);

        return this.recipeFactory.create(id, ingredient, result, extraData);
    }

    @Override
    public void write(PacketByteBuf buf, T recipe) {
        recipe.getInput().write(buf);
        buf.writeItemStack(recipe.output);

        // 调用子类方法写入额外数据
        writeExtraData(buf, recipe);
    }

    /**
     * 从JSON读取配方特有的额外数据
     */
    protected abstract Object readExtraData(JsonObject json);

    /**
     * 从网络数据包读取配方特有的额外数据
     */
    protected abstract Object readExtraData(PacketByteBuf buf);

    /**
     * 将配方特有的额外数据写入网络数据包
     */
    protected abstract void writeExtraData(PacketByteBuf buf, T recipe);

    public interface RecipeFactory<T extends SimpleCraftRecipe> {
        T create(Identifier id, Ingredient input, ItemStack output, Object extraData);
    }
}