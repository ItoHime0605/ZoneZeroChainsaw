package tw.zonezero.chainsaw;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * TaCZ-compatible placement, but fully independent rendering.
 * TaCZ's ammo overlay is for IGun equipment; the chainsaw is NOT a gun.
 * Its own compact weapon-status widget appears only while a saw is held.
 * All graphics are original primitives: no TACZ UI assets are copied.
 */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, value = Dist.CLIENT)
public final class ChainsawHud {
    private ChainsawHud() {}

    @SubscribeEvent
    public static void afterHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.options.hideGui || mc.player.isSpectator()) return;
        ItemStack saw = mc.player.getMainHandItem();
        if (!saw.is(ZoneZeroChainsaw.CHAINSAW.get())) return;

        GuiGraphics g = event.getGuiGraphics();
        int fuel = ZoneZeroChainsaw.fuel(saw);
        int max = Math.max(1, ZoneZeroChainsaw.fuelCapacity(saw));
        int tier = ZoneZeroChainsaw.tier(saw);
        boolean needsOffhand = !mc.player.getOffhandItem().isEmpty();
        boolean on = !needsOffhand && ZoneZeroChainsaw.isEngineOn(saw);

        // Mirrors the location and visual hierarchy of a tactical firearm
        // ammo widget without touching TaCZ's overlay or player renderer.
        int w = 176;
        int h = 64;
        int x = g.guiWidth() - w - 8;
        int y = g.guiHeight() - h - 32;
        if (x < 2 || y < 2) return;

        g.fill(x, y, x + w, y + h, 0xc71a2028);
        g.fill(x + 1, y + 1, x + w - 1, y + 3, on ? 0xffe2a047 : 0xff727b84);
        g.fill(x + 1, y + 45, x + w - 1, y + 46, 0x98717a80);
        g.fill(x + 124, y + 7, x + 125, y + 43, 0x99717b84);

        // Left: fuel capacity, analogous to ammunition count.
        g.drawString(mc.font,
                Component.translatable("hud.zonezero_chainsaw.compact_title", tier),
                x + 8, y + 8, 0xffdce4e5, false);
        g.drawString(mc.font, Integer.toString(fuel), x + 8, y + 21,
                fuel * 5 <= max ? 0xffe96860 : 0xfff4f2e9, false);
        g.drawString(mc.font, "/" + max, x + 8 + mc.font.width(Integer.toString(fuel)) + 4,
                y + 23, 0xff9aaab0, false);

        int barX = x + 8;
        int barY = y + 37;
        int barLength = 106;
        int filled = (int) (barLength * (long) fuel / max);
        g.fill(barX, barY, barX + barLength, barY + 5, 0xff424b53);
        if (filled > 0) {
            int c = fuel * 5 <= max ? 0xffdf645a : on ? 0xffe4b367 : 0xff92c6b5;
            g.fill(barX, barY, barX + filled, barY + 5, c);
        }

        // Right: persistent engine readout. Invalid two-hand use is explicit.
        int c = needsOffhand ? 0xffed7770 : on ? 0xffeec479 : 0xffa9b4b8;
        String key = needsOffhand
                ? "hud.zonezero_chainsaw.blocked"
                : on ? "hud.zonezero_chainsaw.running" : "hud.zonezero_chainsaw.stopped";
        g.drawString(mc.font, Component.translatable(key), x + 131, y + 14, c, false);
        g.drawString(mc.font, Component.translatable("hud.zonezero_chainsaw.unit"),
                x + 131, y + 29, 0xff8e9ba2, false);

        // Bottom strip is concise and never requires TaCZ or gun ammo state.
        String footer = needsOffhand
                ? "hud.zonezero_chainsaw.free_hand"
                : on ? "hud.zonezero_chainsaw.hold_cut"
                : "hud.zonezero_chainsaw.press_r";
        g.drawString(mc.font, Component.translatable(footer),
                x + 8, y + 51, needsOffhand ? 0xffed7770 : 0xffaab7ba, false);
    }
}
