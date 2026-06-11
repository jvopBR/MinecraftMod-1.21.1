package net.umerlinn.mccourse.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.item.custom.ChainsawItem;
import org.lwjgl.opengl.INTELMapTexture;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MCCourseMod.MOD_ID);

    public static final DeferredItem<Item> BLACK_OPAL = ITEMS.registerSimpleItem("black_opal");
    public static final DeferredItem<Item> RAW_BLACK_OPAL =
            ITEMS.registerItem("raw_black_opal", Item::new, new Item.Properties());

    public static final DeferredItem<Item> CHAINSAW =
            ITEMS.registerItem("chainsaw", ChainsawItem:: new, new Item.Properties().durability(32));





    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
