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

/**
 * <h1>面团配方序列化器</h1>
 *
 * <ul>
 *   <li>支持面粉类型（FlourType）到数量的映射</li>
 *   <li>支持液体类型到数量的映射</li>
 *   <li>支持额外物品的数组格式和旧格式</li>
 *   <li>自动合并相同物品的数量</li>
 * </ul>
 *
 * <h2>JSON格式示例</h2>
 * <pre>{@code
 * {
 *   "type": "foodcraft:dough_making",
 *   "output": {"item": "foodcraft:dough", "count": 1},
 *   "flours": {
 *     "wheat": 2,
 *     "rice": 1
 *   },
 *   "liquids": {
 *     "water": 1,
 *     "milk": 2
 *   },
 *   "extra_items": {
 *     "items": [
 *       {"item": "minecraft:sugar", "count": 1},
 *       {"item": "minecraft:egg", "count": 2},
 *       {"item": "minecraft:sugar", "count": 1}  // 会自动合并为count: 2
 *     ]
 *   }
 * }
 * }</pre>
 *
 * <h2>字段说明</h2>
 * <table border="1">
 *   <tr><th>字段</th><th>类型</th><th>必选</th><th>描述</th></tr>
 *   <tr><td>output</td><td>object</td><td>是</td><td>输出物品，包含item和count字段</td></tr>
 *   <tr><td>flours</td><td>object</td><td>是</td><td>面粉要求，键为面粉类型，值为数量</td></tr>
 *   <tr><td>liquids</td><td>object</td><td>是</td><td>液体要求，键为液体类型，值为数量</td></tr>
 *   <tr><td>extra_items</td><td>object</td><td>否</td><td>额外物品要求，推荐使用items数组格式</td></tr>
 * </table>
 *
 * @see DoughRecipe
 * @see RecipeSerializer
 * @see FlourItem.FlourType
 */
public class DoughRecipeSerializer implements RecipeSerializer<DoughRecipe> {

    /**
     * <h3>从JSON对象读取面团配方</h3>
     *
     * <p>该方法解析JSON数据，创建面团配方对象。支持两种额外物品格式：</p>
     * <ul>
     *   <li><b>推荐格式（数组）</b>: <code>"items": [{"item": "...", "count": X}, ...]</code></li>
     *   <li><b>旧格式（对象）</b>: <code>"item_name": {"item": "...", "count": X}</code></li>
     * </ul>
     *
     * <p>数组格式会自动合并相同物品的数量。</p>
     */
    @Override
    public DoughRecipe read(Identifier id, JsonObject json) {
        // 1. 读取输出物品
        JsonObject outputObj = JsonHelper.getObject(json, "output");
        ItemStack output = new ItemStack(
                Registries.ITEM.get(new Identifier(
                        JsonHelper.getString(outputObj, "item")
                )),
                JsonHelper.getInt(outputObj, "count", 1)
        );

        // 2. 读取面粉要求（面粉类型 -> 数量）
        Map<FlourItem.FlourType, Integer> flourRequirements = new HashMap<>();
        JsonObject floursObj = JsonHelper.getObject(json, "flours");
        for (Map.Entry<String, JsonElement> entry : floursObj.entrySet()) {
            FlourItem.FlourType flourType = FlourItem.FlourType.fromId(entry.getKey());
            int count = entry.getValue().getAsInt();
            flourRequirements.put(flourType, count);
        }

        // 3. 读取液体要求（液体类型 -> 数量）
        Map<String, Integer> liquidRequirements = new HashMap<>();
        JsonObject liquidsObj = JsonHelper.getObject(json, "liquids");
        for (Map.Entry<String, JsonElement> entry : liquidsObj.entrySet()) {
            liquidRequirements.put(entry.getKey(), entry.getValue().getAsInt());
        }

        // 4. 读取额外物品要求
        Map<Ingredient, Integer> extraRequirements = new HashMap<>();
        JsonObject extrasObj = JsonHelper.getObject(json, "extra_items", new JsonObject());

        // 方法1：支持数组格式（推荐）
        if (extrasObj.has("items") && extrasObj.get("items").isJsonArray()) {
            // 4.1 数组格式：支持相同物品的多个实例
            JsonArray itemsArray = extrasObj.getAsJsonArray("items");
            Map<String, Integer> itemCounts = new HashMap<>();

            // 统计每个物品的总数量
            for (JsonElement element : itemsArray) {
                JsonObject itemObj = element.getAsJsonObject();
                String itemId = JsonHelper.getString(itemObj, "item");
                int count = JsonHelper.getInt(itemObj, "count", 1);
                // 使用merge方法合并相同物品的数量
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
            // 4.2 旧格式兼容（不推荐使用）
            for (Map.Entry<String, JsonElement> entry : extrasObj.entrySet()) {
                JsonObject itemObj = entry.getValue().getAsJsonObject();
                Ingredient ingredient = Ingredient.fromJson(itemObj);
                int count = JsonHelper.getInt(itemObj, "count", 1);
                extraRequirements.put(ingredient, count);
            }
        }

        // 5. 创建并返回配方对象
        return new DoughRecipe(id, output, flourRequirements, liquidRequirements, extraRequirements);
    }

    @Override
    public DoughRecipe read(Identifier id, PacketByteBuf buf) {
        // 1. 读取输出物品
        ItemStack output = buf.readItemStack();

        // 2. 读取面粉要求
        int flourCount = buf.readVarInt();
        Map<FlourItem.FlourType, Integer> flourRequirements = new HashMap<>(flourCount);
        for (int i = 0; i < flourCount; i++) {
            FlourItem.FlourType flourType = buf.readEnumConstant(FlourItem.FlourType.class);
            int count = buf.readVarInt();
            flourRequirements.put(flourType, count);
        }

        // 3. 读取液体要求
        int liquidCount = buf.readVarInt();
        Map<String, Integer> liquidRequirements = new HashMap<>(liquidCount);
        for (int i = 0; i < liquidCount; i++) {
            String liquidType = buf.readString();
            int count = buf.readVarInt();
            liquidRequirements.put(liquidType, count);
        }

        // 4. 读取额外物品要求
        int extraCount = buf.readVarInt();
        Map<Ingredient, Integer> extraRequirements = new HashMap<>(extraCount);
        for (int i = 0; i < extraCount; i++) {
            String itemId = buf.readString();
            int count = buf.readVarInt();
            Item item = Registries.ITEM.get(new Identifier(itemId));
            Ingredient ingredient = Ingredient.ofItems(item);
            extraRequirements.put(ingredient, count);
        }

        // 5. 创建并返回配方对象
        return new DoughRecipe(id, output, flourRequirements, liquidRequirements, extraRequirements);
    }

    @Override
    public void write(PacketByteBuf buf, DoughRecipe recipe) {
        // 1. 写入输出物品
        buf.writeItemStack(recipe.getOutput(null));

        // 2. 写入面粉要求
        buf.writeVarInt(recipe.getFlourRequirements().size());
        for (Map.Entry<FlourItem.FlourType, Integer> entry : recipe.getFlourRequirements().entrySet()) {
            buf.writeEnumConstant(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        // 3. 写入液体要求
        buf.writeVarInt(recipe.getLiquidRequirements().size());
        for (Map.Entry<String, Integer> entry : recipe.getLiquidRequirements().entrySet()) {
            buf.writeString(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }

        // 4. 写入额外物品要求
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