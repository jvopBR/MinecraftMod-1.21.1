package net.umerlinn.mccourse.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.umerlinn.mccourse.MCCourseMod;

/**
 * Java-defined 3D model for the wardrobe's swinging doors, rendered by WardrobeBlockEntityRenderer
 * instead of the baked JSON block model (which the wardrobe still has, but only for break-particle
 * texture purposes now that PART=PRIMARY renders RenderShape.ENTITYBLOCK_ANIMATED).
 *
 * Coordinates use the same 16-units-per-block, Y-up space as the JSON models did — the "door"
 * spans both the bottom and top block positions continuously (y 7 to 29 out of 0-32) instead of
 * being split at the block boundary, since a hand-built model isn't constrained to fit inside one
 * block the way a baked JSON model is.
 *
 * Split into 5 separate part trees, one per texture (wood/leg/gold/metal/interior), because
 * ModelPart rendering binds one texture per draw call — unlike JSON block models, which let each
 * face reference its own "texture variable" within a single model. Each tree has its own doorLeft
 * (hinge at local x=1, PRIMARY's column) and doorRight (hinge at local x=15, SECOND's column)
 * parts so the two doors swing outward from the true outer edges rather than mirroring each
 * other's hinge side.
 */
public class WardrobeModel {

    public static final ModelLayerLocation WOOD =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "wardrobe"), "wood");
    public static final ModelLayerLocation LEG =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "wardrobe"), "leg");
    public static final ModelLayerLocation GOLD =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "wardrobe"), "gold");
    public static final ModelLayerLocation METAL =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "wardrobe"), "metal");
    public static final ModelLayerLocation INTERIOR =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "wardrobe"), "interior");

    public static final PartPose DOOR_LEFT_PIVOT = PartPose.offset(1.0F, 0.0F, 0.0F);
    public static final PartPose DOOR_RIGHT_PIVOT = PartPose.offset(15.0F, 0.0F, 0.0F);

    public static LayerDefinition createWoodLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Front face pulled back to z=0.08 (instead of 0) so it doesn't z-fight with the
        // stationary dark "cavity" panel in the interior layer, which occupies that sliver right
        // where the closed doors used to fully hide it.
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0, 1, 0.08F, 16, 29, 15.92F), PartPose.ZERO);
        root.addOrReplaceChild("drawer", CubeListBuilder.create().texOffs(0, 0)
                .addBox(1.7F, 2.5F, -0.3F, 12.6F, 3.0F, 0.05F), PartPose.ZERO);
        root.addOrReplaceChild("doorLeft", CubeListBuilder.create().texOffs(0, 0)
                .addBox(1.2F, 7.7F, -0.33F, 11.6F, 20.6F, 0.05F), DOOR_LEFT_PIVOT);
        root.addOrReplaceChild("doorRight", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-12.8F, 7.7F, -0.33F, 11.6F, 20.6F, 0.05F), DOOR_RIGHT_PIVOT);
        return LayerDefinition.create(mesh, 16, 16);
    }

    public static LayerDefinition createLegLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("plinth", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0, 0, 0, 16, 1, 16), PartPose.ZERO);
        root.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0, 30, 0, 16, 2, 16), PartPose.ZERO);
        root.addOrReplaceChild("doorLeft", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0.5F, 7.3F, -0.28F, 13.0F, 21.4F, 0.03F), DOOR_LEFT_PIVOT);
        root.addOrReplaceChild("doorRight", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-13.5F, 7.3F, -0.28F, 13.0F, 21.4F, 0.03F), DOOR_RIGHT_PIVOT);
        return LayerDefinition.create(mesh, 16, 16);
    }

    public static LayerDefinition createGoldLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("drawer", CubeListBuilder.create().texOffs(0, 0)
                .addBox(1.0F, 2.0F, -0.25F, 14.0F, 4.0F, 0.25F), PartPose.ZERO);
        root.addOrReplaceChild("doorLeft", CubeListBuilder.create().texOffs(0, 0)
                .addBox(0, 7.0F, -0.25F, 14.0F, 22.0F, 0.25F), DOOR_LEFT_PIVOT);
        root.addOrReplaceChild("doorRight", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-14.0F, 7.0F, -0.25F, 14.0F, 22.0F, 0.25F), DOOR_RIGHT_PIVOT);
        return LayerDefinition.create(mesh, 16, 16);
    }

    // Two things live here, both dark ("quase preto" per the user), both distinct from the wood
    // exterior:
    // - doorLeft/doorRight: the inside FACE OF THE DOOR ITSELF — sits just behind the leg accent
    //   layer (z -0.25 to -0.28, see createLegLayer), parented to the door pivots so it swings
    //   RIGIDLY WITH the door and ends up wherever the open door ends up.
    // - cavity: the WARDROBE'S OWN body front, right where the closed doors used to hide it
    //   completely (x/y matches doorLeft's own footprint exactly; z 0 to 0.08, in front of the
    //   body's now-recessed front face — see createWoodLayer). PartPose.ZERO — this one does
    //   NOT rotate with the door. This is the piece that actually reads as "opening the door
    //   reveals a dark cavity, like a chest": doorLeft/doorRight alone swing away with the door
    //   and stop covering the opening, so on their own they don't darken what's newly visible
    //   through it — something stationary has to sit behind the doors for that.
    public static LayerDefinition createInteriorLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("doorLeft", CubeListBuilder.create().texOffs(0, 0)
                .addBox(1.2F, 7.7F, -0.25F, 11.6F, 20.6F, 0.1F), DOOR_LEFT_PIVOT);
        root.addOrReplaceChild("doorRight", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-12.8F, 7.7F, -0.25F, 11.6F, 20.6F, 0.1F), DOOR_RIGHT_PIVOT);
        root.addOrReplaceChild("cavity", CubeListBuilder.create().texOffs(0, 0)
                .addBox(1.2F, 7.7F, 0F, 11.6F, 20.6F, 0.08F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }

    public static LayerDefinition createMetalLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("drawerHandle", CubeListBuilder.create().texOffs(0, 0)
                .addBox(6.5F, 3.5F, -0.6F, 3.0F, 1.0F, 0.3F), PartPose.ZERO);
        root.addOrReplaceChild("doorLeft", CubeListBuilder.create().texOffs(0, 0)
                .addBox(5.5F, 12.5F, -0.65F, 3.0F, 1.0F, 0.32F), DOOR_LEFT_PIVOT);
        root.addOrReplaceChild("doorRight", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8.5F, 12.5F, -0.65F, 3.0F, 1.0F, 0.32F), DOOR_RIGHT_PIVOT);
        return LayerDefinition.create(mesh, 16, 16);
    }
}
