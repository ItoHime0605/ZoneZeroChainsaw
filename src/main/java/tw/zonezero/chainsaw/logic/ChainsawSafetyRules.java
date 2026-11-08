package tw.zonezero.chainsaw.logic;

/**
 * Pure safety rules used by the authoritative server and refuelling item.
 * Fuel/level/dye are intentionally never cleared on a runtime-state reset.
 */
public final class ChainsawSafetyRules {
    public static final int FUEL_PER_CAN = 600;
    public static final int ENGINE_TOGGLE_COOLDOWN = 4;

    private ChainsawSafetyRules() {}

    /** A full sealed fuel can is either consumed entirely or not at all. */
    public static boolean canRefuel(boolean canInOffhand, boolean sawInMainHand,
                                    boolean engineOn, int fuel, int capacity, int canCount) {
        return canInOffhand && sawInMainHand && !engineOn && canCount > 0
                && fuel >= 0 && capacity >= FUEL_PER_CAN
                && fuel <= capacity - FUEL_PER_CAN;
    }

    public static int refilledFuel(int fuel, int capacity) {
        if (capacity < FUEL_PER_CAN || fuel < 0 || fuel > capacity - FUEL_PER_CAN)
            return fuel;
        return fuel + FUEL_PER_CAN;
    }

    public static boolean canStart(boolean offhandEmpty, int fuel, boolean alive,
                                   boolean spectator) {
        return offhandEmpty && fuel > 0 && alive && !spectator;
    }

    /** Reject repeated toggle packets on the same/adjacent server ticks. */
    public static boolean toggleAllowed(long tick, long lastToggleTick) {
        return tick >= 0 && (lastToggleTick < 0
                || tick >= lastToggleTick && tick - lastToggleTick >= ENGINE_TOGGLE_COOLDOWN);
    }

    /** Any saw moved out of main hand must have its runtime state cleared. */
    public static boolean mustStop(boolean stillInMainHand, boolean alive,
                                   boolean spectator, boolean offhandEmpty) {
        return !stillInMainHand || !alive || spectator || !offhandEmpty;
    }
}
