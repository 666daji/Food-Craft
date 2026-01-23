package org.foodcraft.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.foodcraft.FoodCraft;
import org.foodcraft.block.process.playeraction.PlayerAction;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家操作列表工具类，用于序列化和反序列化操作列表。
 */
public class PlayerActionListUtil {

    private static final String ACTIONS_KEY = "Actions";
    private static final String ACTION_STRING_KEY = "ActionStr";

    /**
     * 将操作列表写入NBT。
     */
    public static void writeActionsToNbt(NbtCompound nbt, List<PlayerAction> actions) {
        NbtList actionsList = new NbtList();

        for (PlayerAction action : actions) {
            NbtCompound actionNbt = new NbtCompound();
            actionNbt.putString(ACTION_STRING_KEY, action.toString());
            actionsList.add(actionNbt);
        }

        nbt.put(ACTIONS_KEY, actionsList);
    }

    /**
     * 从NBT读取操作列表。
     */
    public static List<PlayerAction> readActionsFromNbt(NbtCompound nbt) {
        List<PlayerAction> actions = new ArrayList<>();

        if (nbt.contains(ACTIONS_KEY, NbtElement.LIST_TYPE)) {
            NbtList actionsList = nbt.getList(ACTIONS_KEY, NbtElement.COMPOUND_TYPE);

            for (int i = 0; i < actionsList.size(); i++) {
                NbtCompound actionNbt = actionsList.getCompound(i);
                String actionStr = actionNbt.getString(ACTION_STRING_KEY);

                try {
                    PlayerAction action = PlayerAction.fromString(actionStr);
                    actions.add(action);
                } catch (Exception e) {
                    // 记录错误但继续处理其他操作
                    FoodCraft.LOGGER.warn("无法解析操作字符串: {}", actionStr, e);
                }
            }
        }

        return actions;
    }

    /**
     * 将操作列表转换为物品堆栈列表（用于库存接口兼容）。
     */
    public static List<ItemStack> actionsToItemStacks(List<PlayerAction> actions) {
        List<ItemStack> stacks = new ArrayList<>();

        for (PlayerAction action : actions) {
            stacks.add(action.toItemStack());
        }

        return stacks;
    }
}