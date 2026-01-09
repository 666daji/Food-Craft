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

/**
 * <h1>简单合成配方序列化器（抽象基类）</h1>
 *
 * <p>提供通用的配方序列化框架，子类可以处理特定类型的额外数据。</p>
 *
 * <h2>设计模式</h2>
 * <p>使用<strong>模板方法模式</strong>，定义了序列化的基本流程，<br>
 * 子类通过实现抽象方法来处理特定类型的额外数据。</p>
 *
 * <h2>主要功能</h2>
 * <ul>
 *   <li>提供通用的原料和结果读取逻辑</li>
 *   <li>定义处理额外数据的抽象方法</li>
 *   <li>使用工厂模式创建配方对象</li>
 *   <li>支持JSON和网络两种序列化方式</li>
 * </ul>
 *
 * <h2>JSON格式示例</h2>
 * <pre>{@code
 * {
 *   "type": "foodcraft:drying",
 *   "ingredient": {"item": "minecraft:apple"},
 *   "result": "foodcraft:dried_apple",
 *   "dryingTime": 200,
 *   "temperature": "medium"
 * }
 * }</pre>
 *
 * <h2>字段说明</h2>
 * <table border="1">
 *   <tr><th>字段</th><th>类型</th><th>必选</th><th>描述</th></tr>
 *   <tr><td>type</td><td>string</td><td>是</td><td>配方类型标识符</td></tr>
 *   <tr><td>ingredient</td><td>object/array</td><td>是</td><td>输入原料，支持对象或数组格式</td></tr>
 *   <tr><td>result</td><td>string</td><td>是</td><td>输出物品ID</td></tr>
 *   <tr><td>extra fields</td><td>various</td><td>否</td><td>子类特有的额外数据字段</td></tr>
 * </table>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * public class DryingRecipeSerializer extends SimpleCraftRecipeSerializer<DryingRecipe> {
 *     public DryingRecipeSerializer() {
 *         super(DryingRecipe::new);
 *     }
 *
 *     @Override
 *     protected Object readExtraData(JsonObject json) {
 *         return new DryingRecipe.ExtraData(
 *             JsonHelper.getInt(json, "dryingTime", 100),
 *             JsonHelper.getString(json, "temperature", "low")
 *         );
 *     }
 *
 *     // 实现其他抽象方法...
 * }
 * }</pre>
 *
 * @param <T> 具体的配方类型，必须继承自SimpleCraftRecipe
 * @author FoodCraft Mod Team
 * @since 1.0.0
 * @see SimpleCraftRecipe
 * @see RecipeSerializer
 */
public abstract class SimpleCraftRecipeSerializer<T extends SimpleCraftRecipe> implements RecipeSerializer<T> {

    /** 配方工厂，用于创建具体的配方对象 */
    private final RecipeFactory<T> recipeFactory;

    /**
     * <h3>构造方法</h3>
     *
     * @param recipeFactory 配方工厂，用于创建具体的配方对象
     */
    public SimpleCraftRecipeSerializer(RecipeFactory<T> recipeFactory) {
        this.recipeFactory = recipeFactory;
    }

    /**
     * <h3>从JSON对象读取简单合成配方</h3>
     *
     * <p><b>处理流程：</b></p>
     * <ol>
     *   <li>读取原料（支持数组或对象格式）</li>
     *   <li>读取结果物品ID并创建ItemStack</li>
     *   <li>调用子类方法读取额外数据</li>
     *   <li>使用工厂方法创建配方对象</li>
     * </ol>
     *
     * @param id 配方的唯一标识符
     * @param json 包含配方数据的JSON对象
     * @return 解析后的配方对象
     * @throws IllegalStateException 如果结果物品不存在
     */
    @Override
    public T read(Identifier id, JsonObject json) {
        // 1. 读取原料（支持数组或对象格式）
        JsonElement ingredientElement = JsonHelper.hasArray(json, "ingredient")
                ? JsonHelper.getArray(json, "ingredient")
                : JsonHelper.getObject(json, "ingredient");
        Ingredient ingredient = Ingredient.fromJson(ingredientElement, false);

        // 2. 读取结果物品
        String resultId = JsonHelper.getString(json, "result");
        ItemStack result = new ItemStack(
                Registries.ITEM.getOrEmpty(new Identifier(resultId))
                        .orElseThrow(() -> new IllegalStateException("Item: " + resultId + " does not exist"))
        );

        // 3. 调用子类方法读取配方特有的额外数据
        Object extraData = readExtraData(json);

        // 4. 使用工厂方法创建配方对象
        return this.recipeFactory.create(id, ingredient, result, extraData);
    }

