package tw.zonezero.chainsaw;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Use a fuel can in one hand and the chainsaw in the other to refill.
 * A can contains 600 cutting units; refilling requires room for a full 600-unit can to avoid wasting fuel.
 */
public final class BiofuelCanItem extends Item {
    public BiofuelCanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack can = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!other.is(ZoneZeroChainsaw.CHAINSAW.get()) || ZoneZeroChainsaw.fuel(other) > 1200) {
            return InteractionResultHolder.pass(can);
        }
        if (!level.isClientSide) {
            int added = ZoneZeroChainsaw.refill(other);
            if (added > 0 && !player.getAbilities().instabuild) {
                can.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(can, level.isClientSide);
    }
}
