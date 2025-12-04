package org.foodcraft.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import org.foodcraft.screen.PotteryTableScreenHandler;

public class ModScreenHandlerTypes {
    public static final ScreenHandlerType<PotteryTableScreenHandler> POTTERY_TABLE =
            register("pottery_table", PotteryTableScreenHandler::new);

    /**
     * 注册单个屏幕处理器类型。
     * @param id 屏幕处理器类型的名称
     * @param factory 屏幕处理器的构造器
     * @param <T> 屏幕处理器的类型
     * @return 注册后的屏幕处理器类型
     */
    private static <T extends ScreenHandler> ScreenHandlerType<T> register(String id, ScreenHandlerType.Factory<T> factory) {
        return Registry.register(Registries.SCREEN_HANDLER, id, new ScreenHandlerType<>(factory, FeatureFlags.VANILLA_FEATURES));
    }

    public static void registerScreenHandlerTypes() {}
}