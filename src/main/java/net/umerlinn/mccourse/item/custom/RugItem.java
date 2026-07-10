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
import net.umerlinn.mccourse.item.ModAttachments;

import java.util.Map;

/**
 * A normal placeable carpet block item — right-clicking open floor places it like vanilla
 * carpet. Sneak-right-clicking the SIDE of any block (ours, vanilla, or another mod's — doesn't
 * matter, since the rug's position is tracked independently of whatever block sits there) lays
 * the rug flush underneath it instead, toggling it off again if one is already there. This avoids
 * the "bed floating a block above a separately-placed carpet" gap: see RugRenderer, which draws
 * the rug directly at the tracked position regardless of what block occupies it. Restricted to
 * side faces (not top/bottom) so it can't be triggered by sneak-clicking the ground the player is
 * standing on, which would tuck an invisible, wasted rug underneath solid terrain.
 */
public class RugItem extends BlockItem {

    private final RugColor color;

    public RugItem(RugColor color, Block block, Properties properties) {
        super(block, properties);
        this.color = color;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS; // normal (non-sneak) click: fall through to standard carpet placement
        }

        Direction face = context.getClickedFace();
        if (face == Direction.UP || face == Direction.DOWN) {
            // Only a side click unambiguously means "slip a rug under THIS block" — clicking the
            // top face (e.g. the ground the player is standing on) was letting rugs get tucked
            // underneath solid terrain, invisible and wasted.
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (level.getBlockState(pos).isAir()) {
            return InteractionResult.PASS; // nothing to lay a rug under here — place normally instead
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Map<BlockPos, RugColor> rugs = level.getData(ModAttachments.RUG_POSITIONS);
        RugColor existing = rugs.get(pos);

        if (existing != null) {
            rugs.remove(pos);
            level.setData(ModAttachments.RUG_POSITIONS, rugs);
            if (!player.isCreative()) {
                Block.popResource(level, pos, new ItemStack(ModFurnitureBlocks.RUGS.get(existing).get().asItem()));
            }
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.CONSUME;
        }

        rugs.put(pos, this.color);
        level.setData(ModAttachments.RUG_POSITIONS, rugs);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }
}
