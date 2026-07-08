package net.umerlinn.mccourse.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.block.ModBlocks;
import net.umerlinn.mccourse.block.ModFurnitureBlocks;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MCCourseMod.MOD_ID);

    public static final Supplier<CreativeModeTab> BLACK_OPAL_ITEMS_TAB =
            CREATIVE_MODE_TABS.register("black_opal_items_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mccourse.black_opal_items_tab"))
                    .icon(() -> new ItemStack(ModItems.BLACK_OPAL.get()))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.BLACK_OPAL);
                        pOutput.accept(ModItems.RAW_BLACK_OPAL);

                        pOutput.accept(ModItems.CHAINSAW);

                    }).build());
    public static final Supplier<CreativeModeTab> FURNITURE_TAB =
            CREATIVE_MODE_TABS.register("furniture_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mccourse.furniture_tab"))
                    .icon(() -> new ItemStack(ModFurnitureBlocks.CHAIRS.get("oak").get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "black_opal_items_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        for (String wood : ModFurnitureBlocks.WOOD_TYPES) {
                            pOutput.accept(ModFurnitureBlocks.CHAIRS.get(wood));
                            pOutput.accept(ModFurnitureBlocks.ARMCHAIRS.get(wood));
                            pOutput.accept(ModFurnitureBlocks.SOFAS.get(wood));
                            pOutput.accept(ModFurnitureBlocks.TABLES.get(wood));
                            pOutput.accept(ModFurnitureBlocks.COFFEE_TABLES.get(wood));
                            pOutput.accept(ModFurnitureBlocks.SHELVES.get(wood));
                            pOutput.accept(ModFurnitureBlocks.BOOKCASES.get(wood));
                        }
                        pOutput.accept(ModFurnitureBlocks.FLOOR_LAMP);
                        pOutput.accept(ModFurnitureBlocks.CEILING_LAMP);
                        pOutput.accept(ModFurnitureBlocks.PICTURE_FRAME);

                        for (var rug : ModFurnitureBlocks.RUGS.values()) {
                            pOutput.accept(rug);
                        }
                        for (var pillow : ModFurnitureBlocks.PILLOWS.values()) {
                            pOutput.accept(pillow);
                        }
                    }).build());

    public static final Supplier<CreativeModeTab> BLACK_OPAL_BLOCKS_TAB =
            CREATIVE_MODE_TABS.register("black_opal_blocks_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mccourse.black_opal_blocks_tab"))
                    .icon(() -> new ItemStack(ModItems.RAW_BLACK_OPAL.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "black_opal_items_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModBlocks.BLACK_OPAL_BLOCK);
                        pOutput.accept(ModBlocks.RAW_BLACK_OPAL_BLOCK);
                        pOutput.accept(ModBlocks.BLACK_OPAL_ORE);
                        pOutput.accept(ModBlocks.BLACK_OPAL_DEEPSLATE_ORE);
                        pOutput.accept(ModBlocks.BLACK_OPAL_END_ORE);
                        pOutput.accept(ModBlocks.BLACK_OPAL_NETHER_ORE);

                    }).build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

}
