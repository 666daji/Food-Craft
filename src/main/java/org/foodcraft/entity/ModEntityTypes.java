package org.foodcraft.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;

public class ModEntityTypes {
    public static final EntityType<CraftBlockEntity> CRAFT_BLOCK_ENTITY = register("craft_block_entity",
            EntityType.Builder.<CraftBlockEntity>create(CraftBlockEntity::new, SpawnGroup.MISC)
                    .setDimensions(1.0F, 1.0F)
                    .maxTrackingRange(8)
    );

    private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, new Identifier(FoodCraft.MOD_ID, id), type.build(id));
    }
}
