package net.umerlinn.mccourse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.block.ModFurnitureBlocks;
import net.umerlinn.mccourse.item.ModAttachments;
import net.umerlinn.mccourse.item.custom.RugColor;

import java.util.Map;

/**
 * Draws every tracked rug position directly, regardless of what block (ours, vanilla, or
 * another mod's) occupies that cell — the rug isn't attached to that block at all, so this
 * works uniformly everywhere instead of needing a per-block-type renderer hook.
 *
 * Must use a stage that actually receives a camera-relative PoseStack. The RenderType-keyed
 * stages (AFTER_SOLID_BLOCKS, AFTER_CUTOUT_BLOCKS, ...) are dispatched from
 * LevelRenderer#renderChunkLayer via the RenderType-only ClientHooks.dispatchRenderStage
 * overload, which hardcodes poseStack=null — so event.getPoseStack() there is a disconnected
 * fresh PoseStack, not the real view transform (confirmed by reading LevelRenderer directly).
 * AFTER_BLOCK_ENTITIES is dispatched with the real posestack, right after normal block entities
 * (like beds) render, so that's what we hook instead.
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

        Map<BlockPos, RugColor> rugs = level.getExistingData(ModAttachments.RUG_POSITIONS).orElse(null);
        if (rugs == null || rugs.isEmpty()) {
            return;
        }

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

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
}
