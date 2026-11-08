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

        private boolean ready() {
            ItemStack stack = player.getMainHandItem();
            return player.isAlive()
                    && stack.is(ZoneZeroChainsaw.CHAINSAW.get())
                    && player.getOffhandItem().isEmpty()
                    && ZoneZeroChainsaw.isEngineOn(stack)
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
            oldHold = hold;
            oldCut = cut;
            hold = SustainedCutLogic.approach(hold, ready() ? 1.0f : 0.0f);
            cut = SustainedCutLogic.approach(cut, cutting() ? 1.0f : 0.0f);
        }

        @Override
        public boolean isActive() {
            // Allow a four-tick fade-out when R is released.
            return ready() || hold > 0.0f || cut > 0.0f;
        }

        @Override
        public Vec3f get3DTransform(String part, TransformType type, float delta, Vec3f base) {
            if (type != TransformType.ROTATION) return base;
            boolean leftMain = player.getMainArm() == HumanoidArm.LEFT;
            boolean rightArm = "rightArm".equals(part) || "right_arm".equals(part);
            boolean leftArm = "leftArm".equals(part) || "left_arm".equals(part);
            if (!rightArm && !leftArm) return base;

            float support = SustainedCutLogic.interpolate(oldHold, hold, delta);
            float forward = SustainedCutLogic.interpolate(oldCut, cut, delta);
            if (support <= 0.0f) return base;

            // Right-handed default. When main arm is left, the two roles
            // mirror instead of putting the left hand behind the machine.
            boolean main = leftMain ? leftArm : rightArm;
            float mirror = rightArm ? 1.0f : -1.0f;

            float readyX = main ? -1.05f : -1.17f;
            float readyY = main ? -0.23f * mirror : -0.40f * mirror;
            float readyZ = main ? 0.08f * mirror : -0.12f * mirror;

            // Both hands remain extended until the user RELEASES left click.
            float cuttingX = main ? -1.47f : -1.56f;
            float cuttingY = main ? -0.13f * mirror : -0.29f * mirror;
            float cuttingZ = main ? 0.06f * mirror : -0.08f * mirror;

            float x = readyX + (cuttingX - readyX) * forward;
            float y = readyY + (cuttingY - readyY) * forward;
            float z = readyZ + (cuttingZ - readyZ) * forward;
            return new Vec3f(
                    base.getX() + (x - base.getX()) * support,
                    base.getY() + (y - base.getY()) * support,
                    base.getZ() + (z - base.getZ()) * support);
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
