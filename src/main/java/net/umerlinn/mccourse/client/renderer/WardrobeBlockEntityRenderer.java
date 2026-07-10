package net.umerlinn.mccourse.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.block.custom.WardrobeBlock;
import net.umerlinn.mccourse.block.entity.WardrobeBlockEntity;
import net.umerlinn.mccourse.client.model.WardrobeModel;

/**
 * Renders the wardrobe's PRIMARY position only (see WardrobeBlock.getRenderShape — the other 3
 * positions are RenderShape.INVISIBLE) — draws all 4 quadrants of the 2x2 assembly from here,
 * translating for the second column instead of needing a block entity/renderer call per position.
 *
 * Split into 5 draw calls (one per texture: wood/leg/gold/metal/interior) since ModelPart
 * rendering binds one texture per call, unlike the JSON block models this replaced which could
 * reference a different "texture variable" per face within a single model. The interior group has
 * no drawer parts — only the doors have an inside face worth texturing differently.
 */
public class WardrobeBlockEntityRenderer implements BlockEntityRenderer<WardrobeBlockEntity> {

    // Doors open to roughly 100 degrees — a bit past perpendicular, reads clearly as "open"
    // without the panel visually vanishing edge-on to the camera at 90.
    private static final float MAX_OPEN_RADIANS = (float) Math.toRadians(100.0);

