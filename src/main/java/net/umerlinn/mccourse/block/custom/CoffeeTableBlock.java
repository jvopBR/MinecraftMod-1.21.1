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
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
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
import net.umerlinn.mccourse.block.entity.CoffeeTableBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CoffeeTableBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Short coffee-table-height legs (7px) topped by a 2px surface at y=7..9 — low enough
    // that items placed on top (via CoffeeTableBlockEntityRenderer) sit at coffee-table
    // height instead of normal table/counter height.
    public static final VoxelShape NORTH_SHAPE = FurnitureShapes.boxes(
            2, 0, 2, 4, 7, 4,
            12, 0, 2, 14, 7, 4,
            2, 0, 12, 4, 7, 14,
            12, 0, 12, 14, 7, 14,
            1, 7, 1, 15, 9, 15
    );

    public static final MapCodec<CoffeeTableBlock> CODEC =
            simpleCodec(props -> new CoffeeTableBlock(props, NORTH_SHAPE));

    private final Map<Direction, VoxelShape> shapesByFacing;

    public CoffeeTableBlock(Properties properties, VoxelShape northShape) {
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
        return new CoffeeTableBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        CoffeeTableBlockEntity table = getTable(level, pos);
        if (table == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        int slot = getClickedSlot(hit, pos);
        ItemStack displayed = table.getItem(slot);

        if (!displayed.isEmpty()) {
            player.addItem(displayed.copy());
            table.setItem(slot, ItemStack.EMPTY, false);
        } else {
            // Tools/weapons always lie flat on their side — standing upright looks wrong on a
            // low table (see isToolOrWeapon). Everything else keeps the default standing pose.
            // NOTE: this can't be a sneak+click choice — ServerPlayerGameMode#useItemOn skips
            // useItemOn/useWithoutItem entirely while sneaking with an item in hand (the same
            // bypass that lets you place a block against a chest instead of opening it), so a
            // sneaking player right-clicking here never even reaches this method.
            table.setItem(slot, stack.copyWithCount(1), isToolOrWeapon(stack));
            if (!player.isCreative()) stack.shrink(1);
        }
        table.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        CoffeeTableBlockEntity table = getTable(level, pos);
        if (table == null) return InteractionResult.PASS;

        int slot = getClickedSlot(hit, pos);
        ItemStack displayed = table.getItem(slot);
        if (displayed.isEmpty()) return InteractionResult.PASS;

        player.addItem(displayed.copy());
        table.setItem(slot, ItemStack.EMPTY, false);
        table.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            CoffeeTableBlockEntity table = getTable(level, pos);
            if (table != null) {
                for (int i = 0; i < CoffeeTableBlockEntity.SLOTS; i++) {
                    ItemStack s = table.getItem(i);
                    if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // Maps the click location to one of 4 quadrants (NW/NE/SW/SE in world space). The
    // tabletop is symmetric under rotation, so — unlike ShelfBlock — this doesn't need to
    // account for FACING at all; world-space quadrants line up with the renderer's slot
    // offsets directly.
    private static int getClickedSlot(BlockHitResult hit, BlockPos pos) {
        double dx = hit.getLocation().x - pos.getX() - 0.5;
        double dz = hit.getLocation().z - pos.getZ() - 0.5;
        int xi = dx < 0 ? 0 : 1;
        int zi = dz < 0 ? 0 : 1;
        return zi * 2 + xi; // 0=NW, 1=NE, 2=SW, 3=SE
    }

    @Nullable
    private static CoffeeTableBlockEntity getTable(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof CoffeeTableBlockEntity t ? t : null;
    }

    // Swords/axes/pickaxes/shovels/hoes all extend TieredItem; trident and mace stand alone.
    // Covers modded tools/weapons too, since most extend these same vanilla base classes.
    private static boolean isToolOrWeapon(ItemStack stack) {
        return stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof MaceItem;
    }
}
