package net.umerlinn.mccourse.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.umerlinn.mccourse.block.ModBlockEntities;

/**
 * Door-swing animation state mirrors vanilla ChestBlockEntity exactly: ContainerOpenersCounter
 * tracks how many players have the menu open (both sides, drives the open/close sound + the
 * synced block event), ChestLidController is the actual smoothly-interpolated angle (client-side
 * only — the server never reads it, just relays the open/closed flag via the block event). Both
 * are reused directly from vanilla rather than reimplemented.
 */
public class WardrobeBlockEntity extends AbstractStorageBlockEntity implements LidBlockEntity {

    public static final int SLOTS = 54; // 9x6 — bigger than the cabinet

    private static final int EVENT_SET_OPEN_COUNT = 1;

    private final ChestLidController lidController = new ChestLidController();
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
            level.blockEvent(pos, state.getBlock(), EVENT_SET_OPEN_COUNT, newCount);
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu chestMenu && chestMenu.getContainer() == WardrobeBlockEntity.this;
        }
    };

    public WardrobeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARDROBE.get(), pos, state, SLOTS);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.mccourse.wardrobe");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        return ChestMenu.sixRows(id, playerInventory, this);
    }

    @Override
    public void startOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.incrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.decrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == EVENT_SET_OPEN_COUNT) {
            this.lidController.shouldBeOpen(type > 0);
            return true;
        }
        return super.triggerEvent(id, type);
    }

    @Override
    public float getOpenNess(float partialTicks) {
        return this.lidController.getOpenness(partialTicks);
    }

    public static void lidAnimateTick(Level level, BlockPos pos, BlockState state, WardrobeBlockEntity blockEntity) {
        blockEntity.lidController.tickLid();
    }
}
