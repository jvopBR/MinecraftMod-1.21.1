package net.umerlinn.mccourse.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Floor-standing candle holder — a small wrought-iron stand, like a torch replacement, that grows
 * a new arm each time another candle is added (up to CandleHolderBlockEntity.SLOTS). Symmetric on
 * all 4 sides so, unlike most furniture here, it has no FACING property at all (matches vanilla
 * CandleBlock, which is likewise facing-less).
 */
public class CandleHolderBlock extends AbstractCandleHolderBlock {

    public static final MapCodec<CandleHolderBlock> CODEC = simpleCodec(CandleHolderBlock::new);

    private static final VoxelShape SHAPE = FurnitureShapes.boxes(
            6, 0, 6, 10, 2, 10,   // base foot
            7, 2, 7, 9, 9, 9,     // center post
            6, 9, 6, 10, 10, 10   // cup (slot 0)
    );
    // Unlike wall/hanging, each tier of candle_holder_N.json was hand-redesigned in Blockbench as
    // its own independent arrangement (not "previous tier + one more arm"), so the offsets aren't
    // a single fixed array indexed by slot — each candle count has its own set of cup positions,
    // read directly off that tier's actual model file. Keep these in sync if the models move.
    private static final Vec3[] TIER_1 = {
            new Vec3(0.5, 10.0 / 16, 0.5), // candle_holder.json's cup (6,9,6 - 10,10,10)
    };
    private static final Vec3[] TIER_2 = {
            new Vec3(0.5, 11.0 / 16, 4.0 / 16),  // candle_holder_2.json cup (6,10,2 - 10,11,6)
            new Vec3(0.5, 11.0 / 16, 12.0 / 16), // cup (6,10,10 - 10,11,14)
    };
    private static final Vec3[] TIER_3 = {
            new Vec3(0.5, 11.0 / 16, 0.5),         // candle_holder_3.json cup (6,10,6 - 10,11,10)
            new Vec3(0.5, 11.0 / 16, 13.0 / 16),   // cup (6,10,11 - 10,11,15)
            new Vec3(0.512, 11.0 / 16, 0.202),     // cup (6.196,10,1.236 - 10.196,11,5.236)
    };
    private static final Vec3[] TIER_4 = {
            new Vec3(0.5, 10.0 / 16, 3.0 / 16),  // candle_holder_4.json cup (6,9,1 - 10,10,5)
            new Vec3(3.0 / 16, 10.0 / 16, 0.5),  // cup (1,9,6 - 5,10,10)
            new Vec3(0.5, 10.0 / 16, 13.0 / 16), // cup (6,9,11 - 10,10,15)
            new Vec3(13.0 / 16, 10.0 / 16, 0.5), // cup (11,9,6 - 15,10,10)
    };

    public CandleHolderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(CANDLES, 0));
    }

    @Override
    protected MapCodec<? extends AbstractCandleHolderBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Vec3[] candleOffsets(BlockState state) {
        return switch (state.getValue(CANDLES)) {
            case 2 -> TIER_2;
            case 3 -> TIER_3;
            case 4 -> TIER_4;
            default -> TIER_1;
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
