package tw.zonezero.chainsaw;

import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/** Pure transaction validation for the upgrade station's three material slots. */
public final class ChainsawUpgradeRules {
    private ChainsawUpgradeRules() {}
    private static final int[] COUNTS = {3, 6, 27, 3, 3, 3};
    private static final String[] MATERIALS = {
        "minecraft:iron_block",
        "minecraft:iron_block",
        "zombie_extreme:titan_ingot",
        "zombie_extreme:metal_block",
        "minecraft:diamond_block",
        "minecraft:netherite_block"
    };

    public static int requiredCount(int resultingTier) {
        return resultingTier >= 0 && resultingTier < COUNTS.length ? COUNTS[resultingTier] : 0;
    }

    public static String materialId(int resultingTier) {
        return resultingTier >= 0 && resultingTier < MATERIALS.length ? MATERIALS[resultingTier] : "";
    }

    public static boolean isDyeOperation(ItemStack[] ingredients) {
        int dyes = 0;
        for (ItemStack item : ingredients) {
            if (item.isEmpty()) continue;
            if (!(item.getItem() instanceof DyeItem) || item.getCount() != 1) return false;
            dyes++;
        }
        return dyes == 1;
    }

    public static int resultingTier(ItemStack saw) {
        if (!saw.is(ZoneZeroChainsaw.CHAINSAW.get())) return -1;
        boolean initialized = saw.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBoolean("ChainsawInitialized");
        return initialized ? ZoneZeroChainsaw.tier(saw) + 1 : 0;
    }

    public static ItemStack preview(ItemStack saw, ItemStack[] ingredients) {
        if (!saw.is(ZoneZeroChainsaw.CHAINSAW.get()) || ingredients.length != 3) return ItemStack.EMPTY;
        if (isDyeOperation(ingredients)) return dyePreview(saw, ingredients);
        int target = resultingTier(saw);
        if (target < 0 || target > 5) return ItemStack.EMPTY;
        int fuelCans = 0;
        int metals = 0;
        boolean valid = true;
        for (ItemStack item : ingredients) {
            if (item.isEmpty()) continue;
            if (item.is(ZoneZeroChainsaw.FUEL.get())) fuelCans += item.getCount();
            else if (BuiltInRegistries.ITEM.getKey(item.getItem()).toString().equals(MATERIALS[target])) metals += item.getCount();
            else valid = false;
        }
        if (!valid || fuelCans != 1 || metals != COUNTS[target]) return ItemStack.EMPTY;
        ItemStack result = saw.copyWithCount(1);
        ZoneZeroChainsaw.setTier(result, target);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                result, tag -> tag.putBoolean("ChainsawInitialized", true));
        return result;
    }

    private static ItemStack dyePreview(ItemStack saw, ItemStack[] ingredients) {
        DyeItem dye = null;
        int count = 0;
        for (ItemStack item : ingredients) {
            if (item.isEmpty()) continue;
            if (!(item.getItem() instanceof DyeItem d) || item.getCount() != 1 || dye != null) return ItemStack.EMPTY;
            dye = d;
            count++;
        }
        if (count != 1 || dye == null) return ItemStack.EMPTY;
        ItemStack result = saw.copyWithCount(1);
        ZoneZeroChainsaw.setDyeColor(result, dye.getDyeColor().getId());
        return result;
    }
}
