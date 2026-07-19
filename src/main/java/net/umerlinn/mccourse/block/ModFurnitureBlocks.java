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
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.block.custom.AbstractCandleHolderBlock;
import net.umerlinn.mccourse.block.custom.CabinetBlock;
import net.umerlinn.mccourse.block.custom.CandleHolderBlock;
import net.umerlinn.mccourse.block.custom.CoffeeTableBlock;
import net.umerlinn.mccourse.block.custom.ConnectingBlock;
import net.umerlinn.mccourse.block.custom.FurnitureShapes;
import net.umerlinn.mccourse.block.custom.HangingCandleHolderBlock;
import net.umerlinn.mccourse.block.custom.HorizontalFurnitureBlock;
import net.umerlinn.mccourse.block.custom.LampPostBlock;
import net.umerlinn.mccourse.block.custom.LightFurnitureBlock;
import net.umerlinn.mccourse.block.custom.MultiblockPart;
import net.umerlinn.mccourse.block.custom.PillowBlock;
import net.umerlinn.mccourse.block.custom.WallCandleHolderBlock;
import net.umerlinn.mccourse.block.custom.WallSconceBlock;
import net.umerlinn.mccourse.block.custom.ShelfBlock;
import net.umerlinn.mccourse.block.custom.StorageBookcaseBlock;
import net.umerlinn.mccourse.block.custom.TableBlock;
import net.umerlinn.mccourse.block.custom.SeatBlock;
import net.umerlinn.mccourse.block.custom.WardrobeBlock;
import net.umerlinn.mccourse.item.custom.CandleHolderBlockItem;
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
    private static final VoxelShape PICTURE_FRAME_SHAPE = FurnitureShapes.boxes(
            0, 1, 15, 16, 15, 16,
            2, 3, 14.5, 14, 13, 15
    );
    private static final VoxelShape FLOOR_LAMP_SHAPE = FurnitureShapes.boxes(
            5, 0, 5, 11, 1, 11,
            7.5, 1, 7.5, 8.5, 12, 8.5,
            4.5, 11.6, 4.5, 11.5, 16, 11.5
    );
    private static final VoxelShape CEILING_LAMP_SHAPE = FurnitureShapes.boxes(
            6, 15, 6, 10, 16, 10,
            7.5, 13, 7.5, 8.5, 15, 8.5,
            4, 9.6, 4, 12, 13, 12
    );
    private static final VoxelShape TABLE_LAMP_SHAPE = FurnitureShapes.boxes(
            5.5, 0, 5.5, 10.5, 1, 10.5,
            7.5, 1, 7.5, 8.5, 5, 8.5,
            5, 4.6, 5, 11, 9, 11
    );
    private static final VoxelShape TRIPOD_LAMP_SHAPE = FurnitureShapes.boxes(
            4, 0, 4, 12, 10, 12,
            5, 10, 5, 11, 15.5, 11
    );
    private static final VoxelShape PENDANT_LAMP_SHAPE = FurnitureShapes.boxes(
            4.5, 6.6, 4.5, 11.5, 12, 11.5,
            7.6, 12, 7.6, 8.4, 16, 8.4
    );
    // Lamp post shapes live in LampPostBlock (one per vertical PART).
    private static final VoxelShape WALL_SCONCE_SHAPE = FurnitureShapes.boxes(
            6, 2, 15, 10, 13, 16,
            6, 4.4, 6, 10, 10, 10,
            7.5, 10, 7, 8.5, 12, 15
    );
    private static final VoxelShape PAPER_LANTERN_SHAPE = FurnitureShapes.boxes(
            4.5, 0, 4.5, 11.5, 10.8, 11.5
    );
    private static final VoxelShape LAVA_LAMP_SHAPE = FurnitureShapes.boxes(
            6, 0, 6, 10, 9.5, 10
    );
    private static final VoxelShape MUG_SHAPE = FurnitureShapes.boxes(
            5, 0, 5, 11, 7, 11
    );
    private static final VoxelShape LOLLIPOP_JAR_SHAPE = FurnitureShapes.boxes(
            4.5, 0, 4.5, 11.5, 7, 11.5,
            6, 7, 6, 10, 12, 10
    );

    // --- Sala de Estar ---
    public static final Map<String, DeferredBlock<SeatBlock>> CHAIRS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<SeatBlock>> ARMCHAIRS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<TableBlock>> TABLES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<CoffeeTableBlock>> COFFEE_TABLES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<ShelfBlock>> SHELVES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<HorizontalFurnitureBlock>> BOOKCASES = new LinkedHashMap<>();
    // Separate block/item/BlockEntity from BOOKCASES so the plain decorative bookcase keeps
    // working with no storage behind it — same split CabinetBlock/WardrobeBlock already use
    // relative to plain furniture.
    public static final Map<String, DeferredBlock<StorageBookcaseBlock>> STORAGE_BOOKCASES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<ConnectingBlock>> SOFAS = new LinkedHashMap<>();
    // Storage furniture — real container inventories (see MultiPartStorageBlock), spanning two
    // block positions placed as a single action, same pattern vanilla beds/doors use.
    public static final Map<String, DeferredBlock<CabinetBlock>> CABINETS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<WardrobeBlock>> WARDROBES = new LinkedHashMap<>();
    // A real placeable carpet (same 1/16-thick vanilla CarpetBlock shape) per sofa cushion color.
    // Also usable directly on a bed via RugItem, laid flush underneath instead of placed on top.
    public static final Map<RugColor, DeferredBlock<CarpetBlock>> RUGS = new EnumMap<>(RugColor.class);
    // Thick decorative cushion — 4px tall, rotatable, no collision. Detects bottom slabs below
    // and uses slab_offset=true so the model shifts down 8px to sit flush on the slab's top face.
    public static final Map<RugColor, DeferredBlock<PillowBlock>> PILLOWS = new EnumMap<>(RugColor.class);

    // --- Iluminação ---
    // 10 lamp styles sharing LightFurnitureBlock (FACING + rotated shape + constant light):
    // modern (floor/ceiling/table/tripod/pendant), medieval (chandelier/lamp post/wall sconce),
    // cozy-retro (paper lantern/lava lamp). Light level varies by style — main lamps 15,
    // accent lamps dimmer for ambiance.
    public static final DeferredBlock<LightFurnitureBlock> FLOOR_LAMP =
            registerBlock("floor_lamp", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(15), FLOOR_LAMP_SHAPE));
    public static final DeferredBlock<LightFurnitureBlock> CEILING_LAMP =
            registerBlock("ceiling_lamp", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(15), CEILING_LAMP_SHAPE));
    public static final DeferredBlock<LightFurnitureBlock> TABLE_LAMP =
            registerBlock("table_lamp", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(14), TABLE_LAMP_SHAPE));
    public static final DeferredBlock<LightFurnitureBlock> TRIPOD_LAMP =
            registerBlock("tripod_lamp", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(15), TRIPOD_LAMP_SHAPE));
    public static final DeferredBlock<LightFurnitureBlock> PENDANT_LAMP =
            registerBlock("pendant_lamp", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(15), PENDANT_LAMP_SHAPE));
    // 3 blocks tall (base/pole/lantern head placed as one action) — only the head emits light.
    public static final DeferredBlock<LampPostBlock> LAMP_POST =
            registerBlock("lamp_post", () -> new LampPostBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.5f)
                            .lightLevel(state -> state.getValue(LampPostBlock.PART) == MultiblockPart.THIRD ? 15 : 0)
                            .noOcclusion()
                            .isSuffocating((s, l, p) -> false)));
    // Swinging lantern: touch it and it sways (see WallSconceBlock). The lantern itself is drawn
    // by WallSconceBlockEntityRenderer from the hidden WALL_SCONCE_LANTERN block below.
    public static final DeferredBlock<WallSconceBlock> WALL_SCONCE =
            registerBlock("wall_sconce", () -> new WallSconceBlock(LightFurnitureBlock.lightProps(14), WALL_SCONCE_SHAPE));
    // Never placed/obtainable — exists only so the sconce renderer has a BlockState whose model
    // is just the hanging lantern (same hidden-technical-block pattern as WALL_CANDLE_HOLDER's
    // shared-item trio and the old book props). No BlockItem registered.
    public static final DeferredBlock<Block> WALL_SCONCE_LANTERN =
            BLOCKS.register("wall_sconce_lantern", () -> new Block(
                    BlockBehaviour.Properties.of().strength(0.5f).noCollission().noOcclusion()));
    public static final DeferredBlock<LightFurnitureBlock> PAPER_LANTERN =
            registerBlock("paper_lantern", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(12), PAPER_LANTERN_SHAPE));
    public static final DeferredBlock<LightFurnitureBlock> LAVA_LAMP =
            registerBlock("lava_lamp", () -> new LightFurnitureBlock(LightFurnitureBlock.lightProps(10), LAVA_LAMP_SHAPE));
    public static final DeferredBlock<HorizontalFurnitureBlock> PICTURE_FRAME =
            registerBlock("picture_frame", () -> new HorizontalFurnitureBlock(
                    BlockBehaviour.Properties.of().strength(0.5f).noOcclusion()
                            .isSuffocating((s, l, p) -> false),
                    PICTURE_FRAME_SHAPE));

    // --- Decoração / Props ---
    public static final DeferredBlock<HorizontalFurnitureBlock> MUG =
            registerBlock("mug", () -> new HorizontalFurnitureBlock(
                    BlockBehaviour.Properties.of().strength(0.5f).noOcclusion()
                            .isSuffocating((s, l, p) -> false).sound(SoundType.STONE),
                    MUG_SHAPE));
    public static final DeferredBlock<HorizontalFurnitureBlock> LOLLIPOP_JAR =
            registerBlock("lollipop_jar", () -> new HorizontalFurnitureBlock(
                    BlockBehaviour.Properties.of().strength(0.5f).noOcclusion()
                            .isSuffocating((s, l, p) -> false).sound(SoundType.STONE),
                    LOLLIPOP_JAR_SHAPE));
    // Real ignite/extinguish (flint and steel, flaming arrows, explosions) via AbstractCandleBlock
    // — see AbstractCandleHolderBlock's doc comment. Light scales with candle count once LIT,
    // matching vanilla CandleBlock.LIGHT_EMISSION's 3-per-candle formula exactly.
    private static final BlockBehaviour.Properties CANDLE_HOLDER_PROPS = BlockBehaviour.Properties.of()
            .strength(0.5f).noOcclusion()
            .isSuffocating((s, l, p) -> false).sound(SoundType.METAL)
            .lightLevel(state -> state.getValue(AbstractCandleHolderBlock.LIT) ? Math.min(15, state.getValue(AbstractCandleHolderBlock.CANDLES) * 3) : 0);

    // One item, three blocks (like vanilla torch/lantern) — registered as plain blocks (no
    // registerBlock() helper, which would give each its own separate BlockItem) since
    // CandleHolderBlockItem below picks whichever of the three to place based on the clicked face.
    public static final DeferredBlock<CandleHolderBlock> CANDLE_HOLDER =
            BLOCKS.register("candle_holder", () -> new CandleHolderBlock(CANDLE_HOLDER_PROPS));
    public static final DeferredBlock<WallCandleHolderBlock> WALL_CANDLE_HOLDER =
            BLOCKS.register("wall_candle_holder", () -> new WallCandleHolderBlock(CANDLE_HOLDER_PROPS));
    public static final DeferredBlock<HangingCandleHolderBlock> HANGING_CANDLE_HOLDER =
            BLOCKS.register("hanging_candle_holder", () -> new HangingCandleHolderBlock(CANDLE_HOLDER_PROPS));
    public static final DeferredItem<CandleHolderBlockItem> CANDLE_HOLDER_ITEM =
            ModItems.ITEMS.register("candle_holder", () -> new CandleHolderBlockItem(
                    CANDLE_HOLDER.get(), WALL_CANDLE_HOLDER.get(), HANGING_CANDLE_HOLDER.get(),
                    new Item.Properties()));

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
            COFFEE_TABLES.put(wood, registerBlock(wood + "_coffee_table", () -> new CoffeeTableBlock(openProps, CoffeeTableBlock.NORTH_SHAPE)));
            SHELVES.put(wood, registerBlock(wood + "_shelf", () -> new ShelfBlock(openProps, ShelfBlock.NORTH_SHAPE)));
            BOOKCASES.put(wood, registerBlock(wood + "_bookcase", () -> new HorizontalFurnitureBlock(cubeProps, Shapes.block())));
            STORAGE_BOOKCASES.put(wood, registerBlock(wood + "_storage_bookcase", () -> new StorageBookcaseBlock(cubeProps)));
            SOFAS.put(wood, registerBlock(wood + "_sofa", () -> new ConnectingBlock(openProps)));
            CABINETS.put(wood, registerBlock(wood + "_cabinet", () -> new CabinetBlock(openProps)));
            WARDROBES.put(wood, registerBlock(wood + "_wardrobe", () -> new WardrobeBlock(openProps, wood)));
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
