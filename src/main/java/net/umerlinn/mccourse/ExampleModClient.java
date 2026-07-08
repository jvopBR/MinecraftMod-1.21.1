package net.umerlinn.mccourse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.umerlinn.mccourse.entity.ModEntities;
import net.umerlinn.mccourse.entity.SeatEntity;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = MCCourseMod.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = MCCourseMod.MOD_ID, value = Dist.CLIENT)
public class ExampleModClient {
    public ExampleModClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        MCCourseMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        MCCourseMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEAT.get(), ctx -> new EntityRenderer<SeatEntity>(ctx) {
            @Override
            public ResourceLocation getTextureLocation(SeatEntity entity) {
                return ResourceLocation.fromNamespaceAndPath(MCCourseMod.MOD_ID, "");
            }
        });

        // Rug rendering (RugRenderer) doesn't hook into any specific block's renderer — it scans
        // tracked positions directly via RenderLevelStageEvent, so no registration is needed here.
    }
}
