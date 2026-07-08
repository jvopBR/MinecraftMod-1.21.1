package net.umerlinn.mccourse.item.custom;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum RugColor implements StringRepresentable {
    CREAM, ESPRESSO, MUSTARD, SAGE, TERRACOTTA, CHARCOAL, BURGUNDY, ROSE, SLATE;

    public static final Codec<RugColor> CODEC = StringRepresentable.fromEnum(RugColor::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
