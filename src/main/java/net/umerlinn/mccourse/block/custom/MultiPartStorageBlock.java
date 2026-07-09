package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Base for storage furniture that spans several block positions placed as a single action —
 * one PRIMARY (the clicked position, owns the real Container BlockEntity) plus up to 3
 * SECOND/THIRD/FOURTH positions at offsets the subclass defines. Placing claims every position
 * (rejecting if any is occupied); breaking ANY of them removes the whole group.
 *
 * Cleanup is done with a direct level.setBlock(...) from onRemove, NOT by returning AIR from
 * updateShape. Returning AIR from updateShape looks correct but isn't: confirmed via decompiled
 * source (Block.updateOrDestroy, called by NeighborUpdater.executeShapeUpdate for every
 * updateShape result) that the framework always applies an AIR result via
 * level.destroyBlock(pos, dropBlock=true, ...) — and critically, Level.setBlock strips the
 * "suppress drops" flag bit before propagating to neighbours (see markAndNotifyBlock:
 * `i = flags & -34`), so there is no flag combination that avoids it. That meant the vacated
 * position independently rolled this block's loot table too, dropping a duplicate item whenever
 * either half was broken. A direct setBlock (not destroyBlock) never rolls a loot table at all,
 * so this is the fix, not a flags tweak.
 */
public abstract class MultiPartStorageBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<MultiblockPart> PART = EnumProperty.create("part", MultiblockPart.class);

    private static final MultiblockPart[] SECONDARY_PARTS =
            {MultiblockPart.SECOND, MultiblockPart.THIRD, MultiblockPart.FOURTH};

    private final Map<MultiblockPart, Map<Direction, VoxelShape>> shapes;

    protected MultiPartStorageBlock(Properties properties, Map<MultiblockPart, VoxelShape> partNorthShapes) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, MultiblockPart.PRIMARY));
        this.shapes = partNorthShapes.entrySet().stream().collect(
                java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> FurnitureShapes.rotateHorizontal(e.getValue())));
    }

    /**
     * Offsets from PRIMARY to SECOND, THIRD, FOURTH (in that order — list size = how many
     * secondaries this block uses, 0 to 3), already resolved for the given FACING.
     */
    protected abstract List<BlockPos> secondaryOffsets(Direction facing);

    /** Real Container BlockEntity for the PRIMARY position. Never called for other parts. */
    protected abstract BlockEntity createBlockEntity(BlockPos pos, BlockState state);

    private BlockPos offsetFromPrimary(BlockState state) {
        MultiblockPart part = state.getValue(PART);
        if (part == MultiblockPart.PRIMARY) {
            return BlockPos.ZERO;
        }
        int index = part.ordinal() - 1; // SECOND=1->0, THIRD=2->1, FOURTH=3->2
        return secondaryOffsets(state.getValue(FACING)).get(index);
    }

    private BlockPos primaryPos(BlockState state, BlockPos pos) {
        return pos.subtract(offsetFromPrimary(state));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        for (BlockPos offset : secondaryOffsets(facing)) {
            BlockPos secondaryPos = pos.offset(offset);
            if (!level.getWorldBorder().isWithinBounds(secondaryPos) || !level.getBlockState(secondaryPos).canBeReplaced(ctx)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, MultiblockPart.PRIMARY);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            List<BlockPos> offsets = secondaryOffsets(state.getValue(FACING));
            for (int i = 0; i < offsets.size(); i++) {
                BlockPos secondaryPos = pos.offset(offsets.get(i));
                level.setBlock(secondaryPos, state.setValue(PART, SECONDARY_PARTS[i]), 3);
            }
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !movedByPiston) {
            BlockPos primaryPos = primaryPos(state, pos);
            List<BlockPos> offsets = secondaryOffsets(state.getValue(FACING));
            removeSibling(level, primaryPos, pos);
            for (BlockPos offset : offsets) {
                removeSibling(level, primaryPos.offset(offset), pos);
            }
            if (state.getValue(PART) == MultiblockPart.PRIMARY) {
                Containers.dropContentsOnDestroy(state, newState, level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void removeSibling(Level level, BlockPos siblingPos, BlockPos beingRemoved) {
        if (siblingPos.equals(beingRemoved)) {
            return;
        }
        if (level.getBlockState(siblingPos).is(this)) {
            level.setBlock(siblingPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(primaryPos(state, pos));
        if (be instanceof MenuProvider provider) {
            player.openMenu(provider);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public final BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == MultiblockPart.PRIMARY ? createBlockEntity(pos, state) : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapes.get(state.getValue(PART)).get(state.getValue(FACING));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(primaryPos(state, pos)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}
