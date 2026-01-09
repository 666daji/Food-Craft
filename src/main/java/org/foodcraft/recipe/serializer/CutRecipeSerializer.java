package org.foodcraft.recipe.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import org.foodcraft.recipe.CutRecipe;

import java.util.HashMap;
import java.util.Map;

/**
 * <h1>切割配方序列化器</h1>
 *
 * <ul>
 *   <li>支持两种输出格式（对象格式和简写格式）</li>
 *   <li>管理切割过程中的中间状态</li>
 *   <li>处理5个槽位的库存状态</li>
 * </ul>
 *
 * <h2>JSON格式示例</h2>
 * <pre>{@code
 * {
 *   "type": "foodcraft:cutting",
 *   "input": {"item": "minecraft:carrot"},
 *   "output": {"item": "foodcraft:chopped_carrot", "count": 3},
 *   "totalCuts": 5,
 *   "defaultState": {
 *     "0": {"item": "foodcraft:carrot_chunk", "count": 1},
 *     "2": {"item": "foodcraft:carrot_slice", "count": 2}
 *   },
 *   "cutStates": {
 *     "2": {
 *       "0": {"item": "foodcraft:carrot_chunk", "count": 2},
 *       "3": {"item": "foodcraft:carrot_dice", "count": 1}
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>字段说明</h2>
 * <table border="1">
 *   <tr><th>字段</th><th>类型</th><th>必选</th><th>描述</th></tr>
 *   <tr><td>input</td><td>object</td><td>是</td><td>输入物品，使用Minecraft标准Ingredient格式</td></tr>
 *   <tr><td>output</td><td>object/string</td><td>是</td><td>输出物品，支持对象格式或简写格式</td></tr>
 *   <tr><td>totalCuts</td><td>integer</td><td>否</td><td>总切割次数，默认1</td></tr>
 *   <tr><td>defaultState</td><td>object</td><td>否</td><td>默认库存状态（5个槽位）</td></tr>
 *   <tr><td>cutStates</td><td>object</td><td>否</td><td>特定切割次数的库存状态映射</td></tr>
 * </table>
 *
 * @see CutRecipe
 * @see RecipeSerializer
 */
public class CutRecipeSerializer implements RecipeSerializer<CutRecipe> {

    @Override
    public CutRecipe read(Identifier id, JsonObject json) {
        // 1. 读取输入物品
        Ingredient input = Ingredient.fromJson(JsonHelper.getObject(json, "input"));

        // 2. 读取输出物品（支持两种格式）
        ItemStack output;
        if (json.get("output").isJsonObject()) {
            // 格式1：对象格式 - {"output": {"item": "...", "count": X}}
            JsonObject outputObj = json.getAsJsonObject("output");
            output = new ItemStack(
                    JsonHelper.getItem(outputObj, "item"),
                    JsonHelper.getInt(outputObj, "count", 1)
            );
        } else {
            // 格式2：简写格式 - {"output": "...", "outputCount": X}
            output = new ItemStack(
                    JsonHelper.getItem(json, "output"),
                    JsonHelper.getInt(json, "outputCount", 1)
            );
        }

        // 3. 读取总切菜次数
        int totalCuts = JsonHelper.getInt(json, "totalCuts", 1);

        // 4. 读取默认库存状态
        DefaultedList<ItemStack> defaultState = readInventoryState(
                JsonHelper.getObject(json, "defaultState", new JsonObject())
        );

        // 5. 读取特定切菜次数的库存状态映射
        Map<Integer, DefaultedList<ItemStack>> cutStateMap = new HashMap<>();
        if (json.has("cutStates")) {
            JsonObject cutStates = JsonHelper.getObject(json, "cutStates");
            for (Map.Entry<String, JsonElement> entry : cutStates.entrySet()) {
                try {
                    int cutIndex = Integer.parseInt(entry.getKey());
                    JsonObject stateObject = entry.getValue().getAsJsonObject();
                    cutStateMap.put(cutIndex, readInventoryState(stateObject));
                } catch (NumberFormatException e) {
                    // 忽略无效的键（非数字键）
                }
            }
        }

        // 6. 创建并返回配方对象
        return new CutRecipe(id, input, output, totalCuts, cutStateMap, defaultState);
    }

