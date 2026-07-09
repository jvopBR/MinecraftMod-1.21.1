package net.umerlinn.mccourse.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared plumbing for the cabinet/wardrobe BlockEntities, mirroring vanilla BarrelBlockEntity
 * (NonNullList of items + loot-table-on-first-open support via RandomizableContainerBlockEntity).
 * Subclasses just provide a size, a menu factory and a display name.
 */
public abstract class AbstractStorageBlockEntity extends RandomizableContainerBlockEntity {

    private final int containerSize;
    private NonNullList<ItemStack> items;

    protected AbstractStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int containerSize) {
        super(type, pos, state);
        this.containerSize = containerSize;
        this.items = NonNullList.withSize(containerSize, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return containerSize;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.items, registries);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(containerSize, ItemStack.EMPTY);
        if (!this.tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.items, registries);
        }
    }
}
