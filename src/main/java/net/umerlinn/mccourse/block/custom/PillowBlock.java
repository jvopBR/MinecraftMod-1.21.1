package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.umerlinn.mccourse.entity.ModEntities;
import net.umerlinn.mccourse.entity.SeatEntity;

import java.util.List;
import java.util.Map;

public class PillowBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // 0 = no offset (full block, y=0..4)
    // 1 = -4px (blocks ~12/16 tall: enchanting table, etc.)
    // 2 = -8px (blocks ~8-9/16 tall: slab, bed, stonecutter, hopper)
    // 3 = sofa-backrest mode (model in sofa block space: y=7..15, z=8..11 — rendered via SOFA_SEAT_POSITIONS)
    public static final IntegerProperty HEIGHT_OFFSET = IntegerProperty.create("height_offset", 0, 3);

    private final Map<Direction, VoxelShape> shapesByFacing;

    public PillowBlock(Properties properties, VoxelShape northShape) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HEIGHT_OFFSET, 0));
        this.shapesByFacing = FurnitureShapes.rotateHorizontal(northShape);
    }

    // Reads the actual max-Y of the block below (0..1 range) and quantises to one of 3 offset
    // levels. Falls back to 0 if the shape is empty or inaccessible (e.g. a block with no shape).
    private static int computeOffsetLevel(BlockState below, LevelAccessor level, BlockPos belowPos) {
        if (below.isAir()) return 0;
        try {
            VoxelShape shape = below.getShape(level, belowPos);
            double topY = shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y);
            if (topY <= 0.5625) return 2;   // ≤9/16: slab, bed, hopper, stonecutter
            if (topY <= 0.8125) return 1;   // ≤13/16: enchanting table (12/16), etc.
            return 0;                         // >13/16: full-cube-equivalent top
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        BlockState below = ctx.getLevel().getBlockState(pos.below());
        return this.defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(HEIGHT_OFFSET, computeOffsetLevel(below, ctx.getLevel(), pos.below()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return state.setValue(HEIGHT_OFFSET,
                    computeOffsetLevel(neighborState, level, neighborPos));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.isEmptyBlock(pos.below());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return shapesByFacing.get(state.getValue(FACING));
    }

    // Right-click to sit on the pillow (only for height_offset=0, i.e. placed on a full block).
    // Pillows shifted down (on slabs, enchanting tables, etc.) skip sitting — their VoxelShape
    // and visual position don't align well enough for a comfortable seat height.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (state.getValue(HEIGHT_OFFSET) != 0) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        List<SeatEntity> existing = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
        if (!existing.isEmpty()) return InteractionResult.PASS;

        SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), level);
        // getPassengerRidingPosition adds +0.5 to entity Y. Target: player feet at pillow top
        // (pos.Y + 4/16), so entity.Y = pos.Y + 4/16 - 0.5 = pos.Y - 4/16.
        seat.moveTo(pos.getX() + 0.5, pos.getY() - 4.0 / 16.0, pos.getZ() + 0.5);
        level.addFreshEntity(seat);
        player.startRiding(seat, true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).forEach(e -> {
                e.ejectPassengers();
                e.discard();
            });
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HEIGHT_OFFSET);
    }
}
