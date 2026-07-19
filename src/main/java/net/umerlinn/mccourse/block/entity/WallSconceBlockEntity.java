package net.umerlinn.mccourse.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.umerlinn.mccourse.block.ModBlockEntities;

/**
 * Holds the sconce lantern's transient swing state. Nothing here is saved to NBT on purpose —
 * a swing is a moments-long animation, not world data; after a chunk reload the lantern simply
 * hangs still until touched again. The server never reads the animation fields either: it only
 * gates re-triggers via {@link #tryAcceptTrigger}; clients receive the swing through the block
 * event (see WallSconceBlock) and each stamp it against their own game-time clock.
 */
public class WallSconceBlockEntity extends BlockEntity {

    /** Minimum ticks between accepted touch triggers, so entityInside (fired every tick while
     * overlapping) re-pumps the swing at a natural rate instead of restarting it 20x/second. */
    private static final int TRIGGER_COOLDOWN_TICKS = 10;
    private static final float MAX_SWING_RADIANS = (float) Math.toRadians(30);
    /** Amplitude decays to 1/e after this many ticks (~1s); renderer treats the swing as over at 100. */
    private static final float DECAY_TICKS = 20.0f;
    /** Angular frequency in radians/tick — ~1.75 full swings per second. */
    private static final float FREQUENCY = 0.55f;

    private long swingStartTime = Long.MIN_VALUE;
    private int swingSign = 1;
    private float swingStrength;

    private long lastAcceptedTrigger = Long.MIN_VALUE;

    public WallSconceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WALL_SCONCE.get(), pos, state);
    }

    /** Server-side re-trigger gate. */
    public boolean tryAcceptTrigger(long gameTime) {
        if (gameTime - lastAcceptedTrigger < TRIGGER_COOLDOWN_TICKS) {
            return false;
        }
        lastAcceptedTrigger = gameTime;
        return true;
    }

    /** Client side (via block event): restart the pendulum from its highest point. */
    public void startSwing(int sign, float strength, long gameTime) {
        this.swingStartTime = gameTime;
        this.swingSign = sign >= 0 ? 1 : -1;
        this.swingStrength = Mth.clamp(strength, 0.0f, 1.0f);
    }

    /** Damped pendulum: A·e^(−t/τ)·cos(ωt). Zero when idle or fully decayed. */
    public float swingAngleRadians(long gameTime, float partialTick) {
        if (swingStartTime == Long.MIN_VALUE) {
            return 0.0f;
        }
        float t = (gameTime - swingStartTime) + partialTick;
        if (t < 0 || t > 100) {
            return 0.0f;
        }
        return swingSign * swingStrength * MAX_SWING_RADIANS
                * (float) (Math.exp(-t / DECAY_TICKS) * Math.cos(t * FREQUENCY));
    }
}
