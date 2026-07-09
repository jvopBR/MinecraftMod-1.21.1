package net.umerlinn.mccourse.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;
import net.umerlinn.mccourse.block.ModBlockEntities;

public class CabinetBlockEntity extends AbstractStorageBlockEntity {

    public static final int SLOTS = 36; // 9x4 — bigger than a chest, smaller than the wardrobe

    public CabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABINET.get(), pos, state, SLOTS);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.mccourse.cabinet");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        // No 3-arg ChestMenu.fourRows(...) overload exists (only threeRows/sixRows do) — use
        // the general constructor directly for a custom Container at a non-3/6 row count.
        return new ChestMenu(MenuType.GENERIC_9x4, id, playerInventory, this, 4);
    }
}
