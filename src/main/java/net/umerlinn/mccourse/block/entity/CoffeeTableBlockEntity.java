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

public class CoffeeTableBlockEntity extends BlockEntity {

    public static final int SLOTS = 4;
    private final ItemStack[] items =
            new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    // Per-slot display orientation: false = standing (default FIXED pose), true = lying flat
    // on its side, chosen by the player at placement time (sneak+right-click).
    private final boolean[] lying = new boolean[SLOTS];

    public CoffeeTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COFFEE_TABLE.get(), pos, state);
    }

    public ItemStack getItem(int slot) {
        return items[slot];
    }

    public boolean isLying(int slot) {
        return lying[slot];
    }

    public void setItem(int slot, ItemStack stack, boolean lying) {
        items[slot] = stack;
        this.lying[slot] = lying;
        setChanged();
    }

    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < SLOTS; i++) {
            if (!items[i].isEmpty()) {
                tag.put("Item" + i, items[i].save(registries));
                if (lying[i]) tag.putBoolean("Lying" + i, true);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < SLOTS; i++) {
            items[i] = tag.contains("Item" + i)
                    ? ItemStack.parseOptional(registries, tag.getCompound("Item" + i))
                    : ItemStack.EMPTY;
            lying[i] = tag.getBoolean("Lying" + i);
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
