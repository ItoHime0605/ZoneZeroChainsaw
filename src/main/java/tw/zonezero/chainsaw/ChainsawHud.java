package tw.zonezero.chainsaw;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import tw.zonezero.chainsaw.logic.ChainsawHudStyle;

/**
 * Original, transparent guide-bar gauge: the only state-dependent visual
 * is its silver/red blade image. Fuel digits ALWAYS use the same ivory-white
 * color and cool slate outline (even at zero fuel and with the engine on).
 * No TaCZ assets, oversized black panels, animation shaders or custom fonts.
 */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, value = Dist.CLIENT)
public final class ChainsawHud {
    private static final ResourceLocation BAR_IDLE = ResourceLocation.fromNamespaceAndPath(
            ZoneZeroChainsaw.MODID, "textures/gui/guide_idle.png");
    private static final ResourceLocation BAR_RUNNING = ResourceLocation.fromNamespaceAndPath(
            ZoneZeroChainsaw.MODID, "textures/gui/guide_running.png");

    private ChainsawHud() {}

    @SubscribeEvent
    public static void afterHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.options.hideGui ||
                mc.player.isSpectator()) return;
        ItemStack saw = mc.player.getMainHandItem();
        if (!saw.is(ZoneZeroChainsaw.CHAINSAW.get())) return;

        GuiGraphics g = event.getGuiGraphics();
        if (g.guiWidth() < ChainsawHudStyle.TOTAL_WIDTH + 8 ||
                g.guiHeight() < ChainsawHudStyle.HEIGHT + 50) return;
        int x = g.guiWidth() - ChainsawHudStyle.TOTAL_WIDTH - 9;
        int y = g.guiHeight() - ChainsawHudStyle.HEIGHT - 17;

        boolean running = ZoneZeroChainsaw.isEngineOn(saw)
                && mc.player.getOffhandItem().isEmpty()
                && ZoneZeroChainsaw.fuel(saw) > 0;

        // Engine state affects only which ORIGINAL blade artwork is rendered.
        ResourceLocation bar = running ? BAR_RUNNING : BAR_IDLE;
        g.blit(bar, x, y, 0, 0,
                ChainsawHudStyle.BAR_WIDTH, ChainsawHudStyle.HEIGHT,
                ChainsawHudStyle.BAR_WIDTH, ChainsawHudStyle.HEIGHT);

        // A single fixed-color fuel value. Not a separate percentage bar.
        String digits = ZoneZeroChainsaw.fuel(saw) + "/" + ZoneZeroChainsaw.fuelCapacity(saw);
        int tx = x + ChainsawHudStyle.BAR_WIDTH + 7;
        int ty = y + 15;
        g.pose().pushPose();
        g.pose().translate(tx, ty, 0.0f);
        g.pose().scale(ChainsawHudStyle.TEXT_SCALE, ChainsawHudStyle.TEXT_SCALE, 1.0f);

        // Four-pixel slate outline gives stable contrast on snow, sunlight,
        // night, dark grass and shader scenes without a black background.
        g.drawString(mc.font, digits, -1, 0, ChainsawHudStyle.TEXT_OUTLINE, false);
        g.drawString(mc.font, digits, 1, 0, ChainsawHudStyle.TEXT_OUTLINE, false);
        g.drawString(mc.font, digits, 0, -1, ChainsawHudStyle.TEXT_OUTLINE, false);
        g.drawString(mc.font, digits, 0, 1, ChainsawHudStyle.TEXT_OUTLINE, false);
        g.drawString(mc.font, digits, 0, 0, ChainsawHudStyle.TEXT_COLOR, false);
        g.pose().popPose();
    }
}
