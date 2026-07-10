package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
     * Same rotation convention as {@link #rotateHorizontal}, but for a single anchor point (e.g.
     * a candle's render/particle position) instead of a VoxelShape. Keeping both derived from the
     * identical (x,z) -> (1-z,x) transform guarantees the point can never drift out of sync with
     * the shape it's meant to sit inside, regardless of facing.
     */
    public static Map<Direction, Vec3> rotateHorizontalPoint(Vec3 north) {
        Map<Direction, Vec3> points = new EnumMap<>(Direction.class);
        Vec3 point = north;
        points.put(Direction.NORTH, point);
        for (Direction dir : new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            point = new Vec3(1 - point.z, point.y, point.x);
            points.put(dir, point);
        }
        return points;
    }

    /** Batch version of {@link #rotateHorizontalPoint} for a whole array of points at once (e.g. all 6 candle slots), keeping every entry rotated by the identical transform. */
    public static Map<Direction, Vec3[]> rotateHorizontalPoints(Vec3[] north) {
        Map<Direction, Vec3[]> result = new EnumMap<>(Direction.class);
        Vec3[] current = north;
        result.put(Direction.NORTH, current);
        for (Direction dir : new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            Vec3[] rotated = new Vec3[current.length];
            for (int i = 0; i < current.length; i++) {
                Vec3 point = current[i];
                rotated[i] = new Vec3(1 - point.z, point.y, point.x);
            }
            result.put(dir, rotated);
            current = rotated;
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
