package tw.zonezero.chainsaw;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
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
import tw.zonezero.chainsaw.logic.FirstPersonGrip;

/**
 * First-person chainsaw grip ONLY. The playerAnimator layer owns the
 * third-person two-arm pose, and rendering/skin layers remain vanilla.
 *
 * Every idle/running/cutting model shares the SAME first-person item display
 * transform. Hand-space movement is interpolated smoothly as LMB is held;
 * no abrupt resize, model jump or new four-tick swing is introduced here.
 */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ChainsawTwoHandedClient {
    private ChainsawTwoHandedClient() {}

    private static boolean canWield(LivingEntity entity, ItemStack stack) {
        return entity != null
                && entity.getMainHandItem().is(ZoneZeroChainsaw.CHAINSAW.get())
                && (entity.getMainHandItem() == stack ||
                        ItemStack.isSameItemSameComponents(entity.getMainHandItem(), stack))
                && entity.getOffhandItem().isEmpty()
                && ZoneZeroChainsaw.isEngineOn(stack)
                && ZoneZeroChainsaw.fuel(stack) > 0;
    }

    @SubscribeEvent
    public static void clientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm,
                                                    ItemStack stack, float partialTick,
                                                    float equipProgress, float swingProgress) {
                if (!stack.is(ZoneZeroChainsaw.CHAINSAW.get())) return false;
                boolean running = canWield(player, stack);
                float cut = running ? ChainsawClientInput.cutProgress(partialTick) : 0.0f;
                boolean leftMain = arm == HumanoidArm.LEFT;
                FirstPersonGrip.Offset offset = FirstPersonGrip.mainHand(
                        player.getOffhandItem().isEmpty(), running, leftMain, cut);
                pose.translate(offset.x(), offset.y(), offset.z());
                // false = preserve NeoForge/vanilla first-person item and skin
                // rendering. No replacement of player render buffers or layers.
                return false;
            }
        }, ZoneZeroChainsaw.CHAINSAW.get());
    }

    @EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, value = Dist.CLIENT)
    public static final class FirstPersonSupport {
        private FirstPersonSupport() {}

        @SubscribeEvent
        public static void hand(RenderHandEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (event.getHand() != InteractionHand.OFF_HAND || mc.player == null
                    || mc.screen != null || mc.player.isSpectator()) return;
            ItemStack saw = mc.player.getMainHandItem();
            if (!canWield(mc.player, saw)) return;
            float cut = ChainsawClientInput.cutProgress(event.getPartialTick());
            FirstPersonGrip.Offset offset = FirstPersonGrip.supportHand(
                    mc.player.getMainArm() == HumanoidArm.LEFT, cut);
            event.getPoseStack().translate(offset.x(), offset.y(), offset.z());
            // Tilt only the supporting arm toward the forward U guard.
            float tilt = -12.0f - 7.0f * cut;
            event.getPoseStack().mulPose(
                    com.mojang.math.Axis.XP.rotationDegrees(tilt));
        }
    }
}
