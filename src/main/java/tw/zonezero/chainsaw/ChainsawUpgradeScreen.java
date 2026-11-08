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
        int tier = menu.nextTier();
        if (menu.isDye()) {
            graphics.drawString(font, "染色：消耗 1 個染料", x + 42, y + 60, 0xffd8b27b, false);
        } else if (menu.materialMissing()) {
            graphics.drawString(font,
                    Component.translatable("upgrade.zonezero_chainsaw.missing_material"),
                    x + 13, y + 61, 0xffeca79f, false);
        } else if (tier >= 0 && tier <= 5) {
            String[] names = {"鐵方塊", "鐵方塊", "鈦錠", "黑金屬方塊", "鑽石方塊", "獄髓方塊"};
            int count = ChainsawUpgradeRules.requiredCount(tier);
            graphics.drawString(font, names[tier] + " x" + count + " + 生質燃油瓶 x1", x + 34, y + 59, 0xffe4bd85, false);
            graphics.drawString(font, "傷害 " + menu.nextDamage() + " / 油箱 " + menu.nextFuelCapacity(), x + 34, y + 69, 0xffa8d9a4, false);
        } else {
            graphics.drawString(font, "放入電鋸與升級材料或染料", x + 15, y + 62, 0xffd8b27b, false);
        }
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
