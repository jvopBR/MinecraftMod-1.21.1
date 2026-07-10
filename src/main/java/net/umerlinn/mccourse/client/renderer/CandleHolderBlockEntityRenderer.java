package net.umerlinn.mccourse.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.umerlinn.mccourse.block.custom.AbstractCandleHolderBlock;
import net.umerlinn.mccourse.block.entity.CandleHolderBlockEntity;

/**
 * Shared by all three candle holder blocks (floor/wall/hanging) — draws every filled slot's real
 * candle (any of vanilla's 17 colors) at the position the block itself reports via
 * candleOffsets(), same slot-loop technique as the old CandelabraBlockEntityRenderer.
 *
 * Renders the candle's actual BLOCK model via BlockRenderDispatcher.renderSingleBlock, not its
 * item form (tried ItemDisplayContext.GROUND first — still visibly an "item on display" look, not
 * a real placed candle). This draws the exact same baked model a normally-placed candle uses —
 * correct proportions, correct texture, and the correct lit/unlit variant if vanilla has one —
 * since it *is* the same rendering path, just repositioned onto the holder's arm instead of the
 * ground.
 */
public class CandleHolderBlockEntityRenderer implements BlockEntityRenderer<CandleHolderBlockEntity> {

    public CandleHolderBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CandleHolderBlockEntity holder, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        if (!(holder.getBlockState().getBlock() instanceof AbstractCandleHolderBlock block)) return;
        boolean lit = holder.getBlockState().getValue(AbstractCandleHolderBlock.LIT);
        Vec3[] offsets = block.candleOffsets(holder.getBlockState());

        for (int slot = 0; slot < CandleHolderBlockEntity.SLOTS; slot++) {
            ItemStack candle = holder.getCandle(slot);
            if (candle.isEmpty() || slot >= offsets.length) continue;
            if (!(candle.getItem() instanceof BlockItem blockItem)) continue;

            BlockState candleState = blockItem.getBlock().defaultBlockState();
            if (candleState.hasProperty(CandleBlock.LIT)) {
                candleState = candleState.setValue(CandleBlock.LIT, lit);
            }

            Vec3 offset = offsets[slot];
            float scale = block.candleScale(holder.getBlockState());
            poseStack.pushPose();
            // The candle's own model is centered at local (0.5, 0, 0.5) with its base at y=0.
            // Translate to the target offset *first*, then scale, then shift by the candle's own
            // local center so scaling happens around the offset point instead of the block
            // origin — otherwise a scale != 1 would drag the candle off its intended position.
            poseStack.translate(offset.x, offset.y, offset.z);
            poseStack.scale(scale, scale, scale);
            poseStack.translate(-0.5, 0, -0.5);

            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    candleState,
                    poseStack,
                    bufferSource,
                    combinedLight,
                    combinedOverlay,
                    ModelData.EMPTY,
                    null
            );

            poseStack.popPose();
        }
    }
}
