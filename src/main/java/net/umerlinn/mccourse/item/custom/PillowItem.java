package net.umerlinn.mccourse.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.umerlinn.mccourse.block.ModFurnitureBlocks;
import net.umerlinn.mccourse.block.custom.ConnectingBlock;
import net.umerlinn.mccourse.item.ModAttachments;

import java.util.Map;

/**
 * Normal right-click → places the pillow block on top of the surface (standard BlockItem logic).
 * Sneak + right-click on any occupied block → lays the pillow flush underneath, rendered by
 * RugRenderer at floor level, toggling it off again when clicked a second time.
 * Same pattern as RugItem but uses the PILLOW_POSITIONS attachment and 4px pillow model.
 */
public class PillowItem extends BlockItem {

    private final RugColor color;

    public PillowItem(RugColor color, Block block, Properties properties) {
        super(block, properties);
        this.color = color;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        // Clicking the TOP face with shift = standard BlockItem placement (pillow on top of the
        // block). This allows placing on interactive blocks (stonecutter, enchanting table, etc.)
        // that would otherwise open their UI on a non-shift right-click.
        if (context.getClickedFace() == Direction.UP) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (level.getBlockState(pos).isAir()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // On a sofa, the click height determines the zone:
        //   click Y > 7/16 above block base → backrest zone → SOFA_SEAT_POSITIONS (leaning pillow)
        //   click Y ≤ 7/16                  → base/floor zone → PILLOW_POSITIONS  (like a rug)
        // On any other block, always use PILLOW_POSITIONS (floor-level under-block).
        boolean isSofa = level.getBlockState(pos).getBlock() instanceof ConnectingBlock;
        boolean sofaBackrest = isSofa
                && (context.getClickLocation().y - pos.getY()) > (7.0 / 16.0);
        var attachment = sofaBackrest ? ModAttachments.SOFA_SEAT_POSITIONS : ModAttachments.PILLOW_POSITIONS;

        Map<BlockPos, RugColor> pillows = level.getData(attachment);
        RugColor existing = pillows.get(pos);

        if (existing != null) {
            pillows.remove(pos);
            level.setData(attachment, pillows);
            if (!player.isCreative()) {
                Block.popResource(level, pos, new ItemStack(ModFurnitureBlocks.PILLOWS.get(existing).get().asItem()));
            }
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.CONSUME;
        }

        pillows.put(pos, this.color);
        level.setData(attachment, pillows);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }
}
