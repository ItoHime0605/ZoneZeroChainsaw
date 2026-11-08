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
        // A held cut is one animation state; only the server-side damage
        // and fuel pulses happen every four ticks.
        check(SustainedCutLogic.canCut(true,true,600));
        check(!SustainedCutLogic.canCut(true,false,600));
        check(!SustainedCutLogic.canCut(false,true,600));
        check(!SustainedCutLogic.canCut(true,true,0));
        check(SustainedCutLogic.shouldCut(100, 96, true, true, 600));
        check(!SustainedCutLogic.shouldCut(105, 96, true, true, 600));
        check(!SustainedCutLogic.shouldCut(99, 100, true, true, 600));
        check(!SustainedCutLogic.shouldCut(100, -1000, true, true, 600));
        int fuelPulses = 0;
        for (int tick = 1; tick <= 20; tick++)
            if (SustainedCutLogic.consumeUnit(tick)) fuelPulses++;
        check(fuelPulses == 5);
        float holdPose = 0;
        for (int tick = 0; tick < 4; tick++) {
            float next = SustainedCutLogic.approach(holdPose, 1f);
            check(next >= holdPose);
            holdPose = next;
        }
        check(holdPose == 1f);
        check(SustainedCutLogic.interpolate(0f,1f,.5f) == .5f);
        for (int tick = 0; tick < 4; tick++) holdPose = SustainedCutLogic.approach(holdPose, 0f);
        check(holdPose == 0f);
        // First-person positioning is shared across idle, running and
        // cutting. Forward motion is monotone and both arms follow together.
        FirstPersonGrip.Offset ready = FirstPersonGrip.mainHand(true,true,false,0);
        FirstPersonGrip.Offset forward = FirstPersonGrip.mainHand(true,true,false,1);
        FirstPersonGrip.Offset other = FirstPersonGrip.mainHand(true,true,true,1);
        check(forward.z() < ready.z());
        check(forward.y() > ready.y());
        check(forward.x() == -other.x() && forward.y() == other.y());
        check(forward.z() == other.z());
        FirstPersonGrip.Offset support = FirstPersonGrip.supportHand(false,0);
        FirstPersonGrip.Offset supportForward = FirstPersonGrip.supportHand(false,1);
        check(supportForward.z() < support.z());
        check(supportForward.y() > support.y());
        check(supportForward.x() == -FirstPersonGrip.supportHand(true,1).x());
        FirstPersonGrip.Offset blocked = FirstPersonGrip.mainHand(false,false,false,1);
        check(blocked.y() < ready.y() && blocked.z() > ready.z());
        // Both hand anchors are derived from the real authored model:
        // the front U must be farther forward/up than the rear trigger grip.
        ThirdPersonGrip.Point rear = ThirdPersonGrip.rearGrip();
        ThirdPersonGrip.Point front = ThirdPersonGrip.frontGuard();
        check(Math.abs(rear.x()) < 1.0);
        check(rear.z() > 0.5 && rear.z() < 2.0);
        check(front.z() < rear.z() - 5.0);
        check(front.y() > rear.y() + 2.5);
        // Main and supporting arms have separate grips; both aim ahead
        // while holding, and both extend further during continuous cutting.
        ThirdPersonGrip.Rotation mainReady =
                ThirdPersonGrip.arm(false,true,0,false,0,0);
        ThirdPersonGrip.Rotation supportReady =
                ThirdPersonGrip.arm(false,false,0,false,0,0);
        ThirdPersonGrip.Rotation mainCut =
                ThirdPersonGrip.arm(false,true,1,false,0,0);
        ThirdPersonGrip.Rotation supportCut =
                ThirdPersonGrip.arm(false,false,1,false,0,0);
        check(mainReady.x() < -0.9f && supportReady.x() < mainReady.x());
        check(mainCut.x() < mainReady.x() && supportCut.x() < supportReady.x());
        check(Math.abs(mainCut.x() - supportCut.x()) > .05f);
        // Left-main-hand posture mirrors right-main-hand posture.
        ThirdPersonGrip.Rotation mirroredMain =
                ThirdPersonGrip.arm(true,false,1,false,0,0);
        ThirdPersonGrip.Rotation mirroredSupport =
                ThirdPersonGrip.arm(true,true,1,false,0,0);
        check(mainCut.x() == mirroredMain.x());
        check(mainCut.y() == -mirroredMain.y());
        check(mainCut.z() == -mirroredMain.z());
        check(supportCut.x() == mirroredSupport.x());
        check(supportCut.y() == -mirroredSupport.y());
        // Player locomotion applies tiny synchronized sway, not repeating
        // a large up/down chopping motion.
        float mainWalk = ThirdPersonGrip.arm(false,true,1,false,.05,13).x();
        float supportWalk = ThirdPersonGrip.arm(false,false,1,false,.05,13).x();
        check(Math.abs((mainWalk - mainCut.x()) - (supportWalk - supportCut.x())) < .0001);
        check(Math.abs(mainWalk - mainCut.x()) < .04f);
        check(ThirdPersonGrip.arm(false,true,1,true,0,0).x() < mainCut.x());
        // 40 tick coast: fast (12), medium (14), slow (14), then static.
        check(ChainMotion.engineModel(true, 0, 100) == ChainMotion.RUNNING);
        check(ChainMotion.engineModel(false, 140, 100) == ChainMotion.COAST_FAST);
        check(ChainMotion.engineModel(false, 140, 111) == ChainMotion.COAST_FAST);
        check(ChainMotion.engineModel(false, 140, 112) == ChainMotion.COAST_MEDIUM);
        check(ChainMotion.engineModel(false, 140, 125) == ChainMotion.COAST_MEDIUM);
        check(ChainMotion.engineModel(false, 140, 126) == ChainMotion.COAST_SLOW);
        check(ChainMotion.engineModel(false, 140, 139) == ChainMotion.COAST_SLOW);
        check(ChainMotion.engineModel(false, 140, 140) == ChainMotion.IDLE);
        check(ChainMotion.engineModel(false, 140, 141) == ChainMotion.IDLE);
        check(ChainMotion.engineModel(false, 140, 99) == ChainMotion.IDLE);
        check(ChainMotion.engineModel(false, 0, 100) == ChainMotion.IDLE);
        float lastSpeed = ChainMotion.RUNNING;
        for (int step = 0; step <= 40; step++) {
            float speed = ChainMotion.engineModel(false, 140, 100 + step);
            check(speed <= lastSpeed && speed >= ChainMotion.IDLE);
            lastSpeed = speed;
        }
        check(lastSpeed == ChainMotion.IDLE);
        check(ChainMotion.COAST_TICKS == 40);
        // Spatial sound loops must align with server time; 4-tick damage
        // NEVER resets an independent 12-tick engine or 8-tick chain clip.
        check(!ChainsawAudioTiming.engineClip(117,100));
        check(ChainsawAudioTiming.engineClip(118,100));
        check(!ChainsawAudioTiming.engineClip(119,100));
        check(ChainsawAudioTiming.engineClip(130,100));
        check(ChainsawAudioTiming.engineClip(142,100));
        check(!ChainsawAudioTiming.engineClip(130,131));
        check(ChainsawAudioTiming.clipDue(101,ChainsawAudioTiming.NONE,8));
        check(!ChainsawAudioTiming.clipDue(107,101,8));
        check(ChainsawAudioTiming.clipDue(109,101,8));
        check(ChainsawAudioTiming.clipDue(117,109,8));
        check(!ChainsawAudioTiming.clipDue(118,119,8));
        check(!ChainsawAudioTiming.clipDue(-1,ChainsawAudioTiming.NONE,8));
        check(ChainsawAudioTiming.clipDue(140,134,6));
        check(!ChainsawAudioTiming.clipDue(139,134,6));
        check(ChainsawAudioTiming.START_DELAY == 18);
        check(ChainsawAudioTiming.ENGINE_INTERVAL == 12);
        check(ChainsawAudioTiming.CUT_INTERVAL == 8);
        check(ChainsawAudioTiming.CONTACT_COOLDOWN == 6);
        // The HUD uses one fixed ivory-white text color, independent from
        // the engine. Only the two original guide-bar sprites change.
        check(ChainsawHudStyle.TEXT_COLOR == 0xfff0f4f2);
        check(ChainsawHudStyle.TEXT_OUTLINE == 0xff52616b);
        check(ChainsawHudStyle.BAR_WIDTH == 112);
        check(ChainsawHudStyle.HEIGHT == 40);
        check(ChainsawHudStyle.TOTAL_WIDTH >= ChainsawHudStyle.BAR_WIDTH + 65);
        check(ProcessingRules.fermentationReady(600,true,true));
        check(!ProcessingRules.fermentationReady(599,true,true));
        check(ProcessingRules.refiningReady(200,6,true));
        check(!ProcessingRules.refiningReady(200,5,true));
        System.out.println("All chainsaw logic smoke tests passed");
    }
}