    private static final ResourceLocation LEG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "textures/block/furniture/furniture_leg.png");
    private static final ResourceLocation GOLD_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "textures/block/furniture/sofa_cushion_mustard.png");
    private static final ResourceLocation METAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "textures/block/furniture/furniture_metal.png");
    // Door interior lining — dark, per the user's request (a real wardrobe's inside is usually
    // darker than its show face). Reuses the same dark texture as the plinth/cap/reveal-line trim.
    private static final ResourceLocation INTERIOR_TEXTURE = LEG_TEXTURE;
    // Multiplicative tint (see ModelPart#render's 5-arg overload -> Cube#compile -> addVertex's
    // "color" param), applied ONLY to the interior lining draw calls to push it toward "quase
    // preto... a mesma escuridao do bau" (near-black, chest-cavity dark) on top of the already-
    // dark LEG_TEXTURE. Scoped to the interior specifically — an earlier version applied this to
    // the wood body/drawer/door exterior too, which was wrong: the user wanted only the part
    // revealed behind an opened door to go dark, not the whole exterior's per-wood-type coloring.
    // 0xFF explicit alpha (fully opaque) is required — the default (no tint) render() overload
    // passes color=-1 (0xFFFFFFFF), and entityCutout doesn't alpha-blend, so a lower alpha here
    // would not do what "darker" implies.
    private static final int INTERIOR_TINT = 0xFF2E2E2E;

    private final ModelPart woodBody, woodDrawer, woodDoorLeft, woodDoorRight;
    private final ModelPart legPlinth, legCap, legDoorLeft, legDoorRight;
    private final ModelPart goldDrawer, goldDoorLeft, goldDoorRight;
    private final ModelPart metalDrawer, metalDoorLeft, metalDoorRight;
    private final ModelPart interiorDoorLeft, interiorDoorRight, interiorCavity;

    public WardrobeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart wood = context.bakeLayer(WardrobeModel.WOOD);
        this.woodBody = wood.getChild("body");
        this.woodDrawer = wood.getChild("drawer");
        this.woodDoorLeft = wood.getChild("doorLeft");
        this.woodDoorRight = wood.getChild("doorRight");

        ModelPart leg = context.bakeLayer(WardrobeModel.LEG);
        this.legPlinth = leg.getChild("plinth");
        this.legCap = leg.getChild("cap");
        this.legDoorLeft = leg.getChild("doorLeft");
        this.legDoorRight = leg.getChild("doorRight");

        ModelPart gold = context.bakeLayer(WardrobeModel.GOLD);
        this.goldDrawer = gold.getChild("drawer");
        this.goldDoorLeft = gold.getChild("doorLeft");
        this.goldDoorRight = gold.getChild("doorRight");

        ModelPart metal = context.bakeLayer(WardrobeModel.METAL);
        this.metalDrawer = metal.getChild("drawerHandle");
        this.metalDoorLeft = metal.getChild("doorLeft");
        this.metalDoorRight = metal.getChild("doorRight");

        ModelPart interior = context.bakeLayer(WardrobeModel.INTERIOR);
        this.interiorDoorLeft = interior.getChild("doorLeft");
        this.interiorDoorRight = interior.getChild("doorRight");
        this.interiorCavity = interior.getChild("cavity");
    }

    @Override
    public void render(WardrobeBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(WardrobeBlock.FACING);

        float openness = blockEntity.getOpenNess(partialTick);
        float eased = 1.0F - openness;
        eased = 1.0F - eased * eased * eased;
        float angle = eased * MAX_OPEN_RADIANS;

        // Doors hinge on the true outer edges of the 2-wide assembly, so they swing apart from
        // each other rather than mirroring the same direction. (Signs were swapped from the
        // first attempt — re-derived via the actual rotation matrix instead of guessing again:
        // doorLeft's panel extends in +local-X from its hinge at x=1, doorRight's extends in
        // -local-X from its hinge at x=15; for the far edge of each to sweep toward -Z ("out
        // front", where FACING=north's door face is authored) as angle increases from 0, doorLeft
        // needs +angle and doorRight needs -angle — the opposite of what was there before.)
        woodDoorLeft.yRot = angle;
        legDoorLeft.yRot = angle;
        goldDoorLeft.yRot = angle;
        metalDoorLeft.yRot = angle;
        interiorDoorLeft.yRot = angle;
        woodDoorRight.yRot = -angle;
        legDoorRight.yRot = -angle;
        goldDoorRight.yRot = -angle;
        metalDoorRight.yRot = -angle;
        interiorDoorRight.yRot = -angle;

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(blockstateYRot(facing)));
        poseStack.translate(-0.5, 0, -0.5);
        // No manual pixel->block scale here: ModelPart already divides by 16 internally, both for
        // the PartPose pivot offset (ModelPart#translateAndRotate does x/16.0F) and for each
        // Cube's own vertices (Cube#compile does pos.x()/16.0F) — confirmed by reading
        // ModelPart.java directly after a manual scale() here made the whole model collapse to a
        // few stray pixels near the pivot (a double 1/16 scale, 1/256 total). Box/pivot
        // coordinates authored in 0-16 "pixel" units are handled as-is; poseStack stays in normal
        // 1-unit-per-block space the whole time.

        // MultiBufferSource.BufferSource auto-ends a shared-buffer RenderType's batch as soon as
        // a DIFFERENT RenderType's buffer is requested (see getBuffer's `lastSharedType` check) —
        // grabbing all 4 texture buffers up front and interleaving draws across them closes each
        // one before it's actually drawn into, crashing with "Not building!" the moment the next
        // ModelPart tries to add vertices. Fix: finish every draw call for one texture (both
        // columns) before requesting the next texture's buffer at all.
        VertexConsumer woodBuf = bufferSource.getBuffer(RenderType.entityCutout(woodTexture(state)));
        woodBody.render(poseStack, woodBuf, packedLight, packedOverlay);
        woodDrawer.render(poseStack, woodBuf, packedLight, packedOverlay);
        woodDoorLeft.render(poseStack, woodBuf, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(1, 0, 0);
        woodBody.render(poseStack, woodBuf, packedLight, packedOverlay);
        woodDrawer.render(poseStack, woodBuf, packedLight, packedOverlay);
        woodDoorRight.render(poseStack, woodBuf, packedLight, packedOverlay);
        poseStack.popPose();

        VertexConsumer legBuf = bufferSource.getBuffer(RenderType.entityCutout(LEG_TEXTURE));
        legPlinth.render(poseStack, legBuf, packedLight, packedOverlay);
        legCap.render(poseStack, legBuf, packedLight, packedOverlay);
        legDoorLeft.render(poseStack, legBuf, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(1, 0, 0);
        legPlinth.render(poseStack, legBuf, packedLight, packedOverlay);
        legCap.render(poseStack, legBuf, packedLight, packedOverlay);
        legDoorRight.render(poseStack, legBuf, packedLight, packedOverlay);
        poseStack.popPose();

        VertexConsumer goldBuf = bufferSource.getBuffer(RenderType.entityCutout(GOLD_TEXTURE));
        goldDrawer.render(poseStack, goldBuf, packedLight, packedOverlay);
        goldDoorLeft.render(poseStack, goldBuf, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(1, 0, 0);
        goldDrawer.render(poseStack, goldBuf, packedLight, packedOverlay);
        goldDoorRight.render(poseStack, goldBuf, packedLight, packedOverlay);
        poseStack.popPose();

        VertexConsumer metalBuf = bufferSource.getBuffer(RenderType.entityCutout(METAL_TEXTURE));
        metalDrawer.render(poseStack, metalBuf, packedLight, packedOverlay);
        metalDoorLeft.render(poseStack, metalBuf, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(1, 0, 0);
        metalDrawer.render(poseStack, metalBuf, packedLight, packedOverlay);
        metalDoorRight.render(poseStack, metalBuf, packedLight, packedOverlay);
        poseStack.popPose();

        // interiorCavity is NOT parented to a door pivot and gets no yRot — it's the stationary
        // wardrobe-body surface the closed doors hide, not part of the swinging door itself (see
        // createInteriorLayer's comment for why both are needed for the "open door reveals a dark
        // cavity" effect).
        VertexConsumer interiorBuf = bufferSource.getBuffer(RenderType.entityCutout(INTERIOR_TEXTURE));
        interiorDoorLeft.render(poseStack, interiorBuf, packedLight, packedOverlay, INTERIOR_TINT);
        interiorCavity.render(poseStack, interiorBuf, packedLight, packedOverlay, INTERIOR_TINT);
        poseStack.pushPose();
        poseStack.translate(1, 0, 0);
        interiorDoorRight.render(poseStack, interiorBuf, packedLight, packedOverlay, INTERIOR_TINT);
        interiorCavity.render(poseStack, interiorBuf, packedLight, packedOverlay, INTERIOR_TINT);
        poseStack.popPose();

        poseStack.popPose();
    }

    // Direction.toYRot() (NORTH=180, SOUTH=0, WEST=90, EAST=270) doesn't match this mod's JSON
    // blockstate "y" rotation convention (NORTH=0, EAST=90, SOUTH=180, WEST=270), so this method
    // computes the blockstate-style value explicitly instead of reusing toYRot(). That first fix
    // used the JSON convention's numbers directly (EAST=90, WEST=270) and north/south confirmed
    // correct, but east/west still came out backward. Reason: north(0)/south(180) both have
    // sin(theta)=0, so they render identically regardless of which rotation-direction convention
    // Axis.YP.rotationDegrees() vs. the JSON model loader each use internally — they can't
    // reveal a sign mismatch. East/west (sin(theta)=+-1) can, and did: Axis.YP.rotationDegrees()
    // turns out to rotate the opposite handedness from the JSON loader for those two. Fix is to
    // swap EAST/WEST's values (confirmed against the user's actual in-game report, not re-derived
    // from a rotation matrix a third time) while leaving north/south alone.
    private static float blockstateYRot(Direction facing) {
        return switch (facing) {
            case NORTH -> 0F;
            case EAST -> 270F;
            case SOUTH -> 180F;
            case WEST -> 90F;
            default -> 0F;
        };
    }

    private static ResourceLocation woodTexture(BlockState state) {
        String wood = ((WardrobeBlock) state.getBlock()).getWoodName();
        return ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "textures/block/furniture/wood_" + wood + ".png");
    }

    @Override
    public AABB getRenderBoundingBox(WardrobeBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return AABB.encapsulatingFullBlocks(pos, pos.offset(1, 1, 1));
    }
}
