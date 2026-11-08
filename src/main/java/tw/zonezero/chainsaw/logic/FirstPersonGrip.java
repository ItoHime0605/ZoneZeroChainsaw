package tw.zonezero.chainsaw.logic;

/**
 * First-person positioning, separated from Minecraft rendering so the same
 * offsets can be unit-tested. They are in the PoseStack's camera-space units,
 * NOT 16-pixel model coordinates. Never modify player skin or renderer layers.
 */
public final class FirstPersonGrip {
    public record Offset(double x, double y, double z) {}

    private FirstPersonGrip() {}

    public static Offset mainHand(boolean emptyOffhand, boolean running,
                                  boolean leftMainArm, float cutting) {
        if (!emptyOffhand) {
            // A loaded offhand cannot support the chainsaw. Keep it lowered.
            return new Offset(0.0, -0.28, 0.13);
        }
        double side = leftMainArm ? -1.0 : 1.0;
        float t = Math.max(0f, Math.min(1f, cutting));
        double x = -(0.05 + (running ? 0.07 : 0.0) + 0.035 * t) * side;
        double y = -0.025 - (running ? 0.04 : 0.0) + 0.095 * t;
        double z = -0.09 - (running ? 0.14 : 0.0) - 0.34 * t;
        return new Offset(x, y, z);
    }

    public static Offset supportHand(boolean leftMainArm, float cutting) {
        double side = leftMainArm ? -1.0 : 1.0;
        float t = Math.max(0f, Math.min(1f, cutting));
        return new Offset((0.28 + 0.025 * t) * side,
                0.13 + 0.05 * t,
                -0.42 - 0.28 * t);
    }
}
