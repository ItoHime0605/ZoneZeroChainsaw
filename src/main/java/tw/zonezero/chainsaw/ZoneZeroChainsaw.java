package tw.zonezero.chainsaw;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

@Mod(ZoneZeroChainsaw.MODID)
public final class ZoneZeroChainsaw {
    public static final String MODID = "zonezero_chainsaw";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredBlock<Block> FERMENTER = BLOCKS.registerSimpleBlock("fermentation_barrel", BlockBehaviour.Properties.of().strength(2.5f));
    public static final DeferredBlock<Block> REFINERY = BLOCKS.registerSimpleBlock("fuel_refinery", BlockBehaviour.Properties.of().strength(3.5f));
    public static final DeferredItem<Item> FERMENTER_ITEM = ITEMS.registerSimpleBlockItem("fermentation_barrel", FERMENTER);
    public static final DeferredItem<Item> REFINERY_ITEM = ITEMS.registerSimpleBlockItem("fuel_refinery", REFINERY);
    public static final DeferredItem<Item> CRUDE = ITEMS.registerSimpleItem("fermented_crude", new Item.Properties());
    public static final DeferredItem<Item> FUEL = ITEMS.registerSimpleItem("biofuel_bottle", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> CHAINSAW = ITEMS.registerSimpleItem("chainsaw", new Item.Properties().stacksTo(1));

    public ZoneZeroChainsaw(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    // The production input/packet and refilling flow is not yet connected.
    public static int fuel(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return Math.clamp(data.copyTag().getInt("ChainsawFuel"), 0, 1800);
    }
    public static void setFuel(ItemStack stack, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt("ChainsawFuel", Math.clamp(value, 0, 1800)));
    }
    public static int refill(ItemStack stack) {
        int added = Math.min(600, 1800 - fuel(stack));
        if (added > 0) setFuel(stack, fuel(stack) + added);
        return added;
    }
}
