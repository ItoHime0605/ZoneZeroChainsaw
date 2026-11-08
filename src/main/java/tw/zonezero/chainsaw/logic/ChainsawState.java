package tw.zonezero.chainsaw.logic;

/** Server-authoritative tick state. No client-supplied damage or fuel values. */
public final class ChainsawState {
    public static final int CAPACITY = 1800, REFILL = 600, CUT_INTERVAL = 4, STOP_TICKS = 40;
    private int fuel, cutTicks, coastTicks;
    private boolean running;
    public ChainsawState(int storedFuel) { fuel = Math.max(0, Math.min(CAPACITY, storedFuel)); }
    public int fuel() { return fuel; }
    public boolean running() { return running; }
    public int coastTicks() { return coastTicks; }
    public boolean start() {
        if (running || fuel == 0 || coastTicks > 0) return false;
        running = true;
        return true;
    }
    public void stop() {
        if (running) { running = false; coastTicks = STOP_TICKS; cutTicks = 0; }
    }
    public int refill() {
        int n = Math.min(REFILL, CAPACITY - fuel);
        fuel += n;
        return n;
    }
    /** Called once per server tick. True means consume one cut unit and attempt contact. */
    public boolean tick(boolean attackHeld) {
        if (coastTicks > 0) coastTicks--;
        if (!running || !attackHeld) { cutTicks = 0; return false; }
        if (fuel <= 0) { stop(); return false; }
        if (++cutTicks < CUT_INTERVAL) return false;
        cutTicks = 0;
        fuel--;
        if (fuel == 0) stop();
        return true;
    }
    /** Normalized visual speed only; never authorizes hits. */
    public double visualSpeed() {
        return running ? 1.0 : Math.pow(coastTicks / (double) STOP_TICKS, 2);
    }
}
