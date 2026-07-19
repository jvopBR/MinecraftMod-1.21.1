package net.umerlinn.mccourse.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.umerlinn.mccourse.block.ModBlockEntities;
import net.umerlinn.mccourse.block.custom.StorageBookcaseBlock;
import org.jetbrains.annotations.Nullable;

/**
 * Direct per-slot storage like vanilla's ChiseledBookShelfBlockEntity — 6 slots, one book each,
 * no menu/GUI (StorageBookcaseBlock targets an exact slot by where the player clicks on the front
 * face). Deliberately NOT an AbstractStorageBlockEntity: that base is for the ChestMenu pattern
 * (Cabinet/Wardrobe), which this doesn't use at all.
 *
 * Can't reuse vanilla's ChiseledBookShelfBlockEntity directly — its constructor is hardcoded to
 * vanilla's own BlockEntityType.CHISELED_BOOKSHELF, which would reject existing at a position
 * whose block isn't in that exact type's registered valid-blocks list. Mirrors its structure
 * closely otherwise (same 6-slot Container contract, same ItemTags.BOOKSHELF_BOOKS restriction).
 */
public class StorageBookcaseBlockEntity extends BlockEntity implements Container {

    public static final int SLOTS = 6;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int lastInteractedSlot = -1;

    public StorageBookcaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STORAGE_BOOKCASE.get(), pos, state);
    }

    public int getLastInteractedSlot() {
        return lastInteractedSlot;
    }

    /**
     * Copied from vanilla ChiseledBookShelfBlockEntity#updateState: re-derives every
     * slot_N_occupied blockstate property from the current contents (not just the touched one —
     * simpler than tracking deltas) and pushes it with level.setBlock, which both syncs to the
     * client and marks the chunk dirty for saving on its own — no separate setChanged()/
     * sendBlockUpdated() needed, unlike this bookcase's old renderer-driven design.
     */
    private void updateState(int slot) {
        lastInteractedSlot = slot;
        if (level == null) return;

        BlockState state = getBlockState();
        for (int i = 0; i < StorageBookcaseBlock.SLOT_OCCUPIED_PROPERTIES.size(); i++) {
            BooleanProperty slotOccupied = StorageBookcaseBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
            state = state.setValue(slotOccupied, !getItem(i).isEmpty());
        }
        level.setBlock(worldPosition, state, 3);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (!removed.isEmpty()) updateState(slot);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.is(ItemTags.BOOKSHELF_BOOKS)) {
            items.set(slot, stack);
            updateState(slot);
        } else if (stack.isEmpty()) {
            removeItem(slot, 1);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ItemTags.BOOKSHELF_BOOKS) && getItem(slot).isEmpty() && stack.getCount() == getMaxStackSize();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, true, registries);
        tag.putInt("LastInteractedSlot", lastInteractedSlot);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        lastInteractedSlot = tag.getInt("LastInteractedSlot");
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
