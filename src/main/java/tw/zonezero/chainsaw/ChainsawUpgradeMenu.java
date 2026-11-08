package tw.zonezero.chainsaw;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import tw.zonezero.chainsaw.logic.UpgradePlanLogic;

/**
 * Four inputs (one chainsaw, three ingredients) and a virtual result.
 * The result slot may ONLY be taken by an ordinary PICKUP when the server
 * independently validates the entire recipe. Shift-click, Q, number-key,
 * hotbar/offhand swap, cloning and drag-to-output are forbidden.
 *
 * Output is NEVER returned during menu close because it is a preview;
 * only original physical input stacks are returned to the player.
 */
public final class ChainsawUpgradeMenu extends AbstractContainerMenu {
    private final Container input = new SimpleContainer(4) {
        @Override public void setChanged() {
            super.setChanged();
            if (!updating) updateOutput();
        }
    };
    private final Container output = new SimpleContainer(1);
    private boolean updating;
    private final net.minecraft.world.level.Level level;
    private final BlockPos stationPos;

    /** Kept for the vanilla client factory; the server sends no custom data. */
    public ChainsawUpgradeMenu(int id, Inventory inventory) {
        this(id, inventory, inventory.player.blockPosition());
    }

    public ChainsawUpgradeMenu(int id, Inventory inventory, BlockPos pos) {
        super(ZoneZeroChainsaw.UPGRADE_MENU.get(), id);
        level = inventory.player.level();
        stationPos = pos;

        addSlot(new Slot(input, 0, 25, 36) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(ZoneZeroChainsaw.CHAINSAW.get());
            }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i=0;i<3;i++) {
            addSlot(new Slot(input, i+1, 70+i*18, 36) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return ChainsawUpgradeRules.isAllowedIngredient(stack);
                }
            });
        }
        addSlot(new Slot(output, 0, 150, 36) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public int getMaxStackSize() { return 1; }

            @Override public boolean mayPickup(Player player) {
                if (!ChainsawUpgradeMenu.this.stillValid(player)) return false;
                ItemStack expected = ChainsawUpgradeRules.preview(input.getItem(0), materials());
                return !expected.isEmpty()
                    && !getItem().isEmpty()
                    && getItem().getCount() == 1
                    && ItemStack.isSameItemSameComponents(getItem(), expected);
            }

            @Override public void onTake(Player player, ItemStack stack) {
                // Server runs this on the same game thread as mayPickup.
                // A preview must never be consumed on the client.
                if (!player.level().isClientSide) consumeInputs(stack);
                super.onTake(player, stack);
            }
        });

        for (int row=0;row<3;row++)
            for (int col=0;col<9;col++)
                addSlot(new Slot(inventory,col+row*9+9,8+col*18,84+row*18));
        for (int col=0;col<9;col++)
            addSlot(new Slot(inventory,col,8+col*18,142));
    }

    private ItemStack[] materials() {
        return new ItemStack[] {input.getItem(1), input.getItem(2), input.getItem(3)};
    }

    private void updateOutput() {
        if (updating) return;
        updating = true;
        try {
            if (!level.isClientSide) {
                ItemStack saw = input.getItem(0);
                if (saw.is(ZoneZeroChainsaw.CHAINSAW.get()))
                    ZoneZeroChainsaw.clearRuntimeState(saw);
            }
            output.setItem(0, ChainsawUpgradeRules.preview(input.getItem(0), materials()));
        } finally {
            updating = false;
        }
        broadcastChanges();
    }

    /**
     * Server-only atomic recipe consumption after a validated output pickup.
     * No other method removes these ingredients, so all slots are updated
     * together and the preview is invalidated before the next pickup.
     */
    private void consumeInputs(ItemStack taken) {
        ItemStack expected = ChainsawUpgradeRules.preview(input.getItem(0), materials());
        UpgradePlanLogic.Plan plan = ChainsawUpgradeRules.plan(input.getItem(0), materials());
        if (!plan.valid() || expected.isEmpty()
                || taken.getCount() != 1
                || !ItemStack.isSameItemSameComponents(taken, expected)) {
            // This should be impossible after mayPickup on the game thread.
            // Clear the phantom preview rather than consuming unrelated items.
            updating = true;
            output.setItem(0, ItemStack.EMPTY);
            updating = false;
            broadcastChanges();
            return;
        }

        int remaining = plan.dye() ? 0 : ChainsawUpgradeRules.requiredCount(plan.targetTier());
        int fuelCans = plan.dye() ? 0 : 1;
        String material = plan.dye() ? "" : ChainsawUpgradeRules.materialId(plan.targetTier());
        updating = true;
        try {
            input.setItem(0, ItemStack.EMPTY);
            for (int slot=1;slot<4;slot++) {
                ItemStack ingredient = input.getItem(slot);
                if (ingredient.isEmpty()) continue;
                if (plan.dye()) {
                    if (ingredient.getItem() instanceof DyeItem) ingredient.shrink(1);
                } else if (ingredient.is(ZoneZeroChainsaw.FUEL.get()) && fuelCans>0) {
                    ingredient.shrink(1);
                    fuelCans--;
                } else if (!material.isEmpty()
                        && BuiltInRegistries.ITEM.getKey(ingredient.getItem())
                            .toString().equals(material)) {
                    int used = Math.min(remaining, ingredient.getCount());
                    ingredient.shrink(used);
                    remaining -= used;
                }
                if (ingredient.isEmpty()) input.setItem(slot, ItemStack.EMPTY);
            }
            output.setItem(0, ItemStack.EMPTY);
        } finally {
            updating = false;
        }
        updateOutput();
    }

    @Override
    public void clicked(int slotId, int mouseButton, ClickType clickType, Player player) {
        if (slotId == 4 && clickType != ClickType.PICKUP) return;
        super.clicked(slotId, mouseButton, clickType, player);
    }

    public int nextTier() { return ChainsawUpgradeRules.resultingTier(input.getItem(0)); }
    public boolean isDye() { return ChainsawUpgradeRules.plan(input.getItem(0), materials()).dye(); }
    public boolean materialMissing() {
        int tier = nextTier();
        return !isDye() && tier >= 0 && tier <= 5
                && !ChainsawUpgradeRules.materialAvailable(tier);
    }
    public int nextFuelCapacity() {
        int tier=nextTier();
        return tier<0 || tier>5 ? 0 : Math.min(3000,(tier+1)*600);
    }
    public int nextDamage() {
        int tier=nextTier();
        return tier<0 || tier>5 ? 0 : new int[] {2,3,4,6,8,10}[tier];
    }

    @Override
    public boolean stillValid(Player player) {
        // Client menus trust server validation. The server received the
        // actual station position from the MenuProvider, not a guessed block
        // underneath the player.
        if (player.level().isClientSide) return true;
        return player.level() == level
            && level.getBlockState(stationPos).is(ZoneZeroChainsaw.UPGRADE_STATION.get())
            && player.distanceToSqr(stationPos.getX()+.5,stationPos.getY()+.5,
                                    stationPos.getZ()+.5) <= 64;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) return;
        updating = true;
        try {
            // Drop only actual input, not the preview output.
            output.setItem(0, ItemStack.EMPTY);
            for (int i=0;i<4;i++) {
                ItemStack stack=input.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    if (stack.is(ZoneZeroChainsaw.CHAINSAW.get()))
                        ZoneZeroChainsaw.clearRuntimeState(stack);
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
        } finally {
            updating = false;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 5 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original=slot.getItem();
        ItemStack result=original.copy();
        if (original.is(ZoneZeroChainsaw.CHAINSAW.get())) {
            if (!moveItemStackTo(original,0,1,false)) return ItemStack.EMPTY;
        } else if (ChainsawUpgradeRules.isAllowedIngredient(original)) {
            if (!moveItemStackTo(original,1,4,false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
