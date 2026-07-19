package net.umerlinn.mccourse.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.umerlinn.mccourse.block.entity.CoffeeTableBlockEntity;

public class CoffeeTableBlockEntityRenderer implements BlockEntityRenderer<CoffeeTableBlockEntity> {

    // Quadrant offsets from block center (world axes) — match CoffeeTableBlock.getClickedSlot's
    // 0=NW,1=NE,2=SW,3=SE mapping. The tabletop is symmetric under rotation, so unlike the
    // shelf, no per-facing rotation is needed here.
    private static final double[] SLOT_DX = {-4.0 / 16.0, 4.0 / 16.0, -4.0 / 16.0, 4.0 / 16.0};
    private static final double[] SLOT_DZ = {-4.0 / 16.0, -4.0 / 16.0, 4.0 / 16.0, 4.0 / 16.0};

    // Surface top sits at y=9/16 (see CoffeeTableBlock.NORTH_SHAPE).
    private static final double SURFACE_Y = 9.0 / 16.0;
    // Held-item (non-block) rendering rests slightly above the surface — its FIXED-context pose
    // isn't anchored at the model's own base the way a real block's is, so it needs the margin.
    private static final double HELD_ITEM_Y = 10.5 / 16.0;

    private static final float HELD_ITEM_SCALE = 0.4f;
    // Natural (1x) scale reads as oversized once anchored correctly — a real block's full-size
    // footprint (up to 16/16 wide) dwarfs the table's own small surface, unlike a handheld tool's
    // FIXED-context sprite which was already sized for on-table display.
    private static final float BLOCK_PROP_SCALE = 0.5f;

    public CoffeeTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CoffeeTableBlockEntity table, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        for (int slot = 0; slot < CoffeeTableBlockEntity.SLOTS; slot++) {
            ItemStack item = table.getItem(slot);
            if (item.isEmpty()) continue;

            double x = 0.5 + SLOT_DX[slot];
            double z = 0.5 + SLOT_DZ[slot];

            if (item.getItem() instanceof BlockItem blockItem) {
                renderBlockProp(blockItem, x, z, poseStack, bufferSource, combinedLight, combinedOverlay);
            } else {
                renderHeldItem(item, x, z, table.isLying(slot), slot, poseStack, bufferSource, combinedLight, combinedOverlay, table);
            }
        }
    }

    // Real block props (mug, any vanilla/modded block item someone drops on the table) — rendered
    // via the actual block model instead of its item form. Tried ItemDisplayContext.FIXED first
    // (like held items below): it isn't anchored at the model's own base, so blocks sank partway
    // into the tabletop instead of resting on top of it. Same fix and reasoning as the candle
    // holders (see candle_holder_family memory): BlockRenderDispatcher.renderSingleBlock draws the
    // exact real-world model. Translate to the surface anchor *before* scaling (order matters once
    // scale != 1 — see the candle renderer's own note on this) so shrinking happens around that
    // point instead of the block's own corner, then shift by the model's own local base-center.
    private static void renderBlockProp(BlockItem blockItem, double x, double z, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        BlockState state = blockItem.getBlock().defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(x, SURFACE_Y, z);
        poseStack.scale(BLOCK_PROP_SCALE, BLOCK_PROP_SCALE, BLOCK_PROP_SCALE);
        poseStack.translate(-0.5, 0, -0.5);

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                state, poseStack, bufferSource, combinedLight, combinedOverlay, ModelData.EMPTY, null
        );

        poseStack.popPose();
    }

    private static void renderHeldItem(ItemStack item, double x, double z, boolean lying, int slot,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int combinedLight, int combinedOverlay, CoffeeTableBlockEntity table) {
        poseStack.pushPose();
        poseStack.translate(x, HELD_ITEM_Y, z);
        if (lying) {
            // Same rotation a floor-mounted vanilla item frame applies before drawing its
            // FIXED-pose item (see ItemFrameRenderer#render) — tips the item from
            // "standing, facing the viewer" to "lying flat, facing up".
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }
        poseStack.scale(HELD_ITEM_SCALE, HELD_ITEM_SCALE, HELD_ITEM_SCALE);

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
