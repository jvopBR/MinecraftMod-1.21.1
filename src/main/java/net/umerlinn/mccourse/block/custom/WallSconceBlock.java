package net.umerlinn.mccourse.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.umerlinn.mccourse.block.entity.WallSconceBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Wall sconce whose hanging lantern physically swings when brushed. The lantern is NOT part of
 * the baked block model (that only has the wall plate + arm) — it's drawn every frame by
 * WallSconceBlockEntityRenderer, rotated around the hook pivot by the BlockEntity's damped
 * pendulum. Trigger flow mirrors vanilla's bell (BellBlockEntity.onHit): server detects the
 * touch in entityInside, broadcasts via level.blockEvent, every client receives it in
 * triggerEvent and stamps the swing against its own clock — no custom packet needed.
 *
 * The collision shape deliberately excludes the lantern (only plate + arm collide) so entities
 * pass through it — that's both what makes entityInside fire there and what makes "brushing
 * the lantern" physically possible at all.
 */
public class WallSconceBlock extends LightFurnitureBlock implements EntityBlock {

    public static final int EVENT_SWING = 1;

    /** Only the wall plate + arm block movement; the lantern is passable (see class doc). */
    private static final Map<Direction, VoxelShape> COLLISION_SHAPES = FurnitureShapes.rotateHorizontal(
            FurnitureShapes.boxes(
                    6, 2, 15, 10, 13, 16,
                    7.5, 11, 7, 8.5, 12, 15));

    // Facing-independent approximation of the lantern's world-space box (it hangs near the block
    // column's center for every facing), used to require an actual lantern touch instead of
    // triggering from anywhere in the block cell.
    private static final double LANTERN_MIN_XZ = 5.5 / 16.0;
    private static final double LANTERN_MAX_XZ = 10.5 / 16.0;
    private static final double LANTERN_MIN_Y = 3.4 / 16.0;
    private static final double LANTERN_MAX_Y = 11.0 / 16.0;
    private static final double TOUCH_MARGIN = 0.08;

    // Position-delta per tick, not getDeltaMovement: for server-side players deltaMovement is
    // stale/near-zero (their movement is client-authoritative), so a speed gate on it never
    // passes and the swing never triggers. Vanilla hits the same issue and solves it the same
    // way — SweetBerryBushBlock.entityInside compares getX()/getZ() against xOld/zOld with a
    // tiny threshold instead of reading deltaMovement. Walking ≈ 0.21/tick, sneaking ≈ 0.065.
    private static final double MIN_TRIGGER_SPEED = 0.01;

    public WallSconceBlock(Properties properties, VoxelShape northShape) {
        super(properties, northShape);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION_SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) {
            return;
        }
        Vec3 delta = new Vec3(entity.getX() - entity.xOld, 0, entity.getZ() - entity.zOld);
        double speed = Math.hypot(delta.x, delta.z);
        if (speed < MIN_TRIGGER_SPEED) {
            return;
        }
        AABB lantern = new AABB(
                pos.getX() + LANTERN_MIN_XZ, pos.getY() + LANTERN_MIN_Y, pos.getZ() + LANTERN_MIN_XZ,
                pos.getX() + LANTERN_MAX_XZ, pos.getY() + LANTERN_MAX_Y, pos.getZ() + LANTERN_MAX_XZ
        ).inflate(TOUCH_MARGIN);
        if (!entity.getBoundingBox().intersects(lantern)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof WallSconceBlockEntity sconce)
                || !sconce.tryAcceptTrigger(level.getGameTime())) {
            return;
        }
        float strength = (float) Mth.clamp(speed * 5.0, 0.35, 1.0);
        level.blockEvent(pos, this, EVENT_SWING, packSwing(state, delta, strength));
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide()
                && level.getBlockEntity(hit.getBlockPos()) instanceof WallSconceBlockEntity sconce
                && sconce.tryAcceptTrigger(level.getGameTime())) {
            level.blockEvent(hit.getBlockPos(), this, EVENT_SWING,
                    packSwing(state, projectile.getDeltaMovement(), 1.0f));
        }
    }

    /**
     * Packs swing direction + strength into the block event's single int param: sign(±1) times
     * strength scaled to 1..100. Direction = which way along the wall the toucher was moving
     * (their motion projected onto the wall-tangent axis).
     */
    private static int packSwing(BlockState state, Vec3 motion, float strength) {
        Direction tangent = state.getValue(FACING).getClockWise();
        double along = motion.x * tangent.getStepX() + motion.z * tangent.getStepZ();
        int sign = along >= 0 ? 1 : -1;
        return sign * Math.max(1, Math.round(strength * 100));
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id == EVENT_SWING) {
            if (level.getBlockEntity(pos) instanceof WallSconceBlockEntity sconce) {
                sconce.startSwing(param >= 0 ? 1 : -1, Math.abs(param) / 100.0f, level.getGameTime());
            }
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WallSconceBlockEntity(pos, state);
    }
}
