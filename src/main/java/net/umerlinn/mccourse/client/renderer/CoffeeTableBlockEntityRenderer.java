package net.umerlinn.mccourse.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.umerlinn.mccourse.block.entity.CoffeeTableBlockEntity;

public class CoffeeTableBlockEntityRenderer implements BlockEntityRenderer<CoffeeTableBlockEntity> {

    // Quadrant offsets from block center (world axes) — match CoffeeTableBlock.getClickedSlot's
    // 0=NW,1=NE,2=SW,3=SE mapping. The tabletop is symmetric under rotation, so unlike the
    // shelf, no per-facing rotation is needed here.
    private static final double[] SLOT_DX = {-4.0 / 16.0, 4.0 / 16.0, -4.0 / 16.0, 4.0 / 16.0};
    private static final double[] SLOT_DZ = {-4.0 / 16.0, -4.0 / 16.0, 4.0 / 16.0, 4.0 / 16.0};

    // Surface top sits at y=9/16 (see CoffeeTableBlock.NORTH_SHAPE); rest items just above it.
    private static final double ITEM_Y = 10.5 / 16.0;

    private static final float ITEM_SCALE = 0.4f;

    public CoffeeTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CoffeeTableBlockEntity table, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        for (int slot = 0; slot < CoffeeTableBlockEntity.SLOTS; slot++) {
            ItemStack item = table.getItem(slot);
            if (item.isEmpty()) continue;

            poseStack.pushPose();
            poseStack.translate(0.5 + SLOT_DX[slot], ITEM_Y, 0.5 + SLOT_DZ[slot]);
            if (table.isLying(slot)) {
                // Same rotation a floor-mounted vanilla item frame applies before drawing its
                // FIXED-pose item (see ItemFrameRenderer#render) — tips the item from
                // "standing, facing the viewer" to "lying flat, facing up".
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                    item,
                    ItemDisplayContext.FIXED,
                    combinedLight,
                    combinedOverlay,
                    poseStack,
                    bufferSource,
                    table.getLevel(),
                    slot
            );

            poseStack.popPose();
        }
    }
}