    @Override
    public CutRecipe read(Identifier id, PacketByteBuf buf) {
        // 1. 读取输入物品
        Ingredient input = Ingredient.fromPacket(buf);

        // 2. 读取输出物品
        ItemStack output = buf.readItemStack();

        // 3. 读取总切菜次数
        int totalCuts = buf.readVarInt();

        // 4. 读取默认库存状态
        int defaultSize = buf.readVarInt();
        DefaultedList<ItemStack> defaultState = DefaultedList.ofSize(defaultSize, ItemStack.EMPTY);
        for (int i = 0; i < defaultSize; i++) {
            defaultState.set(i, buf.readItemStack());
        }

        // 5. 读取特定切菜次数的库存状态映射
        int stateCount = buf.readVarInt();
        Map<Integer, DefaultedList<ItemStack>> cutStateMap = new HashMap<>();
        for (int i = 0; i < stateCount; i++) {
            int cutIndex = buf.readVarInt();
            int stateSize = buf.readVarInt();
            DefaultedList<ItemStack> state = DefaultedList.ofSize(stateSize, ItemStack.EMPTY);
            for (int j = 0; j < stateSize; j++) {
                state.set(j, buf.readItemStack());
            }
            cutStateMap.put(cutIndex, state);
        }

        // 6. 创建并返回配方对象
        return new CutRecipe(id, input, output, totalCuts, cutStateMap, defaultState);
    }

    @Override
    public void write(PacketByteBuf buf, CutRecipe recipe) {
        // 1. 写入输入物品
        recipe.getInput().write(buf);

        // 2. 写入输出物品
        buf.writeItemStack(recipe.getOutput());

        // 3. 写入总切菜次数
        buf.writeVarInt(recipe.getTotalCuts());

        // 4. 写入默认库存状态
        DefaultedList<ItemStack> defaultState = recipe.getDefaultState();
        buf.writeVarInt(defaultState.size());
        for (ItemStack stack : defaultState) {
            buf.writeItemStack(stack);
        }

        // 5. 写入特定切菜次数的库存状态映射
        Map<Integer, DefaultedList<ItemStack>> cutStateMap = recipe.getCutStateMap();
        buf.writeVarInt(cutStateMap.size());
        for (Map.Entry<Integer, DefaultedList<ItemStack>> entry : cutStateMap.entrySet()) {
            buf.writeVarInt(entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (ItemStack stack : entry.getValue()) {
                buf.writeItemStack(stack);
            }
        }
    }

    /**
     * <h3>从JSON对象读取库存状态（5个槽位）</h3>
     *
     * <p>读取格式：<code>{"0": {"item": "...", "count": 1}, "2": {...}}</code></p>
     * <p>没有指定的槽位默认为空物品堆。</p>
     *
     * @param jsonObject JSON对象，包含槽位索引到物品的映射
     * @return DefaultedList<ItemStack> 包含5个槽位的库存状态
     */
    private DefaultedList<ItemStack> readInventoryState(JsonObject jsonObject) {
        DefaultedList<ItemStack> state = DefaultedList.ofSize(5, ItemStack.EMPTY);

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            try {
                int slot = Integer.parseInt(entry.getKey());
                if (slot >= 0 && slot < 5) {
                    JsonObject itemObj = entry.getValue().getAsJsonObject();
                    state.set(slot, new ItemStack(
                            JsonHelper.getItem(itemObj, "item"),
                            JsonHelper.getInt(itemObj, "count", 1)
                    ));
                }
            } catch (NumberFormatException e) {
                // 忽略无效的槽位键（非数字键）
            }
        }

        return state;
    }
}