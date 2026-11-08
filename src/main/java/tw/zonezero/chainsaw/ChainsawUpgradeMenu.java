package tw.zonezero.chainsaw;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeItem;
import net.minecraft.core.registries.BuiltInRegistries;

/** One chainsaw, three material slots and one take-only output. */
public final class ChainsawUpgradeMenu extends AbstractContainerMenu {
    private final Container input = new SimpleContainer(4) {
        @Override public void setChanged() {
            super.setChanged();
            if (!updating) updateOutput();
        }
    };
    private final Container output = new SimpleContainer(1);
    private boolean updating;

    public ChainsawUpgradeMenu(int id, Inventory inventory) {
        super(ZoneZeroChainsaw.UPGRADE_MENU.get(), id);
        addSlot(new Slot(input, 0, 25, 36) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ZoneZeroChainsaw.CHAINSAW.get()); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 0; i < 3; i++) addSlot(new Slot(input, i + 1, 70 + 18 * i, 36));
        addSlot(new Slot(output, 0, 150, 36) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return !getItem().isEmpty(); }
            @Override public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                consumeInputs();
            }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }

    private ItemStack[] materials() {
        return new ItemStack[] {input.getItem(1), input.getItem(2), input.getItem(3)};
    }

    private void updateOutput() {
        updating = true;
        output.setItem(0, ChainsawUpgradeRules.preview(input.getItem(0), materials()));
        updating = false;
        broadcastChanges();
    }

    private void consumeInputs() {
        updating = true;
        boolean dye = ChainsawUpgradeRules.isDyeOperation(materials());
        int tier = ChainsawUpgradeRules.resultingTier(input.getItem(0));
        String material = ChainsawUpgradeRules.materialId(tier);
        int amount = ChainsawUpgradeRules.requiredCount(tier);
        input.setItem(0, ItemStack.EMPTY);
        for (int i = 1; i < 4; i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (dye) { if (stack.getItem() instanceof DyeItem) stack.shrink(1); }
            else if (stack.is(ZoneZeroChainsaw.FUEL.get())) stack.shrink(1);
            else if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(material)) {
                int used = Math.min(amount, stack.getCount());
                stack.shrink(used);
                amount -= used;
            }
        }
        output.setItem(0, ItemStack.EMPTY);
        updating = false;
        updateOutput();
    }

    @Override public boolean stillValid(Player player) { return true; }

    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                ItemStack stack = input.removeItemNoUpdate(i);
                if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
            }
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        // Output shift-click disabled to avoid duplication; normal pickup remains available.
        if (index < 5 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (original.is(ZoneZeroChainsaw.CHAINSAW.get())) {
            if (!moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 1, 4, false)) return ItemStack.EMPTY;
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
