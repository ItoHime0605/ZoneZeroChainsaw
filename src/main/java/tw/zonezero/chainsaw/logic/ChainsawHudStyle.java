package tw.zonezero.chainsaw.logic;

/** Fixed layout palette for the no-panel, transparent chainsaw HUD. */
public final class ChainsawHudStyle {
    public static final int BAR_WIDTH = 112;
    public static final int HEIGHT = 40;
    public static final int TOTAL_WIDTH = 188;
    public static final float TEXT_SCALE = 1.15f;

    /** Do not change font color with engine state, fuel or upgrade tier. */
    public static final int TEXT_COLOR = 0xfff0f4f2;
    public static final int TEXT_OUTLINE = 0xff52616b;

    private ChainsawHudStyle() {}
}
