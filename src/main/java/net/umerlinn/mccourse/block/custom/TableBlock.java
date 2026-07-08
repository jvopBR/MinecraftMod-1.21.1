package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TableBlock extends Block {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;

    // Precomputed hitboxes for all 16 NSEW connection combinations.
    // Bit layout: N=8, S=4, E=2, W=1. A corner leg is removed when either
    // adjacent direction has a connection (matching the multipart model logic).
    private static final VoxelShape[] SHAPES = buildAllShapes();

    private static VoxelShape[] buildAllShapes() {
        VoxelShape top   = Block.box(0, 13, 0, 16, 16, 16); // reaches y=16 so placed items rest on surface
        VoxelShape legNW = Block.box(1, 0, 1,  3, 13,  3);
        VoxelShape legNE = Block.box(13, 0, 1, 15, 13,  3);
        VoxelShape legSW = Block.box(1, 0, 13,  3, 13, 15);
        VoxelShape legSE = Block.box(13, 0, 13, 15, 13, 15);

        VoxelShape[] shapes = new VoxelShape[16];
        for (int mask = 0; mask < 16; mask++) {
            boolean n = (mask & 8) != 0;
            boolean s = (mask & 4) != 0;
            boolean e = (mask & 2) != 0;
            boolean w = (mask & 1) != 0;

            VoxelShape shape = top;
            if (!n && !w) shape = Shapes.or(shape, legNW);
            if (!n && !e) shape = Shapes.or(shape, legNE);
            if (!s && !w) shape = Shapes.or(shape, legSW);
            if (!s && !e) shape = Shapes.or(shape, legSE);
            shapes[mask] = shape;
        }
        return shapes;
    }

    public TableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST,  false)
                .setValue(WEST,  false));
    }

    private static boolean canConnect(BlockState neighbor) {
        return neighbor.getBlock() instanceof TableBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockGetter level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        return defaultBlockState()
                .setValue(NORTH, canConnect(level.getBlockState(pos.north())))
                .setValue(SOUTH, canConnect(level.getBlockState(pos.south())))
                .setValue(EAST,  canConnect(level.getBlockState(pos.east())))
                .setValue(WEST,  canConnect(level.getBlockState(pos.west())));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return switch (direction) {
            case NORTH -> state.setValue(NORTH, canConnect(neighborState));
            case SOUTH -> state.setValue(SOUTH, canConnect(neighborState));
            case EAST  -> state.setValue(EAST,  canConnect(neighborState));
            case WEST  -> state.setValue(WEST,  canConnect(neighborState));
            default    -> super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = 0;
        if (state.getValue(NORTH)) mask |= 8;
        if (state.getValue(SOUTH)) mask |= 4;
        if (state.getValue(EAST))  mask |= 2;
        if (state.getValue(WEST))  mask |= 1;
        return SHAPES[mask];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST);
    }
}
