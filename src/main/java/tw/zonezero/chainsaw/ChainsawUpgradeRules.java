package tw.zonezero.chainsaw;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import tw.zonezero.chainsaw.logic.UpgradePlanLogic;

/**
 * One authoritative result per valid 3-slot transaction.
 *
 * External material IDs are checked against the actual live item registry.
 * Alternative names are accepted ONLY when that exact item was registered:
 * never substitute arbitrary ingots or the Minecraft iron block.
 */
public final class ChainsawUpgradeRules {
    private ChainsawUpgradeRules() {}

    private static final String[][] MATERIAL_IDS = {
        {"minecraft:iron_block"},
        {"minecraft:iron_block"},
        {"zombie_extreme:titanbar", "zombie_extreme:titan_ingot"},
        {"zombie_extreme:metalblock", "zombie_extreme:metal_block"},
        {"minecraft:diamond_block"},
        {"minecraft:netherite_block"}
    };

    public static int requiredCount(int resultingTier) {
        return UpgradePlanLogic.requiredCount(resultingTier);
    }

    /** Empty string means required external material could not be resolved. */
    public static String materialId(int resultingTier) {
        if (resultingTier < 0 || resultingTier >= MATERIAL_IDS.length) return "";
        for (String id : MATERIAL_IDS[resultingTier]) {
            ResourceLocation key = ResourceLocation.parse(id);
            if (BuiltInRegistries.ITEM.containsKey(key)) return id;
        }
        return "";
    }

    public static boolean materialAvailable(int targetTier) {
        return !materialId(targetTier).isEmpty();
    }

    public static boolean isDyeOperation(ItemStack[] ingredients) {
        if (ingredients == null || ingredients.length != 3) return false;
        int dyes = 0;
        for (ItemStack stack : ingredients) {
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof DyeItem) || stack.getCount() != 1) return false;
            dyes++;
        }
        return dyes == 1;
    }

    public static int resultingTier(ItemStack saw) {
        if (!saw.is(ZoneZeroChainsaw.CHAINSAW.get())) return -1;
        CustomData data = saw.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        boolean initialized = data.copyTag().getBoolean("ChainsawInitialized");
        return UpgradePlanLogic.targetTier(initialized, ZoneZeroChainsaw.tier(saw));
    }

    /**
     * Returns an immutable, all-or-nothing plan. Material, fuel and dye are
     * mutually exclusive by slot and exact counts across all 3 slots.
     */
    public static UpgradePlanLogic.Plan plan(ItemStack saw, ItemStack[] ingredients) {
        if (!saw.is(ZoneZeroChainsaw.CHAINSAW.get()) ||
                ingredients == null || ingredients.length != 3)
            return UpgradePlanLogic.Plan.invalid();
        int[] kinds = new int[3], counts = new int[3], colors = new int[3];
        int tier = resultingTier(saw);
        String material = materialId(tier);
        for (int i=0;i<3;i++) {
            ItemStack stack = ingredients[i];
            if (stack.isEmpty()) {
                kinds[i] = UpgradePlanLogic.EMPTY;
                continue;
            }
            counts[i] = stack.getCount();
            if (stack.getItem() instanceof DyeItem dye) {
                kinds[i] = UpgradePlanLogic.DYE;
                colors[i] = dye.getDyeColor().getId();
            } else if (stack.is(ZoneZeroChainsaw.FUEL.get())) {
                kinds[i] = UpgradePlanLogic.FUEL;
            } else if (!material.isEmpty() &&
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(material)) {
                kinds[i] = UpgradePlanLogic.MATERIAL;
            } else {
                kinds[i] = UpgradePlanLogic.INVALID;
            }
        }
        return UpgradePlanLogic.match(tier, ZoneZeroChainsaw.dyeColor(saw),
                kinds, counts, colors);
    }

    /** The preview is a COPY, never the original input saw stack. */
    public static ItemStack preview(ItemStack saw, ItemStack[] ingredients) {
        UpgradePlanLogic.Plan plan = plan(saw, ingredients);
        if (!plan.valid()) return ItemStack.EMPTY;
        ItemStack result = saw.copyWithCount(1);
        ZoneZeroChainsaw.clearRuntimeState(result);
        if (plan.dye()) {
            ZoneZeroChainsaw.setDyeColor(result, plan.dyeColor());
        } else {
            ZoneZeroChainsaw.setTier(result, plan.targetTier());
            CustomData.update(DataComponents.CUSTOM_DATA, result,
                    tag -> tag.putBoolean("ChainsawInitialized", true));
        }
        return result;
    }
}
