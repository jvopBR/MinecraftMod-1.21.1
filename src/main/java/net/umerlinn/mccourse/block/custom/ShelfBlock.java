package net.umerlinn.mccourse.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.umerlinn.mccourse.block.entity.ShelfBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ShelfBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Back panel flush with wall (z=14..16), surface at y=8..10/z=6..14.
    // Support brackets under the surface (y=4..8) on each side.
    // Surface depth center: (6+14)/2 = 10/16 → backward offset from block center = 2/16.
    public static final VoxelShape NORTH_SHAPE = FurnitureShapes.boxes(
            0,  4, 14, 16, 13, 16,  // back panel (flush against wall)
            0,  8,  6, 16, 10, 14,  // shelf surface
            0,  4,  6,  2,  8, 14,  // left support bracket (below surface)
            14, 4,  6, 16,  8, 14   // right support bracket (below surface)
    );

    public static final MapCodec<ShelfBlock> CODEC =
            simpleCodec(props -> new ShelfBlock(props, NORTH_SHAPE));

    private final Map<Direction, VoxelShape> shapesByFacing;

    public ShelfBlock(Properties properties, VoxelShape northShape) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.shapesByFacing = FurnitureShapes.rotateHorizontal(northShape);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapesByFacing.get(state.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShelfBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        ShelfBlockEntity shelf = getShelf(level, pos);
        if (shelf == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        int slot = getClickedSlot(hit, pos, state.getValue(FACING));
        ItemStack displayed = shelf.getItem(slot);

        if (!displayed.isEmpty()) {
            player.addItem(displayed.copy());
            shelf.setItem(slot, ItemStack.EMPTY);
        } else {
            shelf.setItem(slot, stack.copyWithCount(1));
            if (!player.isCreative()) stack.shrink(1);
        }
        shelf.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ShelfBlockEntity shelf = getShelf(level, pos);
        if (shelf == null) return InteractionResult.PASS;

        int slot = getClickedSlot(hit, pos, state.getValue(FACING));
        ItemStack displayed = shelf.getItem(slot);
        if (displayed.isEmpty()) return InteractionResult.PASS;

        player.addItem(displayed.copy());
        shelf.setItem(slot, ItemStack.EMPTY);
        shelf.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            ShelfBlockEntity shelf = getShelf(level, pos);
            if (shelf != null) {
                for (int i = 0; i < ShelfBlockEntity.SLOTS; i++) {
                    ItemStack s = shelf.getItem(i);
                    if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // Map the click location to one of 3 horizontal slots.
    // Uses facing.getClockWise() as the "left → right" axis so ordering is consistent
    // regardless of which cardinal direction the shelf faces.
    private static int getClickedSlot(BlockHitResult hit, BlockPos pos, Direction facing) {
        Direction perp = facing.getClockWise();
        var pv = perp.getNormal();
        double lx = hit.getLocation().x - pos.getX() - 0.5;
        double lz = hit.getLocation().z - pos.getZ() - 0.5;
        double t = lx * pv.getX() + lz * pv.getZ(); // -0.5 .. +0.5
        if (t < -2.0 / 16.0) return 0;
        if (t <  2.0 / 16.0) return 1;
        return 2;
    }

    @Nullable
    private static ShelfBlockEntity getShelf(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof ShelfBlockEntity s ? s : null;
    }
}
