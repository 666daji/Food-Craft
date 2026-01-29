package org.foodcraft.block.process.playeraction.impl;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.foodcraft.block.process.playeraction.PlayerAction;
import org.foodcraft.block.process.step.StepExecutionContext;
import org.foodcraft.registry.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 添加物品操作。
 *
 * <p>这是原物品系统的直接对应。</p>
 */
public class AddItemPlayerAction extends PlayerAction {
    public static final String TYPE = "add_item";
    /** 物品路径编码重映射，用于解决编码冲突 */
    public static final Map<Item, String> REMAPPING = new HashMap<>();

    private final Item item;
    private final int count;

    /**
     * 从字符串参数创建添加物品操作。
     *
     * @param params 参数字符串数组
     * @return 添加物品操作实例
     */
    public static AddItemPlayerAction fromParams(String[] params) {
        if (params.length < 1) {
            throw new IllegalArgumentException("The Add Item parameter requires the Item ID parameter");
        }

        Identifier itemId = Identifier.tryParse(params[0]);
        if (itemId == null) {
            throw new IllegalArgumentException("Invalid item ID: " + params[0]);
        }

        Item item = Registries.ITEM.get(itemId);
        if (item == Items.AIR) {
            throw new IllegalArgumentException("No item found: " + itemId);
        }

        int count = 1;
        if (params.length >= 2) {
            try {
                count = Integer.parseInt(params[1]);
                if (count <= 0) {
                    throw new IllegalArgumentException("The quantity must be greater than 0: " + count);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid quantity: " + params[1]);
            }
        }

        return new AddItemPlayerAction(item, count);
    }

    /**
     * 从上下文创建添加物品操作。
     *
     * @param context 步骤执行上下文
     * @return 如果玩家手持物品，则返回对应的添加物品操作
     */
    public static Optional<PlayerAction> fromContext(StepExecutionContext<?> context) {
        ItemStack heldItem = context.getHeldItemStack();
        if (!heldItem.isEmpty()) {
            return Optional.of(new AddItemPlayerAction(heldItem.getItem(), 1));
        }
        return Optional.empty();
    }

    /**
     * 创建添加物品操作。
     *
     * @param item 要添加的物品
     * @param count 物品数量（默认为1）
     */
    public AddItemPlayerAction(Item item, int count) {
        if (item == null) {
            throw new IllegalArgumentException("Items cannot be null");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("The quantity must be greater than 0");
        }

        this.item = item;
        this.count = count;
    }

    public AddItemPlayerAction(Item item) {
        this(item, 1);
    }

    @Override
    public String toString() {
        Identifier itemId = Registries.ITEM.getId(item);
        return String.format("add_item|%s|%d", itemId, count);
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemStack(item, count);
    }

    @Override
    public void consume(StepExecutionContext<?> context) {
        // 如果不是创造模式，消耗玩家手持物品
        if (!context.isCreateMode()) {
            ItemStack heldItem = context.getHeldItemStack();
            if (!heldItem.isEmpty() && heldItem.isOf(item)) {
                heldItem.decrement(count);
            }
        }

        // 播放操作音效
        context.playSound(context.getItemSounds().getPlaceSound());
    }

    @Override
    public boolean matches(PlayerAction other) {
        if (!(other instanceof AddItemPlayerAction otherAction)) {
            return false;
        }

        // 匹配物品类型和数量
        return this.item == otherAction.item && count == otherAction.count;
    }

    /**
     * 编码格式: i[命名空间前2位]_[物品路径前3位][数量，如果为1则省略]。
     * @return 简短编码
     */
    @Override
    public String getCode() {
        Identifier itemId = Registries.ITEM.getId(item);

        // 1. 操作类型固定位: "i" 表示添加物品
        StringBuilder code = new StringBuilder("i");

        // 2. 命名空间前2位（不足补'_'）
        String namespace = itemId.getNamespace();
        if (namespace.length() >= 2) {
            code.append(namespace, 0, 2);
        } else {
            code.append(namespace).append("_".repeat(2 - namespace.length()));
        }

        code.append("_");

        // 3. 物品路径前3位（不足补'_'）
        String path = itemId.getPath();
        if (REMAPPING.containsKey(item)) {
            code.append(REMAPPING.get(item));
        } else if (path.length() >= 3) {
            code.append(path, 0, 3);
        } else {
            code.append(path).append("_".repeat(3 - path.length()));
        }

        // 4. 数量（如果大于1则添加，否则省略）
        if (count > 1) {
            code.append(count);
        }

        return code.toString();
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("player_action.add_item", item.getName(), count);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    static {
        REMAPPING.put(ModItems.SALMON_CUBES, "msa");
    }
}