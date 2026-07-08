package net.umerlinn.mccourse.block.custom;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LightFurnitureBlock extends HorizontalFurnitureBlock {

    public LightFurnitureBlock(Properties properties, VoxelShape northShape) {
        super(properties, northShape);
    }

    public static BlockBehaviour.Properties lightProps(int lightLevel) {
        return BlockBehaviour.Properties.of()
                .strength(0.5f)
                .lightLevel(state -> lightLevel)
                .noOcclusion()
                .isSuffocating((state, level, pos) -> false);
    }
}
