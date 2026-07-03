package net.umerlinn.mccourse.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.block.custom.ConnectingBlock;
import net.umerlinn.mccourse.block.custom.HorizontalFurnitureBlock;
import net.umerlinn.mccourse.block.custom.LightFurnitureBlock;
import net.umerlinn.mccourse.block.custom.SeatBlock;
import net.umerlinn.mccourse.item.ModItems;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModFurnitureBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MCCourseMod.MOD_ID);

    public static final List<String> WOOD_TYPES = List.of(
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo"
    );

    // --- Sala de Estar ---
    public static final Map<String, DeferredBlock<SeatBlock>> CHAIRS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<SeatBlock>> ARMCHAIRS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<HorizontalFurnitureBlock>> TABLES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<HorizontalFurnitureBlock>> COFFEE_TABLES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<HorizontalFurnitureBlock>> SHELVES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<HorizontalFurnitureBlock>> BOOKCASES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<ConnectingBlock>> SOFAS = new LinkedHashMap<>();

    // --- Iluminação ---
    public static final DeferredBlock<LightFurnitureBlock> FLOOR_LAMP =
            registerBlock("floor_lamp", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(15)));
    public static final DeferredBlock<LightFurnitureBlock> CEILING_LAMP =
            registerBlock("ceiling_lamp", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(15)));
    public static final DeferredBlock<HorizontalFurnitureBlock> PICTURE_FRAME =
            registerBlock("picture_frame", () -> new HorizontalFurnitureBlock(
                    BlockBehaviour.Properties.of().strength(0.5f).noOcclusion()
                            .isSuffocating((s, l, p) -> false)));

    static {
        // Shared props for non-full-cube furniture (chairs, tables, sofas, shelves).
        // noOcclusion() is required so gaps between legs don't cull neighbor faces.
        // isSuffocating=false prevents head-suffocation damage inside the model.
        BlockBehaviour.Properties openProps = BlockBehaviour.Properties.of()
                .strength(1.5f).noOcclusion()
                .isSuffocating((s, l, p) -> false)
                .sound(SoundType.WOOD);

        // Bookcases are full cubes — noOcclusion() would force neighbor faces to render
        // unnecessarily and waste client fill-rate. Omit it for full-cube blocks.
        BlockBehaviour.Properties cubeProps = BlockBehaviour.Properties.of()
                .strength(1.5f)
                .sound(SoundType.WOOD);

        for (String wood : WOOD_TYPES) {
            CHAIRS.put(wood, registerBlock(wood + "_chair", () -> new SeatBlock(openProps)));
            ARMCHAIRS.put(wood, registerBlock(wood + "_armchair", () -> new SeatBlock(openProps)));
            TABLES.put(wood, registerBlock(wood + "_table", () -> new HorizontalFurnitureBlock(openProps)));
            COFFEE_TABLES.put(wood, registerBlock(wood + "_coffee_table", () -> new HorizontalFurnitureBlock(openProps)));
            SHELVES.put(wood, registerBlock(wood + "_shelf", () -> new HorizontalFurnitureBlock(openProps)));
            BOOKCASES.put(wood, registerBlock(wood + "_bookcase", () -> new HorizontalFurnitureBlock(cubeProps)));
            SOFAS.put(wood, registerBlock(wood + "_sofa", () -> new ConnectingBlock(openProps)));
        }
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
