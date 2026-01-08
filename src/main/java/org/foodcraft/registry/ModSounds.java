package org.foodcraft.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;

public class ModSounds {
    public static final SoundEvent COOKING_SOUND = registerSoundEvent("stove_baking");
    public static final SoundEvent GRINDING_STONE_GRINDING = registerSoundEvent("grinding_stone_grinding");
    public static final SoundEvent CUT = registerSoundEvent("cut");
    public static final SoundEvent CUT_MEAT = registerSoundEvent("cut_meat");
    public static final SoundEvent KITCHEN_KNIFE_PLACE = registerSoundEvent("kitchen_knife_place");
    public static final SoundEvent KITCHEN_KNIFE_FETCH = registerSoundEvent("kitchen_knife_fetch");
    public static final SoundEvent KITCHEN_KNIFE_BOARD_PLACE = registerSoundEvent("kitchen_knife_board_place");

    public static final SoundEvent SOUP_FILL = registerSoundEvent("soup_fill");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier(FoodCraft.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void initialize() {}
}
