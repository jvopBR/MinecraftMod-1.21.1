package net.umerlinn.mccourse.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.block.entity.CabinetBlockEntity;
import net.umerlinn.mccourse.block.entity.CandleHolderBlockEntity;
import net.umerlinn.mccourse.block.entity.CoffeeTableBlockEntity;
import net.umerlinn.mccourse.block.entity.ShelfBlockEntity;
import net.umerlinn.mccourse.block.entity.WardrobeBlockEntity;

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

    public static final Supplier<BlockEntityType<CoffeeTableBlockEntity>> COFFEE_TABLE =
            BLOCK_ENTITIES.register("coffee_table", () ->
                    BlockEntityType.Builder.of(
                            CoffeeTableBlockEntity::new,
                            ModFurnitureBlocks.COFFEE_TABLES.values().stream()
                                    .map(Supplier::get)
                                    .toArray(net.minecraft.world.level.block.Block[]::new)
                    ).build(null)
            );

    public static final Supplier<BlockEntityType<CabinetBlockEntity>> CABINET =
            BLOCK_ENTITIES.register("cabinet", () ->
                    BlockEntityType.Builder.of(
                            CabinetBlockEntity::new,
                            ModFurnitureBlocks.CABINETS.values().stream()
                                    .map(Supplier::get)
                                    .toArray(net.minecraft.world.level.block.Block[]::new)
                    ).build(null)
            );

    public static final Supplier<BlockEntityType<WardrobeBlockEntity>> WARDROBE =
            BLOCK_ENTITIES.register("wardrobe", () ->
                    BlockEntityType.Builder.of(
                            WardrobeBlockEntity::new,
                            ModFurnitureBlocks.WARDROBES.values().stream()
                                    .map(Supplier::get)
                                    .toArray(net.minecraft.world.level.block.Block[]::new)
                    ).build(null)
            );

    public static final Supplier<BlockEntityType<CandleHolderBlockEntity>> CANDLE_HOLDER =
            BLOCK_ENTITIES.register("candle_holder", () ->
                    BlockEntityType.Builder.of(
                            CandleHolderBlockEntity::new,
                            ModFurnitureBlocks.CANDLE_HOLDER.get(),
                            ModFurnitureBlocks.WALL_CANDLE_HOLDER.get(),
                            ModFurnitureBlocks.HANGING_CANDLE_HOLDER.get()
                    ).build(null)
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
