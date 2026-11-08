package tw.zonezero.chainsaw.logic;

/**
 * Pure 40-tick chainsaw coast controller. World game time and server-synced
 * coastUntil are authoritative, so every client selects the same stage.
 * No per-tick NBT writes or model regeneration.
 */
public final class ChainMotion {
    public static final int COAST_TICKS = 40;
    public static final float IDLE = 0.0f;
    public static final float COAST_SLOW = 0.25f;
    public static final float COAST_MEDIUM = 0.5f;
    public static final float COAST_FAST = 0.75f;
    public static final float RUNNING = 1.0f;

    private ChainMotion() {}

    public static float engineModel(boolean engineOn, long coastUntil, long currentTick) {
        if (engineOn) return RUNNING;
        if (coastUntil <= 0 || currentTick < 0) return IDLE;
        long remaining = coastUntil - currentTick;
        if (remaining <= 0 || remaining > COAST_TICKS) return IDLE;
        if (remaining > 28) return COAST_FAST;
        if (remaining > 14) return COAST_MEDIUM;
        return COAST_SLOW;
    }
}