    /**
     * <h3>从网络数据包读取简单合成配方</h3>
     *
     * <p><b>数据读取顺序：</b></p>
     * <ol>
     *   <li>原料（Ingredient）</li>
     *   <li>结果物品堆（ItemStack）</li>
     *   <li>额外数据（由子类处理）</li>
     * </ol>
     *
     * @param id 配方的唯一标识符
     * @param buf 包含序列化配方数据的数据包缓冲区
     * @return 解析后的配方对象
     */
    @Override
    public T read(Identifier id, PacketByteBuf buf) {
        // 1. 读取原料
        Ingredient ingredient = Ingredient.fromPacket(buf);

        // 2. 读取结果物品
        ItemStack result = buf.readItemStack();

        // 3. 调用子类方法从网络读取配方特有的额外数据
        Object extraData = readExtraData(buf);

        // 4. 使用工厂方法创建配方对象
        return this.recipeFactory.create(id, ingredient, result, extraData);
    }

    /**
     * <h3>将简单合成配方写入网络数据包</h3>
     *
     * <p><b>数据写入顺序：</b></p>
     * <ol>
     *   <li>原料（Ingredient）</li>
     *   <li>结果物品堆（ItemStack）</li>
     *   <li>额外数据（由子类处理）</li>
     * </ol>
     *
     * @param buf 目标数据包缓冲区
     * @param recipe 要序列化的配方对象
     */
    @Override
    public void write(PacketByteBuf buf, T recipe) {
        // 1. 写入原料
        recipe.getInput().write(buf);

        // 2. 写入结果物品
        buf.writeItemStack(recipe.output);

        // 3. 调用子类方法写入配方特有的额外数据
        writeExtraData(buf, recipe);
    }

    /**
     * <h3>从JSON读取配方特有的额外数据</h3>
     *
     * <p><b>子类必须实现此方法</b>，用于读取配方特有的配置数据。</p>
     *
     * <p><b>示例：</b></p>
     * <pre>{@code
     * protected Object readExtraData(JsonObject json) {
     *     int dryingTime = JsonHelper.getInt(json, "dryingTime", 100);
     *     String temperature = JsonHelper.getString(json, "temperature", "low");
     *     return new DryingData(dryingTime, temperature);
     * }
     * }</pre>
     *
     * @param json 包含配方数据的JSON对象
     * @return 解析出的额外数据对象
     */
    protected abstract Object readExtraData(JsonObject json);

    /**
     * <h3>从网络数据包读取配方特有的额外数据</h3>
     *
     * <p><b>子类必须实现此方法</b>，用于从网络数据包中读取额外数据。</p>
     *
     * @param buf 包含序列化数据的数据包缓冲区
     * @return 解析出的额外数据对象
     */
    protected abstract Object readExtraData(PacketByteBuf buf);

    /**
     * <h3>将配方特有的额外数据写入网络数据包</h3>
     *
     * <p><b>子类必须实现此方法</b>，用于将额外数据写入网络数据包。</p>
     *
     * @param buf 目标数据包缓冲区
     * @param recipe 要序列化的配方对象
     */
    protected abstract void writeExtraData(PacketByteBuf buf, T recipe);

    /**
     * <h3>配方工厂接口</h3>
     *
     * <p>用于创建具体配方对象的函数式接口。</p>
     *
     * @param <T> 配方类型
     */
    @FunctionalInterface
    public interface RecipeFactory<T extends SimpleCraftRecipe> {
        /**
         * <h4>创建配方对象</h4>
         *
         * @param id 配方标识符
         * @param input 输入原料
         * @param output 输出结果
         * @param extraData 额外数据
         * @return 创建的配方对象
         */
        T create(Identifier id, Ingredient input, ItemStack output, Object extraData);
    }
}