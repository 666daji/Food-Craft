package org.foodcraft.recipe.serializer;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.occupy.OccupyUtil;
import org.foodcraft.contentsystem.registry.ContentRegistry;
import org.foodcraft.recipe.StoveRecipe;

import java.util.Objects;

public class StoveRecipeSerializer implements RecipeSerializer<StoveRecipe> {

    @Override
    public StoveRecipe read(Identifier id, JsonObject json) {
        // 读取输入物品（可以是普通物品或内容物）
        String inputString = JsonHelper.getString(json, "ingredient");
        ItemStack input = readStackFromString(inputString);

        // 读取结果物品（可以是普通物品或内容物）
        String resultString = JsonHelper.getString(json, "result");
        ItemStack result = readStackFromString(resultString);

        // 读取烘烤时间、最大输入数量和模具信息
        int inputCount = JsonHelper.getInt(json, "MaxInputCount", 1);
        int stoveTime = JsonHelper.getInt(json, "stoveTime", 200);

        return new StoveRecipe(id, input, result, inputCount, stoveTime);
    }

    @Override
    public StoveRecipe read(Identifier id, PacketByteBuf buf) {
        // 读取输入物品
        ItemStack input = buf.readItemStack();

        // 读取结果物品
        ItemStack result = buf.readItemStack();

        // 读取额外数据
        int inputCount = buf.readInt();
        int stoveTime = buf.readVarInt();

        return new StoveRecipe(id, input, result, inputCount, stoveTime);
    }

    @Override
    public void write(PacketByteBuf buf, StoveRecipe recipe) {
        // 写入输入物品
        buf.writeItemStack(recipe.getInput());

        // 写入结果物品
        buf.writeItemStack(recipe.getOutput(null));

        // 写入额外数据
        buf.writeInt(recipe.getMaxInputCount());
        buf.writeVarInt(recipe.getBakingTime());
    }

    /**
     * 从字符串中读取物品堆栈。
     * <p>使用'|'分割，格式为"类别|值"：</p>
     * <ul>
     *   <li>类别为"item"时：解析为普通的物品堆栈，值为物品ID（如"minecraft:apple"）</li>
     *   <li>类别为"content"时：从内容物注册表中获取对应的抽象内容物，然后使用{@link OccupyUtil#createAbstractOccupy(AbstractContent)}创建占位堆栈</li>
     * </ul>
     * <p>如果字符串中不包含'|'，则默认按普通物品处理。</p>
     *
     * @param idString 要解析的字符串
     * @return 解析后的物品堆栈
     * @throws IllegalArgumentException 如果无法解析出有效物品或内容物
     * @throws NullPointerException 如果输入字符串为null
     */
    private static ItemStack readStackFromString(String idString) {
        Objects.requireNonNull(idString, "Input string cannot be null");

        // 去除前后空格
        idString = idString.trim();

        // 如果不包含'|'，按普通物品处理
        if (!idString.contains("|")) {
            return parseItemStack(idString);
        }

        // 分割字符串
        String[] args = idString.split("\\|", 2); // 限制分割为2部分
        if (args.length != 2) {
            throw new IllegalArgumentException("Invalid string format: '" + idString + "'. Expected format: 'type|value'");
        }

        String type = args[0].trim().toLowerCase();
        String value = args[1].trim();

        return switch (type) {
            case "item" -> parseItemStack(value);
            case "content" -> parseContentStack(value);
            default -> throw new IllegalArgumentException("Unknown type: '" + type + "'. Expected 'item' or 'content'");
        };
    }

    /**
     * 解析普通物品堆栈
     * @param itemId 物品ID字符串
     * @return 物品堆栈
     * @throws IllegalArgumentException 如果物品不存在
     */
    private static ItemStack parseItemStack(String itemId) {
        Identifier identifier = Identifier.tryParse(itemId);
        if (identifier == null) {
            throw new IllegalArgumentException("Invalid item ID format: '" + itemId + "'");
        }

        var item = Registries.ITEM.getOrEmpty(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        return new ItemStack(item);
    }

    /**
     * 解析内容物堆栈
     * @param contentId 内容物ID字符串
     * @return 内容物占位堆栈
     * @throws IllegalArgumentException 如果内容物不存在
     */
    private static ItemStack parseContentStack(String contentId) {
        Identifier identifier = Identifier.tryParse(contentId);
        if (identifier == null) {
            throw new IllegalArgumentException("Invalid content ID format: '" + contentId + "'");
        }

        AbstractContent content = ContentRegistry.get(identifier);
        if (content == null) {
            throw new IllegalArgumentException("Content not found: " + contentId);
        }

        return OccupyUtil.createAbstractOccupy(content);
    }
}