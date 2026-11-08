package tw.zonezero.chainsaw;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Original compact pixel guide-bar gauge.
 * Off: cool grey. Running: red. Fuel value only: e.g. 300/600.
 * No text panels, TaCZ resource copies, or extra status banners.
 */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, value = Dist.CLIENT)
public final class ChainsawHud {
    private ChainsawHud() {}

    private static void stroke(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    @SubscribeEvent
    public static void afterHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.options.hideGui || mc.player.isSpectator()) return;
        ItemStack saw = mc.player.getMainHandItem();
        if (!saw.is(ZoneZeroChainsaw.CHAINSAW.get())) return;
        GuiGraphics g = event.getGuiGraphics();
        int x = g.guiWidth() - 157;
        int y = g.guiHeight() - 52;
        if (x < 3 || y < 3) return;

        boolean running = ZoneZeroChainsaw.isEngineOn(saw) && mc.player.getOffhandItem().isEmpty();
        int edge = running ? 0xffed4847 : 0xffc1c9ca;
        int highlight = running ? 0xffff8b7c : 0xffecf0e8;
        int shade = running ? 0xff922826 : 0xff657277;

        // Light shadow to keep thin guide-bar lines visible over day or night.
        stroke(g, x + 5, y + 6, 82, 23, 0x440e1316);

        // Outer saw bar: a flat-sided, pixel-chamfered nose pointing left.
        stroke(g, x + 12, y + 4, 75, 3, edge);
        stroke(g, x + 7, y + 7, 5, 3, edge);
        stroke(g, x + 4, y + 10, 3, 13, edge);
        stroke(g, x + 7, y + 23, 5, 3, edge);
        stroke(g, x + 12, y + 26, 75, 3, edge);
        stroke(g, x + 84, y + 7, 3, 19, shade);

        // Distinct recessed center of the guide bar, not a fuel progress bar.
        stroke(g, x + 22, y + 11, 61, 2, highlight);
        stroke(g, x + 16, y + 13, 6, 3, highlight);
        stroke(g, x + 14, y + 16, 3, 3, shade);
        stroke(g, x + 16, y + 19, 6, 3, shade);
        stroke(g, x + 22, y + 22, 61, 2, shade);
        stroke(g, x + 82, y + 13, 2, 9, shade);

        // Seven tiny teeth on each longitudinal side, plus sprocket teeth at the nose.
        for (int i = 0; i < 7; i++) {
            int tx = x + 15 + i * 10;
            stroke(g, tx, y + 1, 3, 3, i % 2 == 0 ? shade : edge);
            stroke(g, tx + 2, y + 29, 3, 3, i % 2 == 0 ? edge : shade);
        }
        stroke(g, x + 1, y + 12, 3, 4, edge);
        stroke(g, x + 1, y + 19, 3, 4, shade);

        // Single numerical readout. It follows the real ItemStack tank capacity.
        int fuel = ZoneZeroChainsaw.fuel(saw);
        int max = ZoneZeroChainsaw.fuelCapacity(saw);
        String counter = fuel + "/" + max;
        g.pose().pushPose();
        g.pose().translate(x + 94, y + 12, 0);
        g.pose().scale(1.08f, 1.08f, 1.0f);
        g.drawString(mc.font, counter, 0, 0, running ? 0xffffdddd : 0xfff1f2ee, true);
        g.pose().popPose();
    }
}
