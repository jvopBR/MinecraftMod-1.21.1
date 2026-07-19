package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 3-block-tall street lamp: PRIMARY = base (the clicked position), SECOND = pole, THIRD = the
 * lantern head, which is the only part that emits light (see the state-dependent lightLevel in
 * ModFurnitureBlocks). Placement claims the two positions above; breaking any part removes the
 * whole post. Same claim/cascade pattern as MultiPartStorageBlock (wardrobe), minus the storage:
 * cleanup is a direct level.setBlock from onRemove — NEVER updateShape returning AIR, which
 * always rolls the vacated position's loot table too and duplicates the drop (see
 * MultiPartStorageBlock's doc comment for the decompiled-source proof). The standard drop-self
 * loot table stays correct here because only the position the player actually broke rolls loot;
 * the siblings are removed silently.
 */
public class LampPostBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Reuses the wardrobe's part enum, restricted to 3 values: PRIMARY=base, SECOND=pole, THIRD=head.
    public static final EnumProperty<MultiblockPart> PART =
            EnumProperty.create("part", MultiblockPart.class, MultiblockPart.PRIMARY, MultiblockPart.SECOND, MultiblockPart.THIRD);

    private static final Map<MultiblockPart, VoxelShape> SHAPES = Map.of(
            MultiblockPart.PRIMARY, FurnitureShapes.boxes(
                    4.5, 0, 4.5, 11.5, 3.5, 11.5,
                    6.8, 3.5, 6.8, 9.2, 16, 9.2),
            MultiblockPart.SECOND, FurnitureShapes.boxes(
                    6.8, 0, 6.8, 9.2, 16, 9.2),
            MultiblockPart.THIRD, FurnitureShapes.boxes(
                    6.8, 0, 6.8, 9.2, 2, 9.2,
                    4.3, 2, 4.3, 11.7, 11.8, 11.7)
    );

    public LampPostBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, MultiblockPart.PRIMARY));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        for (int dy = 1; dy <= 2; dy++) {
            BlockPos above = pos.above(dy);
            if (!level.getWorldBorder().isWithinBounds(above) || !level.getBlockState(above).canBeReplaced(ctx)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            level.setBlock(pos.above(), state.setValue(PART, MultiblockPart.SECOND), 3);
            level.setBlock(pos.above(2), state.setValue(PART, MultiblockPart.THIRD), 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !movedByPiston) {
            BlockPos basePos = switch (state.getValue(PART)) {
                case PRIMARY -> pos;
                case SECOND -> pos.below();
                default -> pos.below(2);
            };
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos siblingPos = basePos.above(dy);
                if (!siblingPos.equals(pos) && level.getBlockState(siblingPos).is(this)) {
                    level.setBlock(siblingPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.get(state.getValue(PART));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}
