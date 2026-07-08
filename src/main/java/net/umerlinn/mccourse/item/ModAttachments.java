package net.umerlinn.mccourse.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.umerlinn.mccourse.MCCourseMod;
import net.umerlinn.mccourse.item.custom.RugColor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MCCourseMod.MOD_ID);

    // BlockPos.CODEC encodes as an int list, not a string, so Codec.unboundedMap(BlockPos.CODEC, ...)
    // can't use it as an NBT/network map key — round-trip it as an explicit list of entries instead,
    // the same pattern vanilla itself uses for position-keyed data.
    private record RugEntry(BlockPos pos, RugColor color) {
        static final Codec<RugEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(RugEntry::pos),
                RugColor.CODEC.fieldOf("color").forGetter(RugEntry::color)
        ).apply(instance, RugEntry::new));
    }

    private static final Codec<Map<BlockPos, RugColor>> RUG_MAP_CODEC = RugEntry.CODEC.listOf().xmap(
            entries -> entries.stream().collect(Collectors.toMap(RugEntry::pos, RugEntry::color, (a, b) -> b)),
            map -> map.entrySet().stream().map(e -> new RugEntry(e.getKey(), e.getValue())).collect(Collectors.toList()));

    // Level-wide: which world positions have a rug laid under whatever block occupies them.
    // Attached to the Level (not the target block's own BlockEntity) so this works under ANY
    // block — our own furniture, vanilla blocks, or another mod's — regardless of whether that
    // block has a BlockEntity of its own to hang data off of.
    public static final Supplier<AttachmentType<Map<BlockPos, RugColor>>> RUG_POSITIONS = ATTACHMENT_TYPES.register("rug_positions",
            () -> AttachmentType.<Map<BlockPos, RugColor>>builder(() -> new HashMap<>())
                    .serialize(RUG_MAP_CODEC)
                    .sync(ByteBufCodecs.fromCodecWithRegistries(RUG_MAP_CODEC))
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
