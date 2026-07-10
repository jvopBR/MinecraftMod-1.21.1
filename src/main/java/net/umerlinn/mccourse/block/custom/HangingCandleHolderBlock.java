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
 * Ceiling-mounted candle holder — a small cage hanging from a chain, attaches to the underside
 * of a block like a lantern. Always hangs (no floor-standing toggle, unlike vanilla Lantern);
 * radially symmetric so it needs no FACING property.
 */
public class HangingCandleHolderBlock extends AbstractCandleHolderBlock {

    public static final MapCodec<HangingCandleHolderBlock> CODEC = simpleCodec(HangingCandleHolderBlock::new);

    // Enlarged tier-1 lantern (taller posts 7-11, top ring raised to 11-12, roof cap added at
    // 12-13, mount+chain raised to match) — bottom ring/cup footprint (5-11 x/z, cup itself
    // unchanged at 6,4,6-10,7,10) stayed put, so only the upper envelope grew. Capped at y=16
    // (the mount visually pokes up to y=18 to plug into the block above, but its own block's
    // occupies that space already — extending collision there would be redundant).
    private static final VoxelShape SHAPE = FurnitureShapes.boxes(
            7.5, 13, 7.5, 8.5, 16, 8.5, // mount stub + chain
            5, 6, 5, 11, 13, 11         // cage envelope (posts, rings, roof, cup)
    );
    // TIER_1 keeps the enclosed lantern-cage look (single candle, protected inside a frame).
    // TIER_2+ are the open branching-chandelier style instead (no cage) — user hand-built the
    // connecting arms in Blockbench (zigzag staircases off the central hub, same technique as the
    // floor holder), extending left/right (2), left/back/right (3), left/front/back/right (4).
    // Coordinates read directly off each tier's actual model file; keep in sync if they move.
    private static final Vec3[] TIER_1 = {
            new Vec3(0.5, 7.3 / 16, 0.5), // hanging_candle_holder.json cup (6,4,6 - 10,7,10)
    };
    // Whole assembly moved down (chain lengthened: was 7.5,11,7.5-8.5,15,8.5, now
    // 7.5,8,7.5-8.5,15,8.5) — X/Z positions are unchanged, only the shared Y dropped from 10/16
    // to 7/16 across every tier.
    private static final Vec3[] TIER_2 = {
            new Vec3(3.5 / 16, 7.0 / 16, 0.5),  // hanging_candle_holder_2.json left cup (1.5,6,6 - 5.5,7,10)
            new Vec3(12.5 / 16, 7.0 / 16, 0.5), // right cup (10.5,6,6 - 14.5,7,10)
    };
    private static final Vec3[] TIER_3 = {
            new Vec3(3.0 / 16, 7.0 / 16, 0.5),        // hanging_candle_holder_3.json left cup (1.5,6,6 - 4.5,7,10)
            new Vec3(8.5 / 16, 7.0 / 16, 13.5 / 16),  // back cup (7,6,12 - 10,7,15)
            new Vec3(13.0 / 16, 7.0 / 16, 0.5),       // right cup (11.5,6,6 - 14.5,7,10)
    };
    private static final Vec3[] TIER_4 = {
            new Vec3(3.5 / 16, 7.0 / 16, 0.5),  // hanging_candle_holder_4.json left cup (2.5,6,7 - 4.5,7,9)
            new Vec3(0.5, 7.0 / 16, 3.0 / 16),  // front cup (7,6,2 - 9,7,4)
            new Vec3(0.5, 7.0 / 16, 13.0 / 16), // back cup (7,6,12 - 9,7,14)
            new Vec3(12.5 / 16, 7.0 / 16, 0.5), // right cup (11.5,6,7 - 13.5,7,9)
    };

    public HangingCandleHolderBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(CANDLES, 0));
    }

    @Override
    protected MapCodec<? extends AbstractCandleHolderBlock> codec() {
        return CODEC;
    }

    // The open chandelier arms (2+ candles) still shrink their candles — real-block scale looked
    // oversized dangling off those small arms. Tier 1's cage needs the same reduction too: cup
    // top sits at y=7 and the top ring starts at y=11, so a natural-height (6-unit) candle would
    // reach y=13 and poke straight through the ring/roof cap — 0.7x keeps its top under y=11.
    @Override
    public float candleScale(BlockState state) {
        return 0.7f;
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
        return Block.canSupportCenter(level, pos.above(), Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                      LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.UP && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
