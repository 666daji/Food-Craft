package org.foodcraft.recipe.serializer;

import com.google.gson.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.foodcraft.item.FlourItem;
import org.foodcraft.recipe.DoughRecipe;

import java.util.HashMap;
import java.util.Map;

public class DoughRecipeSerializer implements RecipeSerializer<DoughRecipe> {
    @Override
    public DoughRecipe read(Identifier id, JsonObject json) {
        // 读取输出物品
        JsonObject outputObj = JsonHelper.getObject(json, "output");
        ItemStack output = new ItemStack(
                net.minecraft.registry.Registries.ITEM.get(new Identifier(
                        JsonHelper.getString(outputObj, "item")
                )),
                JsonHelper.getInt(outputObj, "count", 1)
        );

        // 读取面粉要求
        Map<FlourItem.FlourType, Integer> flourRequirements = new HashMap<>();
        JsonObject floursObj = JsonHelper.getObject(json, "flours");
        for (Map.Entry<String, JsonElement> entry : floursObj.entrySet()) {
            FlourItem.FlourType flourType = FlourItem.FlourType.fromId(entry.getKey());
            int count = entry.getValue().getAsInt();
            flourRequirements.put(flourType, count);
        }

        // 读取液体要求
        Map<String, Integer> liquidRequirements = new HashMap<>();
        JsonObject liquidsObj = JsonHelper.getObject(json, "liquids");
        for (Map.Entry<String, JsonElement> entry : liquidsObj.entrySet()) {
            liquidRequirements.put(entry.getKey(), entry.getValue().getAsInt());
        }

        // 读取额外物品要求 - 修复版本
        Map<Ingredient, Integer> extraRequirements = new HashMap<>();
        JsonObject extrasObj = JsonHelper.getObject(json, "extra_items", new JsonObject());

        // 方法1：支持数组格式（推荐）
        if (extrasObj.has("items") && extrasObj.get("items").isJsonArray()) {
            // 数组格式：支持相同物品的多个实例
            JsonArray itemsArray = extrasObj.getAsJsonArray("items");
            Map<String, Integer> itemCounts = new HashMap<>();

            for (JsonElement element : itemsArray) {
                JsonObject itemObj = element.getAsJsonObject();
                String itemId = JsonHelper.getString(itemObj, "item");
                int count = JsonHelper.getInt(itemObj, "count", 1);
                itemCounts.merge(itemId, count, Integer::sum);
            }

            // 创建Ingredient并合并数量
            for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                Identifier itemIdentifier = new Identifier(entry.getKey());
                Item item = Registries.ITEM.get(itemIdentifier);
                Ingredient ingredient = Ingredient.ofItems(item);
                extraRequirements.put(ingredient, entry.getValue());
            }
        } else {
            // 旧格式兼容（不推荐使用）
            for (Map.Entry<String, JsonElement> entry : extrasObj.entrySet()) {
                JsonObject itemObj = entry.getValue().getAsJsonObject();
                Ingredient ingredient = Ingredient.fromJson(itemObj);
                int count = JsonHelper.getInt(itemObj, "count", 1);
                extraRequirements.put(ingredient, count);
            }
        }

        return new DoughRecipe(id, output, flourRequirements, liquidRequirements, extraRequirements);
    }

    @Override
    public DoughRecipe read(Identifier id, PacketByteBuf buf) {
        // 读取输出
        ItemStack output = buf.readItemStack();

        // 读取面粉要求
        int flourCount = buf.readVarInt();
        Map<FlourItem.FlourType, Integer> flourRequirements = new HashMap<>(flourCount);
        for (int i = 0; i < flourCount; i++) {
            FlourItem.FlourType flourType = buf.readEnumConstant(FlourItem.FlourType.class);
            int count = buf.readVarInt();
            flourRequirements.put(flourType, count);
        }

        // 读取液体要求
        int liquidCount = buf.readVarInt();
        Map<String, Integer> liquidRequirements = new HashMap<>(liquidCount);
        for (int i = 0; i < liquidCount; i++) {
            String liquidType = buf.readString();
            int count = buf.readVarInt();
            liquidRequirements.put(liquidType, count);
        }

        // 读取额外物品要求
        int extraCount = buf.readVarInt();
        Map<Ingredient, Integer> extraRequirements = new HashMap<>(extraCount);
        for (int i = 0; i < extraCount; i++) {
            String itemId = buf.readString();
            int count = buf.readVarInt();
            Item item = Registries.ITEM.get(new Identifier(itemId));
            Ingredient ingredient = Ingredient.ofItems(item);
            extraRequirements.put(ingredient, count);
        }

        return new DoughRecipe(id, output, flourRequirements, liquidRequirements, extraRequirements);
    }

    @Override
    public void write(PacketByteBuf buf, DoughRecipe recipe) {
        // 写入输出
        buf.writeItemStack(recipe.getOutput(null));

        // 写入面粉要求
        buf.writeVarInt(recipe.getFlourRequirements().size());
        for (Map.Entry<FlourItem.FlourType, Integer> entry : recipe.getFlourRequirements().entrySet()) {
            buf.writeEnumConstant(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        // 写入液体要求
        buf.writeVarInt(recipe.getLiquidRequirements().size());
        for (Map.Entry<String, Integer> entry : recipe.getLiquidRequirements().entrySet()) {
            buf.writeString(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        // 写入额外物品要求
        buf.writeVarInt(recipe.getExtraRequirements().size());
        for (Map.Entry<Ingredient, Integer> entry : recipe.getExtraRequirements().entrySet()) {
            // 获取物品ID（简化处理，假设Ingredient只包含单个物品）
            ItemStack[] items = entry.getKey().getMatchingStacks();
            if (items.length > 0) {
                Identifier itemId = Registries.ITEM.getId(items[0].getItem());
                buf.writeString(itemId.toString());
                buf.writeVarInt(entry.getValue());
            }
        }
    }
}