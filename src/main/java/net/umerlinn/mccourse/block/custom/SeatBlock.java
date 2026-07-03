package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.umerlinn.mccourse.entity.ModEntities;
import net.umerlinn.mccourse.entity.SeatEntity;

import java.util.List;

public class SeatBlock extends HorizontalFurnitureBlock {

    public SeatBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        List<SeatEntity> existing = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
        if (!existing.isEmpty()) return InteractionResult.PASS;

        SeatEntity seat = new SeatEntity(ModEntities.SEAT.get(), level);
        seat.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(seat);
        player.startRiding(seat, true);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).forEach(entity -> {
                entity.ejectPassengers();
                entity.discard();
            });
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
