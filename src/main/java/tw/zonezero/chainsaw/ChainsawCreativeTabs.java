package tw.zonezero.chainsaw;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Dedicated creative inventory tab for Zone Zero chainsaw equipment. */
public final class ChainsawCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ZoneZeroChainsaw.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.zonezero_chainsaw"))
                    .icon(() -> new ItemStack(ZoneZeroChainsaw.CHAINSAW.get()))
                    .displayItems((parameters, output) -> {
                        // The basic saw starts at Lv.0, with an empty tank and orange housing.
                        output.accept(ZoneZeroChainsaw.CHAINSAW.get());
                        output.accept(ZoneZeroChainsaw.FUEL.get());
                        output.accept(ZoneZeroChainsaw.CRUDE.get());
                        output.accept(ZoneZeroChainsaw.FERMENTER_ITEM.get());
                        output.accept(ZoneZeroChainsaw.REFINERY_ITEM.get());
                        output.accept(ZoneZeroChainsaw.UPGRADE_STATION_ITEM.get());
                    })
                    .build());

    private ChainsawCreativeTabs() {}
}
