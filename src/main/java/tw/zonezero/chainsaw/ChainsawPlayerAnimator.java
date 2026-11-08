package tw.zonezero.chainsaw;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Stable and independent two-hand bracing pose using playerAnimator 2.0.4.
 * No modification to a player's skin color, entity renderer, or TaCZ data.
 * Legs and other body parts are intentionally untouched.
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

        private StableHoldPose(AbstractClientPlayer player) {
            this.player = player;
        }

        @Override
        public boolean isActive() {
            ItemStack saw = player.getMainHandItem();
            return player.isAlive()
                    && saw.is(ZoneZeroChainsaw.CHAINSAW.get())
                    && player.getOffhandItem().isEmpty()
                    && ZoneZeroChainsaw.isEngineOn(saw);
        }

        @Override
        public Vec3f get3DTransform(String part, TransformType type, float delta, Vec3f base) {
            // Return a fixed position, not an animated attack/swing cycle.
            // The arm stays extended for the whole time the motor runs.
            if (type != TransformType.ROTATION) return base;
            return switch (part) {
                case "rightArm", "right_arm" -> new Vec3f(-1.28f, -.24f, .08f);
                case "leftArm", "left_arm" -> new Vec3f(-1.34f, .42f, -.12f);
                default -> base;
            };
        }

        @Override
        public void setupAnim(float tickDelta) {}

        @Override
        public FirstPersonMode getFirstPersonMode(float delta) {
            // First person keeps the Minecraft hands and our own item model.
            // Do not let the library replace TaCZ's or Minecraft's renderer.
            return FirstPersonMode.NONE;
        }
    }
}
