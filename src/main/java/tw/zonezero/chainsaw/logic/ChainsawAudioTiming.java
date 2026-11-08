package tw.zonezero.chainsaw.logic;

/**
 * Deterministic 20-TPS audio scheduling. Every server player is its own
 * sound source; engine/chain clips are scheduled independently from damage.
 * A packet announcing "attack held" MUST NOT reset the sound period.
 */
public final class ChainsawAudioTiming {
    public static final int START_DELAY = 18; // 0.90 seconds
    public static final int ENGINE_INTERVAL = 12; // 0.60 seconds
    public static final int CUT_INTERVAL = 8; // 0.40 seconds
    public static final int CONTACT_COOLDOWN = 6; // 0.30 seconds
    public static final long NONE = Long.MIN_VALUE;

    private ChainsawAudioTiming() {}

    public static boolean engineClip(long tick, long startTick) {
        if (tick < startTick || tick - startTick < START_DELAY) return false;
        return (tick - startTick - START_DELAY) % ENGINE_INTERVAL == 0;
    }

    public static boolean clipDue(long tick, long previousTick, int spacing) {
        return spacing > 0 && tick >= 0 &&
                (previousTick == NONE || tick >= previousTick &&
                 tick - previousTick >= spacing);
    }
}
