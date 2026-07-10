package net.umerlinn.mccourse.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Wall-mounted sconce — attaches to the side of a block like a wall torch, arm reaching outward
 * with a single candle at the tip. FACING is the direction it points AWAY from the wall (same
 * convention as vanilla WallTorchBlock).
 */
public class WallCandleHolderBlock extends AbstractCandleHolderBlock {

    public static final MapCodec<WallCandleHolderBlock> CODEC = simpleCodec(WallCandleHolderBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // NORTH-facing reference pose: backplate flush against a wall to the south, arm reaching
    // north — matches FurnitureShapes.rotateHorizontal's NORTH=0 blockstate convention.
    private static final VoxelShape NORTH_SHAPE = FurnitureShapes.boxes(
            5, 4, 13, 11, 12, 16,   // wall backplate
            6, 6, 6, 10, 8, 13,     // arm
            5, 8, 3, 11, 11, 7      // cup
    );
    private static final Map<Direction, VoxelShape> SHAPES = FurnitureShapes.rotateHorizontal(NORTH_SHAPE);
    // Like the floor and hanging holders, each tier is its own hand-built arrangement (not
    // cumulative) — user redesigned the arm as a zigzag off the backplate for every tier.
    // NORTH-pose coordinates read directly off each tier's actual model file, then rotated per
    // facing via rotateHorizontalPoints; keep in sync if the models move.
    private static final Vec3[] NORTH_TIER_1 = {
            new Vec3(0.5, 11.0 / 16, 4.5 / 16), // wall_candle_holder.json cup (5,8,2 - 11,11,7)
    };
    private static final Vec3[] NORTH_TIER_2 = {
            new Vec3(12.0 / 16, 10.0 / 16, 4.5 / 16), // wall_candle_holder_2.json right cup (9,8,2 - 15,10,7)
            new Vec3(3.0 / 16, 10.0 / 16, 4.5 / 16),  // left cup (0,8,2 - 6,10,7)
    };
    private static final Vec3[] NORTH_TIER_3 = {
            new Vec3(0.5, 11.0 / 16, 5.0 / 16),  // wall_candle_holder_3.json center cup (5,8,3 - 11,11,7)
            new Vec3(1.0, 11.0 / 16, 5.0 / 16),  // right cup (13,8,3 - 19,11,7) — extends past this block
            new Vec3(0.0, 11.0 / 16, 5.0 / 16),  // left cup (-3,8,3 - 3,11,7) — extends past this block
    };
    private static final Vec3[] NORTH_TIER_4 = {
            new Vec3(11.0 / 16, 8.0 / 16, 7.0 / 16),    // wall_candle_holder_4.json upper-right cup (9,5,5 - 13,8,9)
            new Vec3(13.0 / 16, 12.0 / 16, 10.5 / 16),  // lower-right cup (11,9,9 - 15,12,12)
            new Vec3(3.0 / 16, 12.0 / 16, 10.5 / 16),   // lower-left cup (1,9,9 - 5,12,12)
            new Vec3(5.0 / 16, 8.0 / 16, 7.0 / 16),     // upper-left cup (3,5,5 - 7,8,9)
    };
    private static final Map<Direction, Vec3[]> TIER_1 = FurnitureShapes.rotateHorizontalPoints(NORTH_TIER_1);
    private static final Map<Direction, Vec3[]> TIER_2 = FurnitureShapes.rotateHorizontalPoints(NORTH_TIER_2);
    private static final Map<Direction, Vec3[]> TIER_3 = FurnitureShapes.rotateHorizontalPoints(NORTH_TIER_3);
    private static final Map<Direction, Vec3[]> TIER_4 = FurnitureShapes.rotateHorizontalPoints(NORTH_TIER_4);

    public WallCandleHolderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(CANDLES, 0));
    }

    @Override
    protected MapCodec<? extends AbstractCandleHolderBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public Vec3[] candleOffsets(BlockState state) {
        Map<Direction, Vec3[]> tier = switch (state.getValue(CANDLES)) {
            case 2 -> TIER_2;
            case 3 -> TIER_3;
            case 4 -> TIER_4;
            default -> TIER_1;
        };
        return tier.get(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                state = state.setValue(FACING, direction.getOpposite());
                if (state.canSurvive(level, pos)) return state;
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}
