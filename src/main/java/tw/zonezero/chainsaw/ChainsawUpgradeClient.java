package tw.zonezero.chainsaw;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ChainsawUpgradeClient {
    private ChainsawUpgradeClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ZoneZeroChainsaw.UPGRADE_MENU.get(), ChainsawUpgradeScreen::new);
    }
}
