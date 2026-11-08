package tw.zonezero.chainsaw;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Strict server-only input state and 4-tick cutting cadence.
 * A lost mouse-release packet expires within ten ticks, without changing the engine state.
 */
@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID)
public final class ChainsawGameplay {
    private static final class InputState {
        long lastHeldTick;
        int heldTicks;
        ItemStack lastSaw = ItemStack.EMPTY;
    }
    private static final Map<UUID, InputState> INPUTS = new HashMap<>();

    private ChainsawGameplay() {}

    public static void onControl(ServerPlayer player, byte action) {
        if (action < 1 || action > 3) return;
        ItemStack saw = player.getMainHandItem();
        if (!saw.is(ZoneZeroChainsaw.CHAINSAW.get())) {
            INPUTS.remove(player.getUUID());
            return;
        }
        InputState input = INPUTS.computeIfAbsent(player.getUUID(), id -> new InputState());
        input.lastSaw = saw;
        if (action == 1) {
            boolean next = !ZoneZeroChainsaw.isEngineOn(saw);
            if (next && ZoneZeroChainsaw.fuel(saw) <= 0) {
                player.displayClientMessage(Component.translatable("message.zonezero_chainsaw.no_fuel")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            ZoneZeroChainsaw.setEngineOn(saw, next);
            input.heldTicks = 0;
            input.lastHeldTick = -1000;
            player.displayClientMessage(Component.translatable(next
                    ? "message.zonezero_chainsaw.engine_on" : "message.zonezero_chainsaw.engine_off"), true);
        } else if (action == 2) {
            input.lastHeldTick = player.level().getGameTime();
        } else {
            input.lastHeldTick = -1000;
            input.heldTicks = 0;
        }
    }

    @SubscribeEvent
    public static void serverTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        InputState input = INPUTS.get(id);
        if (input == null) return;
        ItemStack saw = player.getMainHandItem();
        if (!player.isAlive() || !saw.is(ZoneZeroChainsaw.CHAINSAW.get()) || saw != input.lastSaw) {
            if (!input.lastSaw.isEmpty() && input.lastSaw.is(ZoneZeroChainsaw.CHAINSAW.get())) {
                ZoneZeroChainsaw.setEngineOn(input.lastSaw, false);
            }
            INPUTS.remove(id);
            return;
        }
        if (!ZoneZeroChainsaw.isEngineOn(saw)) {
            input.heldTicks = 0;
            return;
        }
        if (ZoneZeroChainsaw.fuel(saw) <= 0) {
            ZoneZeroChainsaw.setEngineOn(saw, false);
            input.heldTicks = 0;
            return;
        }
        long tick = player.level().getGameTime();
        if (tick - input.lastHeldTick > 10) {
            input.heldTicks = 0;
            return;
        }
        if (++input.heldTicks < 4) return;
        input.heldTicks = 0;
        ZoneZeroChainsaw.setFuel(saw, ZoneZeroChainsaw.fuel(saw) - 1);
        net.minecraft.world.item.component.CustomData.update(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                saw, tag -> tag.putLong("ChainsawCuttingAt", tick));
        LivingEntity target = findTarget(player);
        if (target != null) {
            float damage = ZoneZeroChainsaw.damagePerHit(saw);
            if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                target.knockback(0.12, player.getX() - target.getX(), player.getZ() - target.getZ());
            }
        }
        if (ZoneZeroChainsaw.fuel(saw) == 0) {
            ZoneZeroChainsaw.setEngineOn(saw, false);
            player.displayClientMessage(Component.translatable("message.zonezero_chainsaw.empty"), true);
        }
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 origin = player.getEyePosition();
        Vec3 goal = origin.add(player.getLookAngle().scale(4.25));
        BlockHitResult block = player.level().clip(new ClipContext(
                origin, goal, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) goal = block.getLocation();
        double best = origin.distanceToSqr(goal);
        LivingEntity picked = null;
        for (LivingEntity entity : player.level().getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(4.75),
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            AABB box = entity.getBoundingBox().inflate(0.22);
            Optional<Vec3> contact = box.clip(origin, goal);
            if (contact.isEmpty()) continue;
            double distance = origin.distanceToSqr(contact.get());
            if (distance < best && player.hasLineOfSight(entity)) {
                picked = entity;
                best = distance;
            }
        }
        return picked;
    }
}
