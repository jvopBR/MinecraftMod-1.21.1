package net.umerlinn.mccourse.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * One item, three blocks — generalizes vanilla's own {@code StandingAndWallBlockItem} (which
 * only ever picks between 2 targets: standing or wall) to 3: floor, wall, hanging. Which one gets
 * placed depends on {@link net.minecraft.world.item.context.BlockPlaceContext#getNearestLookingDirections()}:
 * confirmed via decompiled {@code BlockPlaceContext.java} that this list always has the clicked
 * face's *opposite* direction moved to the front (e.g. clicking the TOP of a block puts DOWN
 * first) — so mapping DOWN to the floor block, UP to the hanging block, and everything else to
 * the wall block reproduces exactly the "click top = stand it up, click underside = hang it,
 * click a side = mount it on the wall" behaviour the reference video showed, matching how vanilla
 * torches/lanterns choose their own placement.
 */
public class CandleHolderBlockItem extends BlockItem {

    private final Block wallBlock;
    private final Block hangingBlock;

    public CandleHolderBlockItem(Block floorBlock, Block wallBlock, Block hangingBlock, Item.Properties properties) {
        super(floorBlock, properties);
        this.wallBlock = wallBlock;
        this.hangingBlock = hangingBlock;
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState result = null;

        for (Direction direction : context.getNearestLookingDirections()) {
            BlockState candidate = switch (direction) {
                case DOWN -> this.getBlock().getStateForPlacement(context);
                case UP -> hangingBlock.getStateForPlacement(context);
                default -> wallBlock.getStateForPlacement(context);
            };
            if (candidate != null && candidate.canSurvive(level, pos)) {
                result = candidate;
                break;
            }
        }

        return result != null && level.isUnobstructed(result, pos, CollisionContext.empty()) ? result : null;
    }

    @Override
    public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
        super.registerBlocks(blockToItemMap, item);
        blockToItemMap.put(wallBlock, item);
        blockToItemMap.put(hangingBlock, item);
    }
}
