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
        // Fuel cans are used from OFFHAND with a stationary main-hand saw.
        // Never consume canisters or increase tank values on the client.
        ItemStack can = player.getItemInHand(hand);
        ItemStack saw = player.getMainHandItem();
        boolean ready = tw.zonezero.chainsaw.logic.ChainsawSafetyRules.canRefuel(
                hand == InteractionHand.OFF_HAND,
                saw.is(ZoneZeroChainsaw.CHAINSAW.get()),
                ZoneZeroChainsaw.isEngineOn(saw),
                ZoneZeroChainsaw.fuel(saw), ZoneZeroChainsaw.fuelCapacity(saw),
                can.getCount());
        if (!ready) return InteractionResultHolder.pass(can);
        if (!level.isClientSide) {
            int before = ZoneZeroChainsaw.fuel(saw);
            int after = tw.zonezero.chainsaw.logic.ChainsawSafetyRules.refilledFuel(
                    before, ZoneZeroChainsaw.fuelCapacity(saw));
            if (after != before) {
                ZoneZeroChainsaw.setFuel(saw, after);
                if (!player.getAbilities().instabuild) can.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(can, level.isClientSide);
    }
}
