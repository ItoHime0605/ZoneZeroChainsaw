package tw.zonezero.chainsaw.logic;

/**
 * Independent upgrade transaction validation; no client or inventory APIs.
 * Every one of the three input slots must be accounted for, with no extras.
 * The returned plan is an all-or-nothing recipe, not a list of partial costs.
 */
public final class UpgradePlanLogic {
    public static final int EMPTY = 0;
    public static final int MATERIAL = 1;
    public static final int FUEL = 2;
    public static final int DYE = 3;
    public static final int INVALID = 4;
    private static final int[] COST = {3, 6, 27, 3, 3, 3};

    public record Plan(boolean valid, boolean dye, int targetTier, int dyeColor) {
        public static Plan invalid() { return new Plan(false,false,-1,-1); }
    }

    private UpgradePlanLogic() {}

    public static int requiredCount(int targetTier) {
        return targetTier >= 0 && targetTier < COST.length ? COST[targetTier] : 0;
    }

    public static int targetTier(boolean initialized, int previousTier) {
        if (previousTier < 0 || previousTier > 5) return -1;
        int tier = initialized ? previousTier + 1 : 0;
        return tier <= 5 ? tier : -1;
    }

    public static Plan match(int targetTier, int originalDye,
                             int[] kind, int[] quantity, int[] colors) {
        if (kind == null || quantity == null || colors == null ||
                kind.length != 3 || quantity.length != 3 || colors.length != 3)
            return Plan.invalid();
        int materials = 0, cans = 0, dyes = 0, selected = -1;
        for (int i=0;i<3;i++) {
            if (quantity[i] < 0) return Plan.invalid();
            if (kind[i] == EMPTY) {
                if (quantity[i] != 0) return Plan.invalid();
            } else if (quantity[i] == MATERIAL) {
                if (quantity[i] <= 0) return Plan.invalid();
                materials += quantity[i];
            } else if (kind[i] == FUEL) {
                if (quantity[i] <= 0) return Plan.invalid();
                cans += quantity[i];
            } else if (kind[i] == DYE) {
                if (quantity[i] != 1 || colors[i] < 0 || colors[i] > 15)
                    return Plan.invalid();
                dyes++;
                selected=colors[i];
            } else {
                return Plan.invalid();
            }
        }
        if (dyes == 1 && cans == 0 && materials == 0 && selected != originalDye)
            return new Plan(true,true,-1,selected);
        if (dyes == 0 && targetTier >= 0 && targetTier <= 5
                && cans == 1 && materials == requiredCount(targetTier))
            return new Plan(true,false,targetTier,-1);
        return Plan.invalid();
    }
}
