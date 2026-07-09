package net.umerlinn.mccourse.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.umerlinn.mccourse.block.ModBlockEntities;
import net.umerlinn.mccourse.block.entity.WardrobeBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A 2x2 wardrobe: PRIMARY (clicked position, bottom) + SECOND (bottom, beside PRIMARY) form the
 * base row, THIRD/FOURTH sit directly above them. Doors swing open (see
 * WardrobeBlockEntityRenderer) via a hand-built Java model instead of the baked JSON block model
 * — only PRIMARY renders (ENTITYBLOCK_ANIMATED, drawing all 4 quadrants in one pass), the other
 * 3 positions are RenderShape.INVISIBLE.
 */
public class WardrobeBlock extends MultiPartStorageBlock {

    public static final VoxelShape BOTTOM_SHAPE = FurnitureShapes.boxes(
            0, 0, 1, 16, 1, 15,
            0, 1, 0, 16, 16, 16
    );
    public static final VoxelShape TOP_SHAPE = FurnitureShapes.boxes(
            0, 0, 0, 16, 16, 16
    );

    private static final Map<MultiblockPart, VoxelShape> SHAPES = Map.of(
            MultiblockPart.PRIMARY, BOTTOM_SHAPE,
            MultiblockPart.SECOND, BOTTOM_SHAPE,
            MultiblockPart.THIRD, TOP_SHAPE,
            MultiblockPart.FOURTH, TOP_SHAPE
    );

    // The codec's wood name is a placeholder — it only matters for structure-template-style
    // block palette round-tripping, not normal placement/rendering (which always uses the real
    // registered instance's own woodName, set per wood type in ModFurnitureBlocks).
    public static final MapCodec<WardrobeBlock> CODEC =
            simpleCodec(props -> new WardrobeBlock(props, "oak"));

    private final String woodName;

    public WardrobeBlock(Properties properties, String woodName) {
        super(properties, SHAPES);
        this.woodName = woodName;
    }

    public String getWoodName() {
        return woodName;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected List<BlockPos> secondaryOffsets(Direction facing) {
        Direction right = facing.getClockWise();
        BlockPos rightPos = new BlockPos(right.getStepX(), 0, right.getStepZ());
        return List.of(
                rightPos,                                  // SECOND: bottom, beside primary
                new BlockPos(0, 1, 0),                      // THIRD: top, above primary
                rightPos.above()                            // FOURTH: top, above SECOND
        );
    }

    @Override
    protected BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WardrobeBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == MultiblockPart.PRIMARY ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() || state.getValue(PART) != MultiblockPart.PRIMARY) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.WARDROBE.get(), WardrobeBlockEntity::lidAnimateTick);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) == MultiblockPart.PRIMARY && level.getBlockEntity(pos) instanceof WardrobeBlockEntity wardrobe) {
            wardrobe.recheckOpen();
        }
    }
}
