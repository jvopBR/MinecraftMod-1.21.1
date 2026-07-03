package net.umerlinn.mccourse.block.custom;

import net.minecraft.util.StringRepresentable;

public enum SofaShape implements StringRepresentable {
    SINGLE, END_LEFT, END_RIGHT, MIDDLE, CORNER_LEFT, CORNER_RIGHT;

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
