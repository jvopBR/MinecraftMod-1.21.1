package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class ConnectingBlock extends SeatBlock {

    public static final EnumProperty<SofaShape> SHAPE = EnumProperty.create("shape", SofaShape.class);

    public ConnectingBlock(Properties properties) {
        super(properties);
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

    private SofaShape computeShape(Direction facing, LevelAccessor level, BlockPos pos) {
        Direction leftDir  = facing.getCounterClockWise(); // player's left when seated
        Direction rightDir = facing.getClockWise();        // player's right when seated

        BlockState leftState  = level.getBlockState(pos.relative(leftDir));
        BlockState rightState = level.getBlockState(pos.relative(rightDir));
        BlockState frontState = level.getBlockState(pos.relative(facing)); // direction player faces

        // Straight connections: neighbor faces same direction
        boolean connLeft  = hasFacing(leftState,  facing);
        boolean connRight = hasFacing(rightState, facing);

        // Corner detection has two cases:
        //
        // Case A — arm is BESIDE us (directly to left/right):
        //   The neighbor on our left/right faces AWAY from us (its facing == opposite of left/rightDir).
        //   e.g., NORTH-facing corner: right(EAST) neighbor faces WEST → corner_right
        //
        // Case B — arm is IN FRONT of us (in our facing direction):
        //   The neighbor in front faces 90° sideways. If it faces our CCW direction → corner_right,
        //   because the inside corner of the L is on our right/east side.
        //   e.g., NORTH-facing corner: front(NORTH) neighbor faces WEST(=CCW) → corner_right

        boolean cornerRight = hasFacing(rightState, rightDir.getOpposite())        // Case A right
                           || hasFacing(frontState, facing.getCounterClockWise()); // Case B front→right

        boolean cornerLeft  = hasFacing(leftState,  leftDir.getOpposite())         // Case A left
                           || hasFacing(frontState, facing.getClockWise());         // Case B front→left

        if (cornerRight) return SofaShape.CORNER_RIGHT;
        if (cornerLeft)  return SofaShape.CORNER_LEFT;
        if (connLeft && connRight) return SofaShape.MIDDLE;
        if (connLeft)  return SofaShape.END_RIGHT;
        if (connRight) return SofaShape.END_LEFT;
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
}
