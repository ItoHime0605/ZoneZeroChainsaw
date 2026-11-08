package tw.zonezero.chainsaw;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Select a compact 3D model only when the actual item stack is held. */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientItemModels {
    private ClientItemModels() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation held = ResourceLocation.fromNamespaceAndPath(ZoneZeroChainsaw.MODID, "held");
            ItemProperties.register(ZoneZeroChainsaw.FUEL.get(), held,
                    (stack, level, entity, seed) -> isHeld(stack, entity) ? 1.0f : 0.0f);
            ItemProperties.register(ZoneZeroChainsaw.CRUDE.get(), held,
                    (stack, level, entity, seed) -> isHeld(stack, entity) ? 1.0f : 0.0f);
        });
    }

    private static boolean isHeld(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return entity != null && (entity.getMainHandItem() == stack || entity.getOffhandItem() == stack);
    }
}
