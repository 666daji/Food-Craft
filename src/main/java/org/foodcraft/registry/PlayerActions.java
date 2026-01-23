package org.foodcraft.registry;

import org.foodcraft.block.process.playeraction.PlayerActionFactory;
import org.foodcraft.block.process.playeraction.impl.AddItemPlayerAction;

public class PlayerActions {

    public static void registerDefaults() {
        // 注册添加物品操作
        PlayerActionFactory.register(
                AddItemPlayerAction.TYPE,
                AddItemPlayerAction::fromParams,
                context -> AddItemPlayerAction.fromContext(context).orElse(null)
        );
    }
}
