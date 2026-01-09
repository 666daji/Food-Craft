package org.foodcraft.recipe.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.foodcraft.recipe.PlatingRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * <h2>JSON格式示例</h2>
 * <pre>{@code
 * {
 *   "type": "foodcraft:plating",
 *   "container": "minecraft:bowl",
 *   "steps": [
 *     "minecraft:carrot",
 *     "minecraft:potato",
 *     "minecraft:beef"
 *   ],
 *   "result": {
 *     "item": "foodcraft:plated_meal",
 *     "count": 1
 *   }
 * }
 * }</pre>
 *
 * <h2>结构说明</h2>
 * <table border="1">
 *   <tr><th>字段</th><th>类型</th><th>描述</th></tr>
 *   <tr><td>container</td><td>string</td><td>容器物品ID</td></tr>
 *   <tr><td>steps</td><td>string[]</td><td>摆盘步骤的物品ID列表</td></tr>
 *   <tr><td>result</td><td>object</td><td>输出物品，包含item和count字段</td></tr>
 * </table>
 *
 * @see PlatingRecipe
 * @see RecipeSerializer
 */
public class PlatingRecipeSerializer implements RecipeSerializer<PlatingRecipe> {

    @Override
    public PlatingRecipe read(Identifier id, JsonObject json) {
        // 1. 读取容器物品
        String containerId = JsonHelper.getString(json, "container");
        Item container = Registries.ITEM.getOrEmpty(new Identifier(containerId))
                .orElseThrow(() -> new JsonParseException("Unknown container item: " + containerId));

        // 2. 读取步骤物品列表
        List<Item> steps = new ArrayList<>();
        for (JsonElement element : JsonHelper.getArray(json, "steps")) {
            String itemId = element.getAsString();
            Item item = Registries.ITEM.getOrEmpty(new Identifier(itemId))
                    .orElseThrow(() -> new JsonParseException("Unknown step item: " + itemId));
            steps.add(item);
        }

        // 3. 读取输出结果
        JsonObject resultObj = JsonHelper.getObject(json, "result");
        ItemStack output = ShapedRecipe.outputFromJson(resultObj);

        // 4. 创建并返回配方对象
        return new PlatingRecipe(id, container, steps, output);
    }

    @Override
    public PlatingRecipe read(Identifier id, PacketByteBuf buf) {
        // 1. 读取容器物品
        Identifier containerId = buf.readIdentifier();
        Item container = Registries.ITEM.getOrEmpty(containerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown container item: " + containerId));

        // 2. 读取步骤物品列表
        int stepCount = buf.readVarInt();
        List<Item> steps = new ArrayList<>(stepCount);
        for (int i = 0; i < stepCount; i++) {
            Identifier stepItemId = buf.readIdentifier();
            Item stepItem = Registries.ITEM.getOrEmpty(stepItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown step item: " + stepItemId));
            steps.add(stepItem);
        }

        // 3. 读取输出结果
        ItemStack output = buf.readItemStack();

        // 4. 创建并返回配方对象
        return new PlatingRecipe(id, container, steps, output);
    }

    @Override
    public void write(PacketByteBuf buf, PlatingRecipe recipe) {
        // 1. 写入容器物品ID
        buf.writeIdentifier(Registries.ITEM.getId(recipe.getContainer()));

        // 2. 写入步骤物品列表
        List<Item> steps = recipe.getSteps();
        buf.writeVarInt(steps.size());
        for (Item stepItem : steps) {
            buf.writeIdentifier(Registries.ITEM.getId(stepItem));
        }

        // 3. 写入输出物品堆
        buf.writeItemStack(recipe.getOutput());
    }
}