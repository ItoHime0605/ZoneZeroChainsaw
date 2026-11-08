package tw.zonezero.chainsaw;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import tw.zonezero.chainsaw.logic.SustainedCutLogic;
import tw.zonezero.chainsaw.logic.ThirdPersonGrip;

/**
 * Two DISTINCT static two-hand poses:
 * engine running -> support/ready stance; held attack -> fixed forward brace.
 * Animation NEVER restarts on 4-tick damage pulses.
 *
 * Uses only the player animation API; never modifies skin colors, render
 * buffers, body/head/legs, or TaCZ gun data. First-person arms and weapon are
 * positioned independently by ChainsawTwoHandedClient.
 */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ChainsawPlayerAnimator {
    private static final ResourceLocation POSE_ID =
            ResourceLocation.fromNamespaceAndPath(ZoneZeroChainsaw.MODID, "two_hand_hold");

    private ChainsawPlayerAnimator() {}

    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                POSE_ID, 1200, StableHoldPose::new));
    }

    private static final class StableHoldPose implements IAnimation {
        private final AbstractClientPlayer player;
        private float oldHold;
        private float hold;
        private float oldCut;
        private float cut;

        private StableHoldPose(AbstractClientPlayer player) {
            this.player = player;
        }

        private boolean mayAnimateSaw() {
            // Release our layer IMMEDIATELY on a mainhand weapon swap,
            // occupied offhand, death or spectating. Otherwise a four-tick
            // fade-out could fight TaCZ gun animations after changing items.
            return player.isAlive()
                    && !player.isSpectator()
                    && player.getMainHandItem().is(ZoneZeroChainsaw.CHAINSAW.get())
                    && player.getOffhandItem().isEmpty();
        }

        private boolean ready() {
            if (!mayAnimateSaw()) return false;
            ItemStack stack = player.getMainHandItem();
            return ZoneZeroChainsaw.isEngineOn(stack)
                    && ZoneZeroChainsaw.fuel(stack) > 0;
        }

        private boolean cutting() {
            if (!ready()) return false;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == player) {
                return ChainsawClientInput.isHoldingCut();
            }
            // The server sets this once when cutting begins and clears it on
            // release/timeout. Remote players do not need keyboard polling.
            return ZoneZeroChainsaw.isCutting(player.getMainHandItem());
        }

        @Override
        public void tick() {
            if (!mayAnimateSaw()) {
                oldHold = hold = 0.0f;
                oldCut = cut = 0.0f;
                return;
            }
            oldHold = hold;
            oldCut = cut;
            hold = SustainedCutLogic.approach(hold, ready() ? 1.0f : 0.0f);
            cut = SustainedCutLogic.approach(cut, cutting() ? 1.0f : 0.0f);
        }

        @Override
        public boolean isActive() {
            // Allow a four-tick fade-out when R is released.
            return mayAnimateSaw() && (ready() || hold > 0.0f || cut > 0.0f);
        }

        @Override
        public Vec3f get3DTransform(String part, TransformType type, float delta, Vec3f base) {
            // Only shoulder rotations for both arms. Other player animation
            // layers and custom model/skin bones are not modified.
            if (type != TransformType.ROTATION) return base;
            boolean rightArm = "rightArm".equals(part) || "right_arm".equals(part);
            boolean leftArm = "leftArm".equals(part) || "left_arm".equals(part);
            if (!rightArm && !leftArm) return base;

            float holdBlend = SustainedCutLogic.interpolate(oldHold, hold, delta);
            if (holdBlend <= 0f) return base;
            float cutBlend = SustainedCutLogic.interpolate(oldCut, cut, delta);

            ThirdPersonGrip.Rotation aim =
                    ThirdPersonGrip.arm(
                            player.getMainArm() == HumanoidArm.LEFT,
                            rightArm, cutBlend, player.isCrouching(),
                            player.getDeltaMovement().horizontalDistanceSqr(),
                            player.tickCount + delta);

            return new Vec3f(
                    base.getX() + (aim.x() - base.getX()) * holdBlend,
                    base.getY() + (aim.y() - base.getY()) * holdBlend,
                    base.getZ() + (aim.z() - base.getZ()) * holdBlend);
        }

        @Override
        public void setupAnim(float partialTicks) {}

        @Override
        public FirstPersonMode getFirstPersonMode(float delta) {
            // Render only the third-person avatar with this layer. First-person
            // item and hands use the normal NeoForge renderer to avoid TaCZ
            // and player skin layer conflicts.
            return FirstPersonMode.NONE;
        }
    }
}
