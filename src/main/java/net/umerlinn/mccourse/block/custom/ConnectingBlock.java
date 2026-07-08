package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class ConnectingBlock extends SeatBlock {

    public static final EnumProperty<SofaShape> SHAPE = EnumProperty.create("shape", SofaShape.class);

    // Legs + base + arm(s)/wraparound panel + backrest, matching each shape's model geometry.
    // The seat cushion itself is left out on purpose so the seat area stays walkable.
    private static final Map<SofaShape, Map<Direction, VoxelShape>> SHAPES = buildShapes();

    public ConnectingBlock(Properties properties) {
        super(properties, SHAPES.get(SofaShape.SINGLE).get(Direction.NORTH));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SHAPE, SofaShape.SINGLE));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        Direction facing = state.getValue(FACING);
        return state.setValue(SHAPE, computeShape(facing, ctx.getLevel(), ctx.getClickedPos()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Recompute on any horizontal neighbor change (front, back, left, right all matter).
        if (direction.getAxis().isHorizontal()) {
            Direction facing = state.getValue(FACING);
            return state.setValue(SHAPE, computeShape(facing, level, pos));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(SHAPE)).get(state.getValue(FACING));
    }

    private SofaShape computeShape(Direction facing, LevelAccessor level, BlockPos pos) {
        Direction leftDir  = facing.getCounterClockWise(); // player's left when seated
        Direction rightDir = facing.getClockWise();        // player's right when seated

        BlockState leftState  = level.getBlockState(pos.relative(leftDir));
        BlockState rightState = level.getBlockState(pos.relative(rightDir));
        BlockState frontState = level.getBlockState(pos.relative(facing));

        // A side counts as "open" (no armrest) either when a same-facing sofa continues
        // the run, or when a perpendicular sofa turns an L right beside us — its own
        // front already faces us, so an armrest here would poke into its seat.
        boolean openLeft  = hasFacing(leftState,  facing) || hasFacing(leftState,  leftDir.getOpposite());
        boolean openRight = hasFacing(rightState, facing) || hasFacing(rightState, rightDir.getOpposite());

        // A perpendicular sofa turning the corner right in front of us runs its own
        // backrest along one of our walls and straight into whichever arm sits on the
        // matching side. Swap that arm for a matching-height wraparound so the two
        // backrests read as one continuous L instead of a wall butting into a low arm.
        boolean wrapLeft  = hasFacing(frontState, rightDir);
        boolean wrapRight = hasFacing(frontState, leftDir);

        if (openLeft && openRight) return SofaShape.MIDDLE;
        if (openLeft)  return wrapRight ? SofaShape.CORNER_RIGHT : SofaShape.END_RIGHT;
        if (openRight) return wrapLeft  ? SofaShape.CORNER_LEFT  : SofaShape.END_LEFT;
        return SofaShape.SINGLE;
    }

    private boolean hasFacing(BlockState state, Direction dir) {
        return state.getBlock() instanceof ConnectingBlock
                && state.getValue(FACING) == dir;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    private static Map<SofaShape, Map<Direction, VoxelShape>> buildShapes() {
        Map<SofaShape, Map<Direction, VoxelShape>> map = new EnumMap<>(SofaShape.class);
        map.put(SofaShape.SINGLE, FurnitureShapes.rotateHorizontal(FurnitureShapes.boxes(
                1, 0, 0, 15, 4, 16,   // base (legs sit within this footprint)
                1, 4, 0, 3, 12, 14,   // left arm
                13, 4, 0, 15, 12, 14, // right arm
                2, 7, 11, 14, 15, 16  // backrest
        )));
        map.put(SofaShape.MIDDLE, FurnitureShapes.rotateHorizontal(FurnitureShapes.boxes(
                0, 0, 0, 16, 4, 16,   // base
                0, 7, 11, 16, 15, 16  // backrest
        )));
        map.put(SofaShape.END_LEFT, FurnitureShapes.rotateHorizontal(FurnitureShapes.boxes(
                1, 0, 0, 16, 4, 16,   // base
                1, 4, 0, 3, 12, 14,   // left arm
                2, 7, 11, 16, 15, 16  // backrest
        )));
        map.put(SofaShape.END_RIGHT, FurnitureShapes.rotateHorizontal(FurnitureShapes.boxes(
                0, 0, 0, 15, 4, 16,   // base
                13, 4, 0, 15, 12, 14, // right arm
                0, 7, 11, 14, 15, 16  // backrest
        )));
        map.put(SofaShape.CORNER_LEFT, FurnitureShapes.rotateHorizontal(FurnitureShapes.boxes(
                0, 0, 0, 16, 4, 16,   // base
                0, 4, 0, 5, 15, 16,   // wraparound panel
                5, 7, 11, 16, 15, 16  // backrest
        )));
        map.put(SofaShape.CORNER_RIGHT, FurnitureShapes.rotateHorizontal(FurnitureShapes.boxes(
                0, 0, 0, 16, 4, 16,   // base
                11, 4, 0, 16, 15, 16, // wraparound panel
                0, 7, 11, 11, 15, 16  // backrest
        )));
        return map;
    }
}
