package tw.zonezero.chainsaw;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Defense-in-depth: the offhand-empty rule also applies to vanilla
 * interactions, not only our C2S continuous cutting packets. No inventory
 * changes or forced item drops; an unsupported saw simply cannot be used.
 */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID)
public final class ChainsawWieldRestrictions {
    private ChainsawWieldRestrictions() {}

    private static boolean blocked(Player player) {
        return player.getMainHandItem().is(ZoneZeroChainsaw.CHAINSAW.get())
                && !player.getOffhandItem().isEmpty();
    }

    @SubscribeEvent
    public static void attackEntity(AttackEntityEvent event) {
        // Suppress the vanilla one-shot attack even if a client bypasses its
        // own input hook. Our 4-tick server-controlled saw contact is separate
        // from Player.attack(), so this cannot grant free unpowered damage.
        if (event.getEntity().getMainHandItem().is(ZoneZeroChainsaw.CHAINSAW.get()))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }
}
