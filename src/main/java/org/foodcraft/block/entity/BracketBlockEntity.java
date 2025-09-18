package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.foodcraft.registry.ModBlockEntityTypes;

public class BracketBlockEntity extends BlockEntity {

    public BracketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.BRACKET, pos, state);
    }
}
