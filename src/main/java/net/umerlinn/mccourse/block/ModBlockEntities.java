package net.umerlinn.mccourse.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.block.entity.ShelfBlockEntity;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MCCourseMod.MOD_ID);

    public static final Supplier<BlockEntityType<ShelfBlockEntity>> SHELF =
            BLOCK_ENTITIES.register("shelf", () ->
                    BlockEntityType.Builder.of(
                            ShelfBlockEntity::new,
                            ModFurnitureBlocks.SHELVES.values().stream()
                                    .map(Supplier::get)
                                    .toArray(net.minecraft.world.level.block.Block[]::new)
                    ).build(null)
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
