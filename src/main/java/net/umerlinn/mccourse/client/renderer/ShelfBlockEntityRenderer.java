package net.umerlinn.mccourse.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.umerlinn.mccourse.block.custom.HorizontalFurnitureBlock;
import net.umerlinn.mccourse.block.custom.ShelfBlock;
import net.umerlinn.mccourse.block.entity.ShelfBlockEntity;

public class ShelfBlockEntityRenderer implements BlockEntityRenderer<ShelfBlockEntity> {

    // Horizontal slot offsets from block center along the shelf's width axis.
    // Slots are evenly spaced at -4/16, 0, +4/16 from center.
    private static final double[] SLOT_OFFSETS = {-4.0 / 16.0, 0, 4.0 / 16.0};

    // Surface depth center is at (6+14)/2 = 10/16. Block center is at 8/16.
    // Backward offset = 10/16 - 8/16 = 2/16.
    private static final double BACK_OFFSET = 2.0 / 16.0;

    // Item center Y: just above surface top (y=10/16) so item appears to rest on shelf.
    private static final double ITEM_Y = 11.5 / 16.0;

    // Scale applied on top of ItemDisplayContext.FIXED's own model transform.
    // Keeps items compact enough to fit 3 side-by-side without looking oversized.
    private static final float ITEM_SCALE = 0.4f;

    public ShelfBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ShelfBlockEntity shelf, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        BlockState state = shelf.getBlockState();
        Direction facing = state.getValue(ShelfBlock.FACING);

        // width axis: perpendicular to facing, used for slot spacing
        Direction perp = facing.getClockWise();
        var pv = perp.getNormal();

        // depth axis: toward the back wall
        Direction back = facing.getOpposite();
        var bv = back.getNormal();

        float yRot = facing.toYRot(); // aligns item to face viewer regardless of shelf orientation

        for (int slot = 0; slot < ShelfBlockEntity.SLOTS; slot++) {
            ItemStack item = shelf.getItem(slot);
            if (item.isEmpty()) continue;

            double dx = 0.5 + pv.getX() * SLOT_OFFSETS[slot] + bv.getX() * BACK_OFFSET;
            double dz = 0.5 + pv.getZ() * SLOT_OFFSETS[slot] + bv.getZ() * BACK_OFFSET;

            poseStack.pushPose();
            poseStack.translate(dx, ITEM_Y, dz);
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    item,
                    ItemDisplayContext.FIXED,
                    combinedLight,
                    combinedOverlay,
                    poseStack,
                    bufferSource,
                    shelf.getLevel(),
                    slot
            );

            poseStack.popPose();
        }
    }
}
