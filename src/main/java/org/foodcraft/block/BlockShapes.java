package org.foodcraft.block;

import net.minecraft.block.Block;
import net.minecraft.util.shape.VoxelShape;
import org.dfood.shape.FoodShapeHandle;

public enum BlockShapes implements FoodShapeHandle.ShapeConvertible {;

    private final VoxelShape shape;
    private final int id;

    BlockShapes(int id, VoxelShape shape) {
        this.shape = shape;
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public VoxelShape getShape() {
        return shape;
    }
}
