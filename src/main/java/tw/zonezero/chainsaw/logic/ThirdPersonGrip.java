package tw.zonezero.chainsaw.logic;

/**
 * Third-person saw grip geometry and mirrored player-arm rotations.
 *
 * The authored model has a rear rubber handle around (17.96, 8.0, 8.4)
 * and a forward transverse U guard at (7.33, 13.1, 10.2).
 * ItemDisplayContext.THIRD_PERSON_RIGHT_HAND rotates -90 degrees around Y.
 * Mapping the two model-space anchors into the same hand-space transform
 * keeps the right hand near the rear grip and the support hand forward/up.
 * All dimensions are 16ths of a Minecraft block.
 */
public final class ThirdPersonGrip {
    public static final double SCALE = 0.59;
    public static final double ITEM_X = 0.18;
    public static final double ITEM_Y = 0.78;
    public static final double ITEM_Z = -4.60;

    public static final double REAR_X = 17.96, REAR_Y = 8.00, REAR_Z = 8.40;
    public static final double FRONT_X = 7.33, FRONT_Y = 13.10, FRONT_Z = 10.20;

    public record Point(double x, double y, double z) {}
    public record Rotation(float x, float y, float z) {}

    private ThirdPersonGrip() {}

    /** Right-main-hand model transform, after rotation about the item center. */
    public static Point transformRight(double x, double y, double z) {
        return new Point(
                ITEM_X - (z - 8.0) * SCALE,
                ITEM_Y + (y - 8.0) * SCALE,
                ITEM_Z + (x - 8.0) * SCALE);
    }

    public static Point rearGrip() {
        return transformRight(REAR_X, REAR_Y, REAR_Z);
    }

    public static Point frontGuard() {
        return transformRight(FRONT_X, FRONT_Y, FRONT_Z);
    }

    /**
     * Both hands keep their distinct main/rear and support/front roles while
     * left-handed players mirror the pose. Legs, body, and head are untouched.
     * Movement applies only tiny synchronized sway, not weapon swing cycles.
     */
    public static Rotation arm(boolean leftMainArm, boolean rightArm, float extension,
                               boolean crouching, double horizontalSpeedSquared,
                               float worldTime) {
        float t = Math.max(0f, Math.min(1f, extension));
        boolean main = leftMainArm != rightArm;
        float mirror = rightArm ? 1f : -1f;

        // Main arm holds the rear trigger handle; the supporting arm reaches
        // across toward the front-facing U, 6.3px forward and 3px higher.
        float readyPitch = main ? -1.05f : -1.29f;
        float cuttingPitch = main ? -1.43f : -1.56f;
        float readyYaw = (main ? -0.18f : -0.51f) * mirror;
        float cuttingYaw = (main ? -0.13f : -0.42f) * mirror;
        float readyRoll = (main ? 0.07f : -0.09f) * mirror;
        float cuttingRoll = (main ? 0.045f : -0.075f) * mirror;

        // Stabilized shared walking movement prevents hand separation;
        // this is intentionally subtle (< 0.035 radians).
        float walking = (float) Math.min(1.0, Math.max(0.0, horizontalSpeedSquared * 25.0));
        float bob = (float) Math.sin(worldTime * 0.54f) * walking * 0.028f;
        float crouch = crouching ? -0.09f : 0f;
        float x = readyPitch + (cuttingPitch - readyPitch) * t + bob + crouch;
        float y = readyYaw + (cuttingYaw - readyYaw) * t;
        float z = readyRoll + (cuttingRoll - readyRoll) * t;
        return new Rotation(x, y, z);
    }
}
