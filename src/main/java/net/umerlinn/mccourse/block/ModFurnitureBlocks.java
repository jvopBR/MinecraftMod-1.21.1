package net.umerlinn.mccourse.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.block.custom.ConnectingBlock;
import net.umerlinn.mccourse.block.custom.FurnitureShapes;
import net.umerlinn.mccourse.block.custom.HorizontalFurnitureBlock;
import net.umerlinn.mccourse.block.custom.LightFurnitureBlock;
import net.umerlinn.mccourse.block.custom.PillowBlock;
import net.umerlinn.mccourse.block.custom.TableBlock;
import net.umerlinn.mccourse.block.custom.SeatBlock;
import net.umerlinn.mccourse.item.custom.PillowItem;
import net.umerlinn.mccourse.item.ModItems;
import net.umerlinn.mccourse.item.custom.RugColor;
import net.umerlinn.mccourse.item.custom.RugItem;

import java.util.EnumMap;
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

    // Hitboxes hug the model geometry instead of the default full-block cube. Each is the
    // NORTH-facing (unrotated) shape; HorizontalFurnitureBlock/SeatBlock rotate it per FACING.
    // Seat cushions are left out of chair/armchair shapes on purpose so the seat area stays
    // walkable — only legs, arms and backrest block movement.
    private static final VoxelShape CHAIR_SHAPE = FurnitureShapes.boxes(
            2, 0, 2, 4, 7, 4,
            12, 0, 2, 14, 7, 4,
            2, 0, 12, 4, 7, 14,
            12, 0, 12, 14, 7, 14,
            2, 9, 12, 14, 15, 14
    );
    private static final VoxelShape ARMCHAIR_SHAPE = FurnitureShapes.boxes(
            2, 0, 2, 4, 7, 4,
            12, 0, 2, 14, 7, 4,
            2, 0, 12, 4, 7, 14,
            12, 0, 12, 14, 7, 14,
            2, 9, 12, 14, 15, 14,
            2, 9, 2, 4, 13, 14,
            12, 9, 2, 14, 13, 14
    );
    private static final VoxelShape TABLE_SHAPE = FurnitureShapes.boxes(
            1, 0, 1, 3, 13, 3,
            13, 0, 1, 15, 13, 3,
            1, 0, 13, 3, 13, 15,
            13, 0, 13, 15, 13, 15,
            0, 13, 0, 16, 15, 16
    );
    private static final VoxelShape COFFEE_TABLE_SHAPE = FurnitureShapes.boxes(
            2, 0, 2, 4, 7, 4,
            12, 0, 2, 14, 7, 4,
            2, 0, 12, 4, 7, 14,
            12, 0, 12, 14, 7, 14,
            1, 7, 1, 15, 9, 15
    );
    private static final VoxelShape SHELF_SHAPE = FurnitureShapes.boxes(
            0, 8, 14, 16, 16, 16,
            0, 8, 2, 16, 10, 14,
            0, 4, 2, 2, 8, 14,
            14, 4, 2, 16, 8, 14
    );
    private static final VoxelShape PICTURE_FRAME_SHAPE = FurnitureShapes.boxes(
            0, 1, 15, 16, 15, 16,
            2, 3, 14.5, 14, 13, 15
    );
    private static final VoxelShape FLOOR_LAMP_SHAPE = FurnitureShapes.boxes(
            4, 0, 4, 12, 2, 12,
            7, 2, 7, 9, 13, 9,
            4, 13, 4, 12, 16, 12
    );
    private static final VoxelShape CEILING_LAMP_SHAPE = FurnitureShapes.boxes(
            7, 12, 7, 9, 16, 9,
            5, 6, 5, 11, 12, 11,
            3, 4, 3, 13, 7, 13
    );

    // --- Sala de Estar ---
    public static final Map<String, DeferredBlock<SeatBlock>> CHAIRS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<SeatBlock>> ARMCHAIRS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<TableBlock>> TABLES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<HorizontalFurnitureBlock>> COFFEE_TABLES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<HorizontalFurnitureBlock>> SHELVES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<HorizontalFurnitureBlock>> BOOKCASES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<ConnectingBlock>> SOFAS = new LinkedHashMap<>();
    // A real placeable carpet (same 1/16-thick vanilla CarpetBlock shape) per sofa cushion color.
    // Also usable directly on a bed via RugItem, laid flush underneath instead of placed on top.
    public static final Map<RugColor, DeferredBlock<CarpetBlock>> RUGS = new EnumMap<>(RugColor.class);
    // Thick decorative cushion — 4px tall, rotatable, no collision. Detects bottom slabs below
    // and uses slab_offset=true so the model shifts down 8px to sit flush on the slab's top face.
    public static final Map<RugColor, DeferredBlock<PillowBlock>> PILLOWS = new EnumMap<>(RugColor.class);

    // --- Iluminação ---
    public static final DeferredBlock<LightFurnitureBlock> FLOOR_LAMP =
            registerBlock("floor_lamp", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(15), FLOOR_LAMP_SHAPE));
    public static final DeferredBlock<LightFurnitureBlock> CEILING_LAMP =
            registerBlock("ceiling_lamp", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(15), CEILING_LAMP_SHAPE));
    public static final DeferredBlock<HorizontalFurnitureBlock> PICTURE_FRAME =
            registerBlock("picture_frame", () -> new HorizontalFurnitureBlock(
                    BlockBehaviour.Properties.of().strength(0.5f).noOcclusion()
                            .isSuffocating((s, l, p) -> false),
                    PICTURE_FRAME_SHAPE));

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
            CHAIRS.put(wood, registerBlock(wood + "_chair", () -> new SeatBlock(openProps, CHAIR_SHAPE)));
            ARMCHAIRS.put(wood, registerBlock(wood + "_armchair", () -> new SeatBlock(openProps, ARMCHAIR_SHAPE)));
            TABLES.put(wood, registerBlock(wood + "_table", () -> new TableBlock(openProps)));
            COFFEE_TABLES.put(wood, registerBlock(wood + "_coffee_table", () -> new HorizontalFurnitureBlock(openProps, COFFEE_TABLE_SHAPE)));
            SHELVES.put(wood, registerBlock(wood + "_shelf", () -> new HorizontalFurnitureBlock(openProps, SHELF_SHAPE)));
            BOOKCASES.put(wood, registerBlock(wood + "_bookcase", () -> new HorizontalFurnitureBlock(cubeProps, Shapes.block())));
            SOFAS.put(wood, registerBlock(wood + "_sofa", () -> new ConnectingBlock(openProps)));
        }

        // Same strength/sound as vanilla wool carpet.
        BlockBehaviour.Properties rugProps = BlockBehaviour.Properties.of()
                .strength(0.1f).sound(SoundType.WOOL);

        for (RugColor color : RugColor.values()) {
            String name = color.getSerializedName() + "_rug";
            DeferredBlock<CarpetBlock> block = BLOCKS.register(name, () -> new CarpetBlock(rugProps));
            RUGS.put(color, block);
            ModItems.ITEMS.register(name, () -> new RugItem(color, block.get(), new Item.Properties()));
        }

        BlockBehaviour.Properties pillowProps = BlockBehaviour.Properties.of()
                .strength(0.5f).noCollission().noOcclusion()
                .isSuffocating((s, l, p) -> false)
                .sound(SoundType.WOOL);
        VoxelShape pillowShape = FurnitureShapes.boxes(0, 0, 0, 16, 4, 16);
        for (RugColor color : RugColor.values()) {
            String name = color.getSerializedName() + "_pillow";
            // Register block and PillowItem separately — PillowItem adds sneak+right-click
            // under-block placement on top of the standard BlockItem place behaviour.
            DeferredBlock<PillowBlock> pillowBlock = BLOCKS.register(name,
                    () -> new PillowBlock(pillowProps, pillowShape));
            PILLOWS.put(color, pillowBlock);
            final RugColor c = color;
            ModItems.ITEMS.register(name, () -> new PillowItem(c, pillowBlock.get(), new Item.Properties()));
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
