package tw.zonezero.chainsaw;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

@Mod(ZoneZeroChainsaw.MODID)
public final class ZoneZeroChainsaw {
    public static final String MODID = "zonezero_chainsaw";
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<ChainsawUpgradeMenu>> UPGRADE_MENU =
            MENUS.register("chainsaw_upgrade", () -> IMenuTypeExtension.create((id, inv, data) -> new ChainsawUpgradeMenu(id, inv)));
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT, MODID);
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SOUND_START =
            SOUNDS.register("start", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "start")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SOUND_ENGINE =
            SOUNDS.register("engine", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "engine")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SOUND_CUT =
            SOUNDS.register("cut", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "cut")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SOUND_STOP =
            SOUNDS.register("stop", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "stop")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SOUND_CONTACT =
            SOUNDS.register("contact", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "contact")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SOUND_STALL =
            SOUNDS.register("stall", () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "stall")));
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredBlock<Block> FERMENTER = BLOCKS.register("fermentation_barrel", () -> new FermentationBarrelBlock(BlockBehaviour.Properties.of().strength(3.0f, 6.0f).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> UPGRADE_STATION = BLOCKS.register("chainsaw_upgrade_station", () -> new ChainsawUpgradeStationBlock(BlockBehaviour.Properties.of().strength(4.0f, 8.0f).sound(SoundType.METAL)));
    public static final DeferredBlock<Block> REFINERY = BLOCKS.registerSimpleBlock("fuel_refinery", BlockBehaviour.Properties.of().strength(3.5f));
    public static final DeferredItem<BlockItem> FERMENTER_ITEM = ITEMS.registerSimpleBlockItem("fermentation_barrel", FERMENTER);
    public static final DeferredItem<BlockItem> UPGRADE_STATION_ITEM = ITEMS.registerSimpleBlockItem("chainsaw_upgrade_station", UPGRADE_STATION);
    public static final DeferredItem<BlockItem> REFINERY_ITEM = ITEMS.registerSimpleBlockItem("fuel_refinery", REFINERY);
    public static final DeferredItem<Item> CRUDE = ITEMS.registerSimpleItem("fermented_crude", new Item.Properties());
    public static final DeferredItem<Item> FUEL = ITEMS.register("biofuel_bottle", () -> new BiofuelCanItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CHAINSAW = ITEMS.register("chainsaw", () -> new ChainsawItem(new Item.Properties().stacksTo(1)));

    public ZoneZeroChainsaw(IEventBus bus) {
        SOUNDS.register(bus);
        BLOCKS.register(bus);
        MENUS.register(bus);
        ITEMS.register(bus);
        ChainsawCreativeTabs.TABS.register(bus);
        bus.addListener(ChainsawControlPacket::register);
    }

    public static int tier(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("ChainsawTier"), 0, 5);
    }
    public static int fuelCapacity(ItemStack stack) { return Math.min(3000, 600 * (tier(stack) + 1)); }
    public static int damagePerHit(ItemStack stack) { return new int[] {2, 3, 4, 6, 8, 10}[tier(stack)]; }
    public static int dyeColor(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("ChainsawColor"), 0, 15);
    }
    public static void setTier(ItemStack stack, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ChainsawTier", Math.clamp(value, 0, 5)));
    }
    public static void setDyeColor(ItemStack stack, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ChainsawColor", Math.clamp(value, 0, 15)));
    }

    public static int fuel(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return Math.clamp(data.copyTag().getInt("ChainsawFuel"), 0, fuelCapacity(stack));
    }
    public static void setFuel(ItemStack stack, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt("ChainsawFuel", Math.clamp(value, 0, fuelCapacity(stack))));
    }
    public static int refill(ItemStack stack) {
        int added = Math.min(600, fuelCapacity(stack) - fuel(stack));
        if (added > 0) setFuel(stack, fuel(stack) + added);
        return added;
    }
    /** Server-synchronized held-cut state. Kept separate from the engine state. */
    public static boolean isCutting(ItemStack stack) {
        return isEngineOn(stack) && fuel(stack) > 0
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                        .copyTag().getBoolean("ChainsawCutting");
    }

    public static void setCutting(ItemStack stack, boolean cutting) {
        boolean current = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean("ChainsawCutting");
        if (current != cutting) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putBoolean("ChainsawCutting", cutting));
        }
    }

    public static boolean isEngineOn(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getBoolean("ChainsawEngineOn");
    }

    public static void setEngineOn(ItemStack stack, boolean on) {
        if (!on) setCutting(stack, false);
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putBoolean("ChainsawEngineOn", on));
    }

    /** Game-time based two-second motor coast after manual or automatic shutdown. */
    public static void setCoastUntil(ItemStack stack, long worldTick) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putLong("ChainsawCoastUntil", worldTick));
    }

    public static long coastUntil(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getLong("ChainsawCoastUntil");
    }
}
