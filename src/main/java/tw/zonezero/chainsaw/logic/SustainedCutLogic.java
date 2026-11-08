package tw.zonezero.chainsaw.logic;

/**
 * Pure logic shared by client and authoritative server.  No Minecraft classes.
 * Continuous cutting is a held state; the 4-tick interval is a damage/fuel
 * cadence, NOT a new animation or swing every four ticks.
 */
public final class SustainedCutLogic {
    public static final int DAMAGE_INTERVAL = 4;
    public static final int INPUT_TIMEOUT = 7;
    public static final float POSE_STEP = 0.25f;

    private SustainedCutLogic() {}

    public static boolean canCut(boolean engineOn, boolean offhandEmpty, int fuel) {
        return engineOn && offhandEmpty && fuel > 0;
    }

    public static boolean heldRecently(long worldTick, long lastHeldTick) {
        return lastHeldTick >= 0 && worldTick >= lastHeldTick
                && worldTick - lastHeldTick <= INPUT_TIMEOUT;
    }

    public static boolean shouldCut(long worldTick, long lastHeldTick,
                                    boolean engineOn, boolean offhandEmpty, int fuel) {
        return canCut(engineOn, offhandEmpty, fuel) && heldRecently(worldTick, lastHeldTick);
    }

    public static boolean consumeUnit(int continuousTicks) {
        return continuousTicks > 0 && continuousTicks % DAMAGE_INTERVAL == 0;
    }

    public static float approach(float value, float target) {
        if (Float.isNaN(value) || Float.isNaN(target)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f,
                value + Math.max(-POSE_STEP, Math.min(POSE_STEP, target - value))));
    }

    public static float interpolate(float prev, float next, float partialTick) {
        float t = Math.max(0.0f, Math.min(1.0f, partialTick));
        return prev + (next - prev) * t;
    }
}
