package org.foodcraft.recipe.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.foodcraft.block.process.playeraction.PlayerAction;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.registry.ContentRegistry;
import org.foodcraft.recipe.PlatingRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * 摆盘配方序列化器，用于JSON格式的摆盘配方解析。
 *
 * <h2>JSON格式示例</h2>
 * <pre>{@code
 * {
 *   "type": "foodcraft:plating",
 *   "container": "foodcraft:iron_plate",
 *   "actions": [
 *     "add_item|minecraft:beef",
 *     "add_item|minecraft:sweet_berries",
 *   ],
 *   "result": "foodcraft:beef_berries_soup"
 * }
 * }</pre>
 *
 * <h2>字段说明</h2>
 * <table border="1">
 *   <tr><th>字段</th><th>类型</th><th>必需</th><th>描述</th></tr>
 *   <tr><td>type</td><td>string</td><td>是</td><td>配方类型，必须为"foodcraft:plating"</td></tr>
 *   <tr><td>container</td><td>string</td><td>是</td><td>容器物品ID</td></tr>
 *   <tr><td>actions</td><td>string[]</td><td>是</td><td>操作序列，每个字符串格式为"操作类型|参数1|参数2..."</td></tr>
 *   <tr><td>result</td><td>string</td><td>是</td><td>输出菜肴的内容ID</td></tr>
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

        // 2. 读取操作列表
        if (!json.has("actions")) {
            throw new JsonParseException("The plating recipe must contain an 'actions' field");
        }

        List<PlayerAction> actions = new ArrayList<>();
        for (JsonElement element : JsonHelper.getArray(json, "actions")) {
            String actionStr = element.getAsString();
            PlayerAction action = PlayerAction.fromString(actionStr);
            actions.add(action);
        }

        // 3. 读取输出结果
        String resultId = JsonHelper.getString(json, "result");
        Identifier result = Identifier.tryParse(resultId);
        if (result == null) {
            throw new JsonParseException("Invalid result ID: " + resultId);
        }

        AbstractContent output = ContentRegistry.get(result);
        if (output == null) {
            throw new JsonParseException("No content found: " + resultId);
        }

        // 4. 创建并返回配方对象
        return new PlatingRecipe(id, container, actions, output);
    }

    @Override
    public PlatingRecipe read(Identifier id, PacketByteBuf buf) {
        // 1. 读取容器物品
        Identifier containerId = buf.readIdentifier();
        Item container = Registries.ITEM.getOrEmpty(containerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown container item: " + containerId));

        // 2. 读取操作列表
        int actionCount = buf.readVarInt();
        List<PlayerAction> actions = new ArrayList<>(actionCount);
        for (int i = 0; i < actionCount; i++) {
            String actionStr = buf.readString();
            PlayerAction action = PlayerAction.fromString(actionStr);
            actions.add(action);
        }

        // 3. 读取输出结果
        AbstractContent output = ContentRegistry.get(buf.readIdentifier());
        if (output == null) {
            throw new IllegalArgumentException("No output found");
        }

        // 4. 创建并返回配方对象
        return new PlatingRecipe(id, container, actions, output);
    }

    @Override
    public void write(PacketByteBuf buf, PlatingRecipe recipe) {
        // 1. 写入容器物品ID
        buf.writeIdentifier(Registries.ITEM.getId(recipe.getContainer()));

        // 2. 写入操作列表
        List<PlayerAction> actions = recipe.getActions();
        buf.writeVarInt(actions.size());
        for (PlayerAction action : actions) {
            buf.writeString(action.toString());
        }

        // 3. 写入输出内容ID
        buf.writeIdentifier(recipe.getDishes().getId());
    }
}