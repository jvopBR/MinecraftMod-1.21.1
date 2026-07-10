package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.umerlinn.mccourse.block.entity.CandleHolderBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared behaviour for the three candle holder placements (floor/wall/hanging) — replaces the old
 * 6-slot CandelabraBlock entirely (see mug_and_candelabra_props/candle_holder_family memory for
 * what came before). Extending vanilla's own AbstractCandleBlock (rather than BaseEntityBlock,
 * like the old candelabra did) is deliberate: it gives real ignite/extinguish parity —
 * animateTick's particle+sound ambiance, flaming-arrow ignition (onProjectileHit) and
 * explosion-extinguish (onExplosionHit) — for free, instead of reimplementing them.
 *
 * Holds up to {@link CandleHolderBlockEntity#SLOTS} real vanilla candle ItemStacks (any of the 17
 * colors, detected via {@code instanceof CandleBlock} rather than a specific registry entry) — a
 * new arm grows in the block model each time another candle is added (see CANDLES below), same
 * idea as the old Candelabra but now with real fire. LIT is a single shared on/off for the whole
 * holder (matches how vanilla's own CandleBlock treats a 1-4 candle stack as one LIT toggle, not
 * per-candle) and, like CANDLES, lives on the blockstate — light emission can only ever read
 * BlockState, never a BlockEntity — see mug_and_candelabra_props memory).
 */
public abstract class AbstractCandleHolderBlock extends AbstractCandleBlock implements EntityBlock {

    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT = AbstractCandleBlock.LIT;
    public static final IntegerProperty CANDLES = IntegerProperty.create("candles", 0, CandleHolderBlockEntity.SLOTS);

    protected AbstractCandleHolderBlock(Properties properties) {
        super(properties);
    }

    /**
     * Fixed world-local (0-1) position for each of the 6 possible arms, in slot order. Slot 0 is
     * always the base single-candle position (unchanged from before this holder supported more
     * than one); slots 1-5 are the arms that appear as more candles are added. Every concrete
     * subclass returns an array of exactly {@code CandleHolderBlockEntity.SLOTS} entries.
     */
    public abstract Vec3[] candleOffsets(BlockState state);

    /** Uniform scale applied to each rendered candle around its own base-center point. 1 = natural (real block) size. */
    public float candleScale(BlockState state) {
        return 1.0f;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CandleHolderBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, CANDLES);
    }

    @Override
    protected Iterable<Vec3> getParticleOffsets(BlockState state) {
        Vec3[] offsets = candleOffsets(state);
        int count = Math.min(state.getValue(CANDLES), offsets.length);
        List<Vec3> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) result.add(offsets[i]);
        return result;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof CandleHolderBlockEntity holder)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stack.isEmpty()) {
            if (state.getValue(LIT)) {
                if (!level.isClientSide()) AbstractCandleBlock.extinguish(player, state, level, pos);
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            int lastFilled = holder.lastFilledSlot();
            if (lastFilled != -1 && player.getAbilities().mayBuild) {
                if (!level.isClientSide()) {
                    ItemStack removed = holder.getCandle(lastFilled);
                    holder.setCandle(lastFilled, ItemStack.EMPTY);
                    player.addItem(removed);
                    syncHolder(level, pos, state, holder);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int emptySlot = holder.firstEmptySlot();
        if (emptySlot == -1 || !(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof CandleBlock)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide()) {
            holder.setCandle(emptySlot, stack.copyWithCount(1));
            if (!player.isCreative()) stack.shrink(1);
            syncHolder(level, pos, state, holder);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * CandleHolderBlockEntity.setChanged() only marks the chunk dirty for saving — it does not by
     * itself send anything to the client, and CANDLES needs to change too (it mirrors
     * holder.filledCount() so the model and the light level can both react, exactly like the old
     * Candelabra's FILLED property). Without this, the server-side candle/count is set correctly
     * but the client's copy never finds out, and CandleHolderBlockEntityRenderer — which only
     * ever reads the client's copy — renders nothing.
     */
    private static void syncHolder(Level level, BlockPos pos, BlockState state, CandleHolderBlockEntity holder) {
        holder.setChanged();
        BlockState updated = state.setValue(CANDLES, holder.filledCount());
        level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        level.sendBlockUpdated(pos, updated, updated, Block.UPDATE_ALL);
    }

    @Nullable
    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        if (itemAbility == ItemAbilities.FIRESTARTER_LIGHT && canLight(context.getLevel(), context.getClickedPos(), state)) {
            return state.setValue(LIT, true);
        }
        return super.getToolModifiedState(state, context, itemAbility, simulate);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        BlockPos pos = hit.getBlockPos();
        if (!level.isClientSide && projectile.isOnFire() && canLight(level, pos, state)) {
            level.setBlockAndUpdate(pos, state.setValue(LIT, true));
        }
    }

    private static boolean canLight(Level level, BlockPos pos, BlockState state) {
        return !state.getValue(LIT) && level.getBlockEntity(pos) instanceof CandleHolderBlockEntity holder && !holder.isEmpty();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof CandleHolderBlockEntity holder && !holder.isEmpty()) {
                for (int i = 0; i < CandleHolderBlockEntity.SLOTS; i++) {
                    ItemStack candle = holder.getCandle(i);
                    if (!candle.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), candle);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
