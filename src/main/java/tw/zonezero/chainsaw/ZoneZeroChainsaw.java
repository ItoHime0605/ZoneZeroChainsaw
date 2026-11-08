package tw.zonezero.chainsaw;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.minecraft.world.item.Item;
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
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredBlock<Block> FERMENTER = BLOCKS.register("fermentation_barrel", () -> new FermentationBarrelBlock(BlockBehaviour.Properties.of().strength(3.0f, 6.0f).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> REFINERY = BLOCKS.registerSimpleBlock("fuel_refinery", BlockBehaviour.Properties.of().strength(3.5f));
    public static final DeferredItem<BlockItem> FERMENTER_ITEM = ITEMS.registerSimpleBlockItem("fermentation_barrel", FERMENTER);
    public static final DeferredItem<BlockItem> REFINERY_ITEM = ITEMS.registerSimpleBlockItem("fuel_refinery", REFINERY);
    public static final DeferredItem<Item> CRUDE = ITEMS.registerSimpleItem("fermented_crude", new Item.Properties());
    public static final DeferredItem<Item> FUEL = ITEMS.register("biofuel_bottle", () -> new BiofuelCanItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> CHAINSAW = ITEMS.registerSimpleItem("chainsaw", new Item.Properties().stacksTo(1));

    public ZoneZeroChainsaw(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static int tier(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("ChainsawTier"), 0, 2);
    }
    public static int fuelCapacity(ItemStack stack) { return 1800 + 600 * tier(stack); }
    public static int damagePerHit(ItemStack stack) { return 2 + tier(stack); }
    public static int dyeColor(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("ChainsawColor"), 0, 15);
    }
    public static void setTier(ItemStack stack, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("ChainsawTier", Math.clamp(value, 0, 2)));
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
}
