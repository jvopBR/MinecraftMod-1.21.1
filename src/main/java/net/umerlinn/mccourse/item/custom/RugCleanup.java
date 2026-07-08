package net.umerlinn.mccourse.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.block.ModFurnitureBlocks;
import net.umerlinn.mccourse.item.ModAttachments;

import java.util.Map;

/**
 * A rug's position is tracked independently of whatever block sits above it (see RugItem), so
 * breaking that block wouldn't otherwise clear the rug entry — it would just sit there forever,
 * growing the level's rug map without bound. Drop the rug and forget the position whenever the
 * block above it is broken.
 */
@EventBusSubscriber(modid = MCCourseMod.MOD_ID)
public class RugCleanup {

    @SubscribeEvent
    static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = event.getPos();

        Map<BlockPos, RugColor> rugs = level.getExistingData(ModAttachments.RUG_POSITIONS).orElse(null);
        if (rugs != null) {
            RugColor removed = rugs.remove(pos);
            if (removed != null) {
                level.setData(ModAttachments.RUG_POSITIONS, rugs);
                Block.popResource(level, pos, new ItemStack(ModFurnitureBlocks.RUGS.get(removed).get().asItem()));
            }
        }

        Map<BlockPos, RugColor> pillows = level.getExistingData(ModAttachments.PILLOW_POSITIONS).orElse(null);
        if (pillows != null) {
            RugColor removed = pillows.remove(pos);
            if (removed != null) {
                level.setData(ModAttachments.PILLOW_POSITIONS, pillows);
                Block.popResource(level, pos, new ItemStack(ModFurnitureBlocks.PILLOWS.get(removed).get().asItem()));
            }
        }

        Map<BlockPos, RugColor> sofaSeats = level.getExistingData(ModAttachments.SOFA_SEAT_POSITIONS).orElse(null);
        if (sofaSeats != null) {
            RugColor removed = sofaSeats.remove(pos);
            if (removed != null) {
                level.setData(ModAttachments.SOFA_SEAT_POSITIONS, sofaSeats);
                Block.popResource(level, pos, new ItemStack(ModFurnitureBlocks.PILLOWS.get(removed).get().asItem()));
            }
        }
    }
}
