package tw.zonezero.chainsaw;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Lightweight fuel status above the hotbar; no texture atlas or external assets required. */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, value = Dist.CLIENT)
public final class ChainsawHud {
    private ChainsawHud() {}

    @SubscribeEvent
    public static void afterHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.options.hideGui) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!stack.is(ZoneZeroChainsaw.CHAINSAW.get())) return;
        GuiGraphics g = event.getGuiGraphics();
        int x = g.guiWidth() - 150;
        int y = g.guiHeight() - 69;
        int fuel = ZoneZeroChainsaw.fuel(stack);
        int max = Math.max(1, ZoneZeroChainsaw.fuelCapacity(stack));
        boolean on = ZoneZeroChainsaw.isEngineOn(stack);
        g.fill(x, y, x + 143, y + 37, 0xc91b2026);
        g.fill(x, y, x + 143, y + 2, on ? 0xffd08b3b : 0xff4e555d);
        g.drawString(mc.font, Component.translatable("hud.zonezero_chainsaw.title",
                ZoneZeroChainsaw.tier(stack)), x + 6, y + 5, 0xffe5e5dd, false);
        g.drawString(mc.font, Component.translatable("hud.zonezero_chainsaw.fuel", fuel, max),
                x + 6, y + 17, 0xffa9bdc2, false);
        int bar = (int)(128L * fuel / max);
        g.fill(x + 6, y + 29, x + 134, y + 34, 0xff343a42);
        if (bar > 0) g.fill(x + 6, y + 29, x + 6 + bar, y + 34,
                fuel * 5 < max ? 0xffcc5045 : on ? 0xffdaaa4d : 0xff74bb68);
        g.fill(x + 137, y + 6, x + 140, y + 15, on ? 0xffe6b555 : 0xff5d6368);
    }
}
