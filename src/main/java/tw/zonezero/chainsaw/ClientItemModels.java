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
            ItemProperties.register(ZoneZeroChainsaw.CHAINSAW.get(),
                    ResourceLocation.fromNamespaceAndPath(ZoneZeroChainsaw.MODID, "engine"),
                    (stack, level, entity, seed) -> {
                        if (ZoneZeroChainsaw.isEngineOn(stack)) return 1.0F;
                        net.minecraft.world.level.Level world = level != null ? level
                                : entity != null ? entity.level() : null;
                        return world != null && ZoneZeroChainsaw.coastUntil(stack) > world.getGameTime()
                                ? 0.5F : 0.0F;
                    });
            ItemProperties.register(ZoneZeroChainsaw.CHAINSAW.get(),
                    ResourceLocation.fromNamespaceAndPath(ZoneZeroChainsaw.MODID, "cutting"),
                    (stack, level, entity, seed) -> {
                        if (!ZoneZeroChainsaw.isEngineOn(stack) || entity == null) return 0.0F;
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.player == entity && mc.screen == null
                                && entity.getMainHandItem() == stack
                                && mc.options.keyAttack.isDown()) return 1.0F;
                        if (level == null) return 0.0F;
                        long last = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                net.minecraft.world.item.component.CustomData.EMPTY)
                                .copyTag().getLong("ChainsawCuttingAt");
                        return last > 0 && level.getGameTime() - last <= 6
                                && level.getGameTime() >= last ? 1.0F : 0.0F;
                    });

            // Minecraft 1.21.1 uses predicate-based model overrides. The
            // undyed saw stays orange (0); each DyeColor ID occupies one
            // exact step in the range 1/16 through 16/16.
            ItemProperties.register(
                    ZoneZeroChainsaw.CHAINSAW.get(),
                    ResourceLocation.fromNamespaceAndPath(ZoneZeroChainsaw.MODID, "dye_color"),
                    (stack, level, entity, seed) -> {
                        var custom = stack.getOrDefault(
                                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
                        if (!custom.contains("ChainsawColor")) return 0.0F;
                        return (ZoneZeroChainsaw.dyeColor(stack) + 1) / 16.0F;
                    });

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
