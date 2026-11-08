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
                        // Inventory, dropped-item and GUI previews MUST never
                        // animate as if someone were wielding the chainsaw.
                        // This also prevents stale engine NBT from running
                        // client-side before the server clears it on pickup.
                        if (!isMainHandSaw(stack, entity)
                                || !entity.getOffhandItem().isEmpty()) return 0.0f;
                        net.minecraft.world.level.Level world = level != null ? level
                                : entity.level();
                        return tw.zonezero.chainsaw.logic.ChainMotion.engineModel(
                                ZoneZeroChainsaw.isEngineOn(stack),
                                ZoneZeroChainsaw.coastUntil(stack),
                                world == null ? -1L : world.getGameTime());
                    });
            ItemProperties.register(ZoneZeroChainsaw.CHAINSAW.get(),
                    ResourceLocation.fromNamespaceAndPath(ZoneZeroChainsaw.MODID, "cutting"),
                    (stack, level, entity, seed) -> {
                        if (!ZoneZeroChainsaw.isEngineOn(stack)
                                || ZoneZeroChainsaw.fuel(stack) <= 0
                                || entity == null
                                || !entity.getOffhandItem().isEmpty()
                                || !isMainHandSaw(stack, entity)) return 0.0f;

                        // Local input is predictive; remote players render
                        // the authoritative, replicated cutting flag.
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.player == entity) {
                            return ChainsawClientInput.isHoldingCut() ? 1.0f : 0.0f;
                        }
                        return ZoneZeroChainsaw.isCutting(stack) ? 1.0f : 0.0f;
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

    /**
     * Some render pipelines supply a defensive ItemStack copy rather than the
     * same Java object. Compare components as well as object identity, but
     * still require this player to wield a chainsaw in the MAIN hand.
     */
    private static boolean isMainHandSaw(ItemStack stack,
            net.minecraft.world.entity.LivingEntity entity) {
        if (entity == null) return false;
        ItemStack held = entity.getMainHandItem();
        return held.is(ZoneZeroChainsaw.CHAINSAW.get()) &&
                (held == stack || ItemStack.isSameItemSameComponents(held, stack));
    }

    private static boolean isHeld(ItemStack stack,
            net.minecraft.world.entity.LivingEntity entity) {
        if (entity == null) return false;
        ItemStack main = entity.getMainHandItem();
        ItemStack off = entity.getOffhandItem();
        return (main == stack || (!main.isEmpty() &&
                ItemStack.isSameItemSameComponents(main, stack))) ||
               (off == stack || (!off.isEmpty() &&
                ItemStack.isSameItemSameComponents(off, stack)));
    }
}
