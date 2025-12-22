package org.foodcraft.recipe.serializer;

import com.google.gson.JsonArray;
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

public class CutRecipeSerializer implements RecipeSerializer<CutRecipe> {

    @Override
    public CutRecipe read(Identifier id, JsonObject json) {
        // 读取输入物品
        Ingredient input = Ingredient.fromJson(JsonHelper.getObject(json, "input"));

        // 读取输出物品 - 支持两种格式
        ItemStack output;
        if (json.get("output").isJsonObject()) {
            JsonObject outputObj = json.getAsJsonObject("output");
            output = new ItemStack(
                    JsonHelper.getItem(outputObj, "item"),
                    JsonHelper.getInt(outputObj, "count", 1)
            );
        } else {
            output = new ItemStack(
                    JsonHelper.getItem(json, "output"),
                    JsonHelper.getInt(json, "outputCount", 1)
            );
        }

        // 读取总切菜次数
        int totalCuts = JsonHelper.getInt(json, "totalCuts", 1);

        // 读取默认库存状态
        DefaultedList<ItemStack> defaultState = readInventoryState(
                JsonHelper.getObject(json, "defaultState", new JsonObject())
        );

        // 读取特定切菜次数的库存状态
        Map<Integer, DefaultedList<ItemStack>> cutStateMap = new HashMap<>();
        if (json.has("cutStates")) {
            JsonObject cutStates = JsonHelper.getObject(json, "cutStates");
            for (Map.Entry<String, JsonElement> entry : cutStates.entrySet()) {
                try {
                    int cutIndex = Integer.parseInt(entry.getKey());
                    JsonObject stateObject = entry.getValue().getAsJsonObject();
                    cutStateMap.put(cutIndex, readInventoryState(stateObject));
                } catch (NumberFormatException e) {
                    // 忽略无效的键
                }
            }
        }

        return new CutRecipe(id, input, output, totalCuts, cutStateMap, defaultState);
    }

    @Override
    public CutRecipe read(Identifier id, PacketByteBuf buf) {
        // 读取输入
        Ingredient input = Ingredient.fromPacket(buf);

        // 读取输出
        ItemStack output = buf.readItemStack();

        // 读取总切菜次数
        int totalCuts = buf.readVarInt();

        // 读取默认状态
        int defaultSize = buf.readVarInt();
        DefaultedList<ItemStack> defaultState = DefaultedList.ofSize(defaultSize, ItemStack.EMPTY);
        for (int i = 0; i < defaultSize; i++) {
            defaultState.set(i, buf.readItemStack());
        }

        // 读取特定状态
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

        return new CutRecipe(id, input, output, totalCuts, cutStateMap, defaultState);
    }

    @Override
    public void write(PacketByteBuf buf, CutRecipe recipe) {
        // 写入输入
        recipe.getInput().write(buf);

        // 写入输出
        buf.writeItemStack(recipe.getOutput());

        // 写入总切菜次数
        buf.writeVarInt(recipe.getTotalCuts());

        // 写入默认状态
        DefaultedList<ItemStack> defaultState = recipe.getDefaultState();
        buf.writeVarInt(defaultState.size());
        for (ItemStack stack : defaultState) {
            buf.writeItemStack(stack);
        }

        // 写入特定状态
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
     * 从JSON对象读取库存状态（5个槽位）
     * 格式: {"0": {"item": "...", "count": 1}, "2": {...}}
     * 没有指定的槽位默认为空
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
                // 忽略无效的槽位键
            }
        }

        return state;
    }

    /**
     * 旧方法：从JSON数组读取库存状态（保持向后兼容）
     */
    private DefaultedList<ItemStack> readInventoryStateFromArray(JsonArray array) {
        DefaultedList<ItemStack> state = DefaultedList.ofSize(5, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(array.size(), 5); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonNull()) {
                JsonObject obj = element.getAsJsonObject();
                state.set(i, new ItemStack(
                        JsonHelper.getItem(obj, "item"),
                        JsonHelper.getInt(obj, "count", 1)
                ));
            }
        }
        return state;
    }
}