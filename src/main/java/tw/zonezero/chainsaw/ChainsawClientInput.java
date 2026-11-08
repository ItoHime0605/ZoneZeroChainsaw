package tw.zonezero.chainsaw;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Captures R and held left mouse; the server decides all actual cutting. */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, value = Dist.CLIENT)
public final class ChainsawClientInput {
    private static boolean lastAttackHeld;
    private static int tickCounter;

    private ChainsawClientInput() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            lastAttackHeld = false;
            return;
        }
        boolean heldSaw = mc.player.getMainHandItem().is(ZoneZeroChainsaw.CHAINSAW.get());
        boolean active = mc.screen == null && heldSaw;
        while (ChainsawClientKeys.TOGGLE_ENGINE.consumeClick()) {
            if (active) PacketDistributor.sendToServer(new ChainsawControlPacket((byte) 1));
        }
        boolean attacking = active && mc.player.getOffhandItem().isEmpty()
                && ZoneZeroChainsaw.isEngineOn(mc.player.getMainHandItem())
                && mc.options.keyAttack.isDown();
        if (attacking != lastAttackHeld || (attacking && ++tickCounter >= 4)) {
            PacketDistributor.sendToServer(new ChainsawControlPacket((byte) (attacking ? 2 : 3)));
            tickCounter = 0;
        }
        lastAttackHeld = attacking;
    }

    @SubscribeEvent
    public static void preventVanillaSingleSwing(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isAttack() || mc.player == null || mc.screen != null) return;
        if (mc.player.getMainHandItem().is(ZoneZeroChainsaw.CHAINSAW.get())) {
            // The new repeated server cutting logic replaces the vanilla one-off hit.
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
