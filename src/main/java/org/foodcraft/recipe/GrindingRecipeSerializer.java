package org.foodcraft.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class GrindingRecipeSerializer<T extends GrindingRecipe> implements RecipeSerializer<T> {
    private final RecipeFactory<T> recipeFactory;

    public GrindingRecipeSerializer(RecipeFactory<T> recipeFactory) {
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

        float experience = JsonHelper.getFloat(json, "experience", 0.0F);
        int grindingTime = JsonHelper.getInt(json, "grindingtime", 200);

        return this.recipeFactory.create(id, ingredient, result, experience, grindingTime);
    }

    @Override
    public T read(Identifier id, PacketByteBuf buf) {
        Ingredient ingredient = Ingredient.fromPacket(buf);
        ItemStack result = buf.readItemStack();
        float experience = buf.readFloat();
        int grindingTime = buf.readVarInt();
        return this.recipeFactory.create(id, ingredient, result, experience, grindingTime);
    }

    @Override
    public void write(PacketByteBuf buf, T recipe) {
        recipe.input.write(buf);
        buf.writeItemStack(recipe.output);
        buf.writeFloat(recipe.experience);
        buf.writeVarInt(recipe.grindingTime);
    }

    public interface RecipeFactory<T extends GrindingRecipe> {
        T create(Identifier id, Ingredient input, ItemStack output, float experience, int grindingTime);
    }
}