package net.umerlinn.mccourse.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.umerlinn.mccourse.block.entity.StorageBookcaseBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Single-block bookcase modeled directly on vanilla's ChiseledBookShelfBlock — practically a copy
 * of its mechanics, just with this mod's own per-wood blocks/textures instead of vanilla's oak-only
 * asset. 6 individually targeted slots (2 rows x 3 columns) on the front face, one book each, no
 * menu/GUI at all — right-click a specific spot with a book to fill just that slot, right-click
 * empty-handed on a filled spot to take just that book back out. Can't reuse vanilla's block class
 * directly (its newBlockEntity is hardcoded to vanilla's own BlockEntityType), so this reuses
 * vanilla's own SLOT_OCCUPIED_PROPERTIES list directly (Property instances are shared across
 * unrelated blocks all the time, e.g. FACING below) and copies getHitSlot/
 * getRelativeHitCoordinatesForBlockFace/getSection verbatim, including vanilla's exact uneven
 * 0.375/0.6875 column split — the multipart slot-overlay quads (see
 * template_storage_bookcase_slot_*.json) are the same uneven widths, so the click target and the
 * visible slot boundary have to agree.
 */
public class StorageBookcaseBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final List<BooleanProperty> SLOT_OCCUPIED_PROPERTIES = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES;

    public static final MapCodec<StorageBookcaseBlock> CODEC = simpleCodec(StorageBookcaseBlock::new);

    public StorageBookcaseBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any().setValue(FACING, Direction.NORTH);
        for (BooleanProperty slotOccupied : SLOT_OCCUPIED_PROPERTIES) {
            state = state.setValue(slotOccupied, false);
        }
        this.registerDefaultState(state);
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        SLOT_OCCUPIED_PROPERTIES.forEach(builder::add);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageBookcaseBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof StorageBookcaseBlockEntity bookcase)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!stack.is(ItemTags.BOOKSHELF_BOOKS)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        OptionalInt slot = getHitSlot(hit, state);
        if (slot.isEmpty()) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(SLOT_OCCUPIED_PROPERTIES.get(slot.getAsInt()))) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!level.isClientSide()) {
            SoundEvent sound = stack.is(Items.ENCHANTED_BOOK)
                    ? SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED
                    : SoundEvents.CHISELED_BOOKSHELF_INSERT;
            bookcase.setItem(slot.getAsInt(), stack.consumeAndReturn(1, player));
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof StorageBookcaseBlockEntity bookcase)) {
            return InteractionResult.PASS;
        }

        OptionalInt slot = getHitSlot(hit, state);
        if (slot.isEmpty()) return InteractionResult.PASS;
        if (!state.getValue(SLOT_OCCUPIED_PROPERTIES.get(slot.getAsInt()))) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            ItemStack removed = bookcase.removeItem(slot.getAsInt(), 1);
            SoundEvent sound = removed.is(Items.ENCHANTED_BOOK)
                    ? SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED
                    : SoundEvents.CHISELED_BOOKSHELF_PICKUP;
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.getInventory().add(removed)) {
                player.drop(removed, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Which of the 6 slots (row*3 + column) the player's click landed on, empty if they didn't hit the front face. */
    private static OptionalInt getHitSlot(BlockHitResult hit, BlockState state) {
        return relativeHitCoordinates(hit, state.getValue(FACING)).map(p -> {
            int row = p.y >= 0.5F ? 0 : 1;
            int column = column(p.x);
            return OptionalInt.of(column + row * 3);
        }).orElseGet(OptionalInt::empty);
    }

    private static Optional<Vec2> relativeHitCoordinates(BlockHitResult hit, Direction facing) {
        Direction hitFace = hit.getDirection();
        if (hitFace != facing) return Optional.empty();

        BlockPos pos = hit.getBlockPos().relative(hitFace);
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        return switch (hitFace) {
            case NORTH -> Optional.of(new Vec2((float) (1.0 - local.x()), (float) local.y()));
            case SOUTH -> Optional.of(new Vec2((float) local.x(), (float) local.y()));
            case WEST -> Optional.of(new Vec2((float) local.z(), (float) local.y()));
            case EAST -> Optional.of(new Vec2((float) (1.0 - local.z()), (float) local.y()));
            case UP, DOWN -> Optional.empty();
        };
    }

    /** Matches vanilla ChiseledBookShelfBlock#getSection exactly (uneven thirds: 0.375 / 0.6875 — not 1/3 / 2/3). */
    private static int column(float x) {
        if (x < 0.375F) return 0;
        return x < 0.6875F ? 1 : 2;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            boolean hadContent = false;
            if (level.getBlockEntity(pos) instanceof StorageBookcaseBlockEntity bookcase && !bookcase.isEmpty()) {
                for (int i = 0; i < StorageBookcaseBlockEntity.SLOTS; i++) {
                    ItemStack stack = bookcase.getItem(i);
                    if (!stack.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
                bookcase.clearContent();
                hadContent = true;
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
            if (hadContent) level.updateNeighbourForOutputSignal(pos, this);
        } else {
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) return 0;
        return level.getBlockEntity(pos) instanceof StorageBookcaseBlockEntity bookcase
                ? bookcase.getLastInteractedSlot() + 1
                : 0;
    }
}
