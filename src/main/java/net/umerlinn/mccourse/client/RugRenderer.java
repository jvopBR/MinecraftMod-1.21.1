package net.umerlinn.mccourse.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.block.ModFurnitureBlocks;
import net.umerlinn.mccourse.block.custom.ConnectingBlock;
import net.umerlinn.mccourse.block.custom.PillowBlock;
import net.umerlinn.mccourse.item.ModAttachments;
import net.umerlinn.mccourse.item.custom.RugColor;

import java.util.Map;

/**
 * Draws every tracked rug / pillow / sofa-seat-pillow position directly, regardless of what
 * block (ours, vanilla, or another mod's) occupies that cell.
 *
 * Must use AFTER_BLOCK_ENTITIES — that stage is dispatched with the real camera-relative
 * PoseStack, unlike the RenderType-keyed stages which pass null.
 *
 * Pillows rendered inside another block's geometry use polygon offset so their texture always
 * wins the depth test against the host geometry, preventing z-fighting / flickering.
 */
@EventBusSubscriber(modid = MCCourseMod.MOD_ID, value = Dist.CLIENT)
public class RugRenderer {

    private static final double MAX_DISTANCE_SQ = 64.0 * 64.0;

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        Map<BlockPos, RugColor> rugs      = level.getExistingData(ModAttachments.RUG_POSITIONS).orElse(null);
        Map<BlockPos, RugColor> pillows   = level.getExistingData(ModAttachments.PILLOW_POSITIONS).orElse(null);
        Map<BlockPos, RugColor> sofaSeats = level.getExistingData(ModAttachments.SOFA_SEAT_POSITIONS).orElse(null);

        boolean hasRugs      = rugs      != null && !rugs.isEmpty();
        boolean hasPillows   = pillows   != null && !pillows.isEmpty();
        boolean hasSofaSeats = sofaSeats != null && !sofaSeats.isEmpty();

        if (!hasRugs && !hasPillows && !hasSofaSeats) {
            return;
        }

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        // --- Rugs (flat 1px carpet, floor level) ---
        if (hasRugs) {
            for (Map.Entry<BlockPos, RugColor> entry : rugs.entrySet()) {
                BlockPos pos = entry.getKey();
                if (pos.distToCenterSqr(camPos.x, camPos.y, camPos.z) > MAX_DISTANCE_SQ || !level.isLoaded(pos)) {
                    continue;
                }
                BlockState rugState = ModFurnitureBlocks.RUGS.get(entry.getValue()).get().defaultBlockState();
                int packedLight = LevelRenderer.getLightColor(level, pos);
                poseStack.pushPose();
                poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
                Minecraft.getInstance().getBlockRenderer()
                        .renderSingleBlock(rugState, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
            bufferSource.endBatch();
        }

        // --- Floor-level pillows (under any non-sofa block) and sofa-backrest pillows ---
        // Both use polygon offset so they win the depth test against the host block's geometry.
        if (hasPillows || hasSofaSeats) {
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-1.0F, -10.0F);

            if (hasPillows) {
                for (Map.Entry<BlockPos, RugColor> entry : pillows.entrySet()) {
                    BlockPos pos = entry.getKey();
                    if (pos.distToCenterSqr(camPos.x, camPos.y, camPos.z) > MAX_DISTANCE_SQ || !level.isLoaded(pos)) {
                        continue;
                    }
                    BlockState pillowState = ModFurnitureBlocks.PILLOWS.get(entry.getValue()).get().defaultBlockState();
                    int packedLight = LevelRenderer.getLightColor(level, pos);
                    poseStack.pushPose();
                    poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
                    Minecraft.getInstance().getBlockRenderer()
                            .renderSingleBlock(pillowState, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            }

            if (hasSofaSeats) {
                for (Map.Entry<BlockPos, RugColor> entry : sofaSeats.entrySet()) {
                    BlockPos pos = entry.getKey();
                    if (pos.distToCenterSqr(camPos.x, camPos.y, camPos.z) > MAX_DISTANCE_SQ || !level.isLoaded(pos)) {
                        continue;
                    }
                    BlockState host = level.getBlockState(pos);
                    // Use the sofa's own FACING so the blockstate rotation orients the pillow
                    // correctly against the backrest regardless of which way the sofa faces.
                    Direction sofaFacing = host.getBlock() instanceof ConnectingBlock
                            ? host.getValue(ConnectingBlock.FACING)
                            : Direction.NORTH;
                    BlockState pillowState = ModFurnitureBlocks.PILLOWS.get(entry.getValue()).get()
                            .defaultBlockState()
                            .setValue(PillowBlock.FACING, sofaFacing)
                            .setValue(PillowBlock.HEIGHT_OFFSET, 3);
                    int packedLight = LevelRenderer.getLightColor(level, pos);
                    poseStack.pushPose();
                    // No extra Y offset — the sofa-pillow model coordinates (y=7..15, z=8..11)
                    // are already expressed in the sofa block's own space.
                    poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
                    Minecraft.getInstance().getBlockRenderer()
                            .renderSingleBlock(pillowState, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
                    poseStack.popPose();
                }
            }

            bufferSource.endBatch();
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
        }
    }
}
