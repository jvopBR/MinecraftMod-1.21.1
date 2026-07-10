package net.umerlinn.mccourse.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.umerlinn.mccourse.block.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Shared across all three candle holder blocks (floor/wall/hanging) — up to 6 real vanilla candle
 * ItemStacks of any color (same idea as the old CandelabraBlockEntity, see
 * mug_and_candelabra_props/candle_holder_family memory), one per arm. Starts empty; each right
 * click with a candle fills the next slot, growing a new arm in the block model (see
 * AbstractCandleHolderBlock.CANDLES).
 */
public class CandleHolderBlockEntity extends BlockEntity {

    public static final int SLOTS = 4;
    private final ItemStack[] candles = new ItemStack[SLOTS];

    public CandleHolderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CANDLE_HOLDER.get(), pos, state);
        Arrays.fill(candles, ItemStack.EMPTY);
    }

    public ItemStack getCandle(int slot) {
        return candles[slot];
    }

    public void setCandle(int slot, ItemStack stack) {
        candles[slot] = stack;
        setChanged();
    }

    public boolean isEmpty() {
        return filledCount() == 0;
    }

    public int filledCount() {
        int count = 0;
        for (ItemStack s : candles) {
            if (!s.isEmpty()) count++;
        }
        return count;
    }

    /** First empty slot, or -1 if every slot already has a candle. */
    public int firstEmptySlot() {
        for (int i = 0; i < SLOTS; i++) {
            if (candles[i].isEmpty()) return i;
        }
        return -1;
    }

    /**
     * Last filled slot (highest index), or -1 if empty. Removing from the end gives a simple,
     * predictable fill/empty order without needing the player to aim at one specific arm.
     */
    public int lastFilledSlot() {
        for (int i = SLOTS - 1; i >= 0; i--) {
            if (!candles[i].isEmpty()) return i;
        }
        return -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < SLOTS; i++) {
            if (!candles[i].isEmpty()) tag.put("Candle" + i, candles[i].save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < SLOTS; i++) {
            candles[i] = tag.contains("Candle" + i)
                    ? ItemStack.parseOptional(registries, tag.getCompound("Candle" + i))
                    : ItemStack.EMPTY;
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
