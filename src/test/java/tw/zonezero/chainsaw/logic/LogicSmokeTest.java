package tw.zonezero.chainsaw.logic;

/** Plain Java assertions; execute with java -ea. */
public final class LogicSmokeTest {
    static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) {
        ChainsawState saw = new ChainsawState(1800);
        check(saw.start());
        for (int i=0;i<3;i++) check(!saw.tick(true));
        check(saw.tick(true)); check(saw.fuel()==1799);
        for (int i=0;i<20;i++) check(!saw.tick(false));
        check(saw.fuel()==1799);
        saw.stop(); check(saw.coastTicks()==40 && !saw.tick(true));
        double previous=saw.visualSpeed();
        for (int i=0;i<40;i++) {
            saw.tick(false);
            check(saw.visualSpeed()<=previous);
            previous=saw.visualSpeed();
        }
        check(saw.visualSpeed()==0);
        check(saw.refill()==1 && saw.fuel()==1800);
        ChainsawState empty = new ChainsawState(1);
        check(empty.start());
        for (int i=0;i<4;i++) empty.tick(true);
        check(empty.fuel()==0 && !empty.running());
        check(!empty.start());
        check(ProcessingRules.fermentationReady(600,true,true));
        check(!ProcessingRules.fermentationReady(599,true,true));
        check(ProcessingRules.refiningReady(200,6,true));
        check(!ProcessingRules.refiningReady(200,5,true));
        System.out.println("All chainsaw logic smoke tests passed");
    }
}
