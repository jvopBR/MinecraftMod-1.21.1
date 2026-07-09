package net.umerlinn.mccourse.block.custom;

import net.minecraft.util.StringRepresentable;

/**
 * Role of a block position within a multi-part structure (see MultiPartStorageBlock). PRIMARY
 * is the clicked position and owns the real BlockEntity; SECOND/THIRD/FOURTH are placed
 * automatically at offsets the subclass defines, in that order.
 */
public enum MultiblockPart implements StringRepresentable {
    PRIMARY, SECOND, THIRD, FOURTH;

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
