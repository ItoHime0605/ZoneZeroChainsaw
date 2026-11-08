package tw.zonezero.chainsaw.logic;

/** Processing timing and quantity rules; inventories must be implemented by block entities. */
public final class ProcessingRules {
    private ProcessingRules() {}
    public static final int FERMENT_TICKS = 600;
    public static final int REFINE_TICKS = 200;
    public static final int CRUDE_PER_BOTTLE = 6;
    public static boolean fermentationReady(int progress, boolean hasInput, boolean outputAccepts) {
        return hasInput && outputAccepts && progress >= FERMENT_TICKS;
    }
    public static boolean refiningReady(int progress, int crudeCount, boolean outputAccepts) {
        return crudeCount >= CRUDE_PER_BOTTLE && outputAccepts && progress >= REFINE_TICKS;
    }
}
