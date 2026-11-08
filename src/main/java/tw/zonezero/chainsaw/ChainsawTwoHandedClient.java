package tw.zonezero.chainsaw;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Real third-person two-hand player arm pose, plus first-person positioning
 * of the empty support hand. Does not change skin color or renderer state.
 */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ChainsawTwoHandedClient {
    private ChainsawTwoHandedClient() {}

    private static boolean canWield(LivingEntity entity, ItemStack stack) {
        return entity != null && entity.getMainHandItem() == stack
                && entity.getOffhandItem().isEmpty()
                && ZoneZeroChainsaw.isEngineOn(stack);
    }

    @SubscribeEvent
    public static void clientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm,
                                                    ItemStack stack, float partialTick,
                                                    float equipProgress, float swingProgress) {
                if (!stack.is(ZoneZeroChainsaw.CHAINSAW.get())) return false;
                if (!player.getOffhandItem().isEmpty()) {
                    // Without both free hands, the unpowered weight hangs low.
                    pose.translate(0.0, -0.42, 0.22);
                } else if (ZoneZeroChainsaw.isEngineOn(stack)) {
                    // Pull the running saw into a centered, two-handed grip.
                    pose.translate(arm == HumanoidArm.RIGHT ? -0.12 : 0.12, -0.10, -0.22);
                }
                // Preserve vanilla's arm/skin rendering and existing model transform.
                return false;
            }
        }, ZoneZeroChainsaw.CHAINSAW.get());
    }

    @EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, value = Dist.CLIENT)
    public static final class FirstPersonSupport {
        private FirstPersonSupport() {}

        @SubscribeEvent
        public static void hand(RenderHandEvent event) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (event.getHand() != InteractionHand.OFF_HAND || mc.player == null) return;
            ItemStack stack = mc.player.getMainHandItem();
            if (!canWield(mc.player, stack)) return;
            // The vanilla empty offhand arm is moved toward the front guard.
            // No layer replacements or RGB manipulation.
            event.getPoseStack().translate(0.17, 0.18, -0.42);
            event.getPoseStack().mulPose(com.mojang.math.Axis.XP.rotationDegrees(-16));
        }
    }
}
