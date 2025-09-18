package org.foodcraft.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.registry.ModEntityTypes;
import org.jetbrains.annotations.Nullable;

public class CraftBlockEntity extends AbstractDecorationEntity {
    private BlockPos blockPos;

    public CraftBlockEntity(EntityType<? extends CraftBlockEntity> entityType, World world) {
        super(entityType, world);
    }

    protected CraftBlockEntity(World world, BlockPos pos) {
        this(ModEntityTypes.CRAFT_BLOCK_ENTITY, world);
        this.blockPos = pos;
    }


    @Override
    public int getWidthPixels() {
        return 15;
    }

    @Override
    public int getHeightPixels() {
        return 15;
    }

    @Override
    public void onBreak(@Nullable Entity entity) {

    }

    @Override
    public void onPlace() {

    }
}
