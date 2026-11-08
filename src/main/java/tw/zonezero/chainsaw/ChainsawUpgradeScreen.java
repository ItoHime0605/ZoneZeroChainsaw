package tw.zonezero.chainsaw;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ChainsawUpgradeScreen extends AbstractContainerScreen<ChainsawUpgradeMenu> {
    public ChainsawUpgradeScreen(ChainsawUpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xff262a2e);
        graphics.fill(x + 2, y + 2, x + 174, y + 164, 0xff46494c);
        graphics.fill(x + 5, y + 5, x + 171, y + 76, 0xff28292b);
        graphics.fill(x + 5, y + 77, x + 171, y + 163, 0xff333638);
        for (int i = 0; i < 5; i++) {
            int sx = i == 0 ? 24 : i == 4 ? 149 : 69 + (i - 1) * 18;
            graphics.fill(x + sx - 1, y + 35, x + sx + 17, y + 53, 0xff111417);
            graphics.fill(x + sx, y + 36, x + sx + 16, y + 52, 0xff626468);
        }
        graphics.drawString(font, Component.literal(">").getString(), x + 49, y + 40, 0xffe3a34e, false);
        graphics.drawString(font, Component.literal(">").getString(), x + 132, y + 40, 0xffe3a34e, false);
        graphics.drawString(font, Component.translatable("gui.zonezero_chainsaw.materials"), x + 66, y + 59, 0xffd8b27b, false);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int sx = x + 7 + col * 18, sy = y + 83 + row * 18;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xff171a1d);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xff66676a);
        }
        for (int col = 0; col < 9; col++) {
            int sx = x + 7 + col * 18, sy = y + 141;
            graphics.fill(sx, sy, sx + 18, sy + 18, 0xff171a1d);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xff66676a);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
