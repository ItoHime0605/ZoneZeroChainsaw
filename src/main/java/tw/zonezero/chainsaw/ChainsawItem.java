package tw.zonezero.chainsaw;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Self-contained information panel on every chainsaw, including existing dyed stacks. */
public final class ChainsawItem extends Item {
    public ChainsawItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int fuel = ZoneZeroChainsaw.fuel(stack);
        int capacity = ZoneZeroChainsaw.fuelCapacity(stack);
        int filled = Math.clamp((int) Math.ceil(10.0 * fuel / Math.max(1, capacity)), 0, 10);
        tooltip.add(Component.translatable("tooltip.zonezero_chainsaw.description").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.zonezero_chainsaw.level", ZoneZeroChainsaw.tier(stack)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.zonezero_chainsaw.damage", ZoneZeroChainsaw.damagePerHit(stack)).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.zonezero_chainsaw.fuel", fuel, capacity).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("[" + "|".repeat(filled) + ".".repeat(10 - filled) + "]")
                .withStyle(fuel > capacity / 5 ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.zonezero_chainsaw.engine",
                ZoneZeroChainsaw.isEngineOn(stack)
                    ? Component.translatable("tooltip.zonezero_chainsaw.engine_on")
                    : Component.translatable("tooltip.zonezero_chainsaw.engine_off"))
                .withStyle(ZoneZeroChainsaw.isEngineOn(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.zonezero_chainsaw.controls").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.zonezero_chainsaw.refuel").withStyle(ChatFormatting.GRAY));
    }
}
