package org.foodcraft.item;

import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;

public class ModPotions {
    public static final Potion MILK = register("milk", new Potion());

    private static Potion register(String name, Potion potion) {
        return Registry.register(Registries.POTION, new Identifier(FoodCraft.MOD_ID, name), potion);
    }
}
