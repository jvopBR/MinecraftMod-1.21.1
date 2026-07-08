package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class FurnitureShapes {

    private FurnitureShapes() {}

    /**
     * Builds a NORTH/EAST/SOUTH/WEST shape map by rotating a NORTH-facing shape clockwise,
     * matching the same y=0/90/180/270 convention blockstate JSON variants use.
     */
    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape north) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        VoxelShape shape = north;
        shapes.put(Direction.NORTH, shape);
        for (Direction dir : new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            shape = rotateClockwise(shape);
            shapes.put(dir, shape);
        }
        return shapes;
    }

    private static VoxelShape rotateClockwise(VoxelShape shape) {
        VoxelShape result = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            result = Shapes.or(result, Shapes.box(
                    1 - box.maxZ, box.minY, box.minX,
                    1 - box.minZ, box.maxY, box.maxX));
        }
        return result;
    }

    /**
     * Unions cuboids given in 0-16 pixel coordinates (the same units used in block model JSON
     * "from"/"to" arrays), six doubles per box: minX, minY, minZ, maxX, maxY, maxZ.
     */
    public static VoxelShape boxes(double... pixels) {
        if (pixels.length % 6 != 0) {
            throw new IllegalArgumentException("boxes() expects a multiple of 6 coordinates");
        }
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < pixels.length; i += 6) {
            shape = Shapes.or(shape, Shapes.box(
                    pixels[i] / 16.0, pixels[i + 1] / 16.0, pixels[i + 2] / 16.0,
                    pixels[i + 3] / 16.0, pixels[i + 4] / 16.0, pixels[i + 5] / 16.0));
        }
        return shape;
    }
}
