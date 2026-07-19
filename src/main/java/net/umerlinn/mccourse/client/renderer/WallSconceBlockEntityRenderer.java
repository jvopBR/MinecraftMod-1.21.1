package net.umerlinn.mccourse.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.umerlinn.mccourse.block.ModFurnitureBlocks;
import net.umerlinn.mccourse.block.custom.WallSconceBlock;
import net.umerlinn.mccourse.block.entity.WallSconceBlockEntity;

/**
 * Draws the sconce's hanging lantern (the hidden WALL_SCONCE_LANTERN block's model, never
 * placeable) every frame, rotated by the BlockEntity's damped-pendulum angle around the hook
 * pivot. The swing axis after facing alignment is local Z — the wall's outward normal — so the
 * lantern swings sideways along the wall, the way a brushed hanging lantern naturally moves.
 * Facing alignment reuses the wardrobe renderer's in-game-confirmed blockstateYRot values
 * (EAST/WEST swapped vs. the JSON convention — see WardrobeBlockEntityRenderer's doc comment).
 */
public class WallSconceBlockEntityRenderer implements BlockEntityRenderer<WallSconceBlockEntity> {

    // Hook pivot in NORTH-pose local space: top of the hanger link, where it meets the arm.
    private static final float PIVOT_X = 8.0f / 16.0f;
    private static final float PIVOT_Y = 11.0f / 16.0f;
    private static final float PIVOT_Z = 7.8f / 16.0f;

    public WallSconceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(WallSconceBlockEntity sconce, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        if (sconce.getLevel() == null) {
            return;
        }
        Direction facing = sconce.getBlockState().getValue(WallSconceBlock.FACING);
        float angle = sconce.swingAngleRadians(sconce.getLevel().getGameTime(), partialTick);

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(blockstateYRot(facing)));
        poseStack.translate(-0.5, 0, -0.5);
        if (angle != 0.0f) {
            poseStack.rotateAround(Axis.ZP.rotation(angle), PIVOT_X, PIVOT_Y, PIVOT_Z);
        }

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                ModFurnitureBlocks.WALL_SCONCE_LANTERN.get().defaultBlockState(),
                poseStack, bufferSource, combinedLight, combinedOverlay, ModelData.EMPTY, null);

        poseStack.popPose();
    }

    // Same values WardrobeBlockEntityRenderer uses (confirmed in-game): Axis.YP rotates with
    // opposite handedness from the JSON model loader, so EAST/WEST are swapped vs. the
    // blockstate convention while NORTH/SOUTH (sin=0) are unaffected.
    private static float blockstateYRot(Direction facing) {
        return switch (facing) {
            case NORTH -> 0F;
            case EAST -> 270F;
            case SOUTH -> 180F;
            case WEST -> 90F;
            default -> 0F;
        };
    }
}
