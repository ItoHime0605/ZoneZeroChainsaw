package tw.zonezero.chainsaw;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import tw.zonezero.chainsaw.logic.SustainedCutLogic;
import tw.zonezero.chainsaw.logic.ChainsawAudioTiming;
import tw.zonezero.chainsaw.logic.ChainsawSafetyRules;
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
        long lastHeldTick = -1000;
        int heldTicks;
        long startedAt;
        long lastToggleAt = -1000;
        long lastCutAudioAt = ChainsawAudioTiming.NONE;
        long lastContactAudioAt = ChainsawAudioTiming.NONE;
        ItemStack lastSaw = ItemStack.EMPTY;
    }
    private static final Map<UUID, InputState> INPUTS = new HashMap<>();

    private ChainsawGameplay() {}

    public static void onControl(ServerPlayer player, byte action) {
        if (action < 1 || action > 3) return;
        if (!player.isAlive() || player.isSpectator()) return;
        ItemStack saw = player.getMainHandItem();
        if (!saw.is(ZoneZeroChainsaw.CHAINSAW.get())) {
            InputState previous = INPUTS.remove(player.getUUID());
            if (previous != null && previous.lastSaw.is(ZoneZeroChainsaw.CHAINSAW.get()))
                ZoneZeroChainsaw.clearRuntimeState(previous.lastSaw);
            return;
        }
        InputState input = INPUTS.computeIfAbsent(player.getUUID(), id -> new InputState());
        if (input.lastSaw != saw) {
            // A restored saw from a chest/login/another slot must never be
            // considered running before a fresh successful R command.
            // This also closes an input-packet-before-next-tick race.
            if (!input.lastSaw.isEmpty())
                ZoneZeroChainsaw.clearRuntimeState(input.lastSaw);
            ZoneZeroChainsaw.clearRuntimeState(saw);
            resetCutInput(input);
            input.lastToggleAt = -1000;
        }
        input.lastSaw = saw;
        // Repeated held packets never reschedule or duplicate a sound loop.
        if (action == 1) {
            long tick = player.level().getGameTime();
            if (!ChainsawSafetyRules.toggleAllowed(tick, input.lastToggleAt)) return;
            boolean next = !ZoneZeroChainsaw.isEngineOn(saw);
            // Both hands are mandatory. A fuel can in the offhand can be used
            // for refilling only while the engine is stopped.
            if (next && !player.getOffhandItem().isEmpty()) {
                player.displayClientMessage(
                        Component.translatable("message.zonezero_chainsaw.offhand_required")
                                .withStyle(ChatFormatting.RED), true);
                resetCutInput(input);
                return;
            }
            if (next && ZoneZeroChainsaw.fuel(saw) <= 0) {
                player.displayClientMessage(Component.translatable("message.zonezero_chainsaw.no_fuel")
                        .withStyle(ChatFormatting.RED), true);
                resetCutInput(input);
                return;
            }
            if (next && !ChainsawSafetyRules.canStart(
                    player.getOffhandItem().isEmpty(), ZoneZeroChainsaw.fuel(saw),
                    player.isAlive(), player.isSpectator())) return;
            input.lastToggleAt = tick;
            ZoneZeroChainsaw.setEngineOn(saw, next);
            ZoneZeroChainsaw.setCutting(saw, false);
            input.lastCutAudioAt = ChainsawAudioTiming.NONE;
            input.lastContactAudioAt = ChainsawAudioTiming.NONE;
            if (next) {
                ZoneZeroChainsaw.setCoastUntil(saw, 0);
                input.startedAt = player.level().getGameTime();
            } else {
                ZoneZeroChainsaw.setCoastUntil(saw, player.level().getGameTime() + 40);
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    next ? ZoneZeroChainsaw.SOUND_START.get() : ZoneZeroChainsaw.SOUND_STOP.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.74f, 1.0f);
            resetCutInput(input);
            player.displayClientMessage(Component.translatable(next
                    ? "message.zonezero_chainsaw.engine_on" : "message.zonezero_chainsaw.engine_off"), true);
        } else if (action == 2) {
            if (SustainedCutLogic.canCut(ZoneZeroChainsaw.isEngineOn(saw),
                    player.getOffhandItem().isEmpty(), ZoneZeroChainsaw.fuel(saw))) {
                input.lastHeldTick = player.level().getGameTime();
                // This replicates a stable pose to other clients only once
                // per state transition, not with each 4-tick damage pulse.
                ZoneZeroChainsaw.setCutting(saw, true);
            } else {
                input.heldTicks = 0;
                input.lastHeldTick = -1000;
                input.lastCutAudioAt = ChainsawAudioTiming.NONE;
                input.lastContactAudioAt = ChainsawAudioTiming.NONE;
                ZoneZeroChainsaw.setCutting(saw, false);
            }
        } else {
            input.lastHeldTick = -1000;
            input.heldTicks = 0;
            input.lastCutAudioAt = ChainsawAudioTiming.NONE;
            input.lastContactAudioAt = ChainsawAudioTiming.NONE;
            ZoneZeroChainsaw.setCutting(saw, false);
        }
    }

    @SubscribeEvent
    public static void serverTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        InputState input = INPUTS.get(id);
        ItemStack saw = player.getMainHandItem();
        if (input == null) {
            // A saw loaded from storage or a previous game session never
            // gains free motor time, cutting, damage or fuel by accident.
            if (saw.is(ZoneZeroChainsaw.CHAINSAW.get())
                    && (ZoneZeroChainsaw.isEngineOn(saw) || ZoneZeroChainsaw.isCutting(saw)))
                ZoneZeroChainsaw.clearRuntimeState(saw);
            return;
        }
        if (ChainsawSafetyRules.mustStop(saw == input.lastSaw, player.isAlive(),
                player.isSpectator(), true)
                || !saw.is(ZoneZeroChainsaw.CHAINSAW.get())) {
            if (!input.lastSaw.isEmpty() && input.lastSaw.is(ZoneZeroChainsaw.CHAINSAW.get())) {
                boolean wasOn = ZoneZeroChainsaw.isEngineOn(input.lastSaw);
                ZoneZeroChainsaw.clearRuntimeState(input.lastSaw);
                if (wasOn && player.isAlive()) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ZoneZeroChainsaw.SOUND_STOP.get(),
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.60f, 1.0f);
                }
            }
            INPUTS.remove(id);
            return;
        }
        // The left hand must remain empty throughout the entire session, not
        // merely when the engine starts. Never eject or move inventory items.
        if (!player.getOffhandItem().isEmpty()) {
            if (ZoneZeroChainsaw.isEngineOn(saw)) {
                ZoneZeroChainsaw.setEngineOn(saw, false);
                ZoneZeroChainsaw.setCoastUntil(saw, player.level().getGameTime() + 40);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ZoneZeroChainsaw.SOUND_STOP.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1.0f);
                player.displayClientMessage(
                        Component.translatable("message.zonezero_chainsaw.offhand_interrupt")
                                .withStyle(ChatFormatting.RED), true);
            }
            input.heldTicks = 0;
            input.lastHeldTick = -1000;
            input.lastCutAudioAt = ChainsawAudioTiming.NONE;
            input.lastContactAudioAt = ChainsawAudioTiming.NONE;
            return;
        }
        if (!ZoneZeroChainsaw.isEngineOn(saw)) {
            resetCutInput(input);
            ZoneZeroChainsaw.setCutting(saw, false);
            return;
        }
        if (ZoneZeroChainsaw.fuel(saw) <= 0) {
            ZoneZeroChainsaw.setEngineOn(saw, false);
            ZoneZeroChainsaw.setCoastUntil(saw, player.level().getGameTime() + 40);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ZoneZeroChainsaw.SOUND_STALL.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.66f, 1.0f);
            input.heldTicks = 0;
            input.lastCutAudioAt = ChainsawAudioTiming.NONE;
            input.lastContactAudioAt = ChainsawAudioTiming.NONE;
            return;
        }
        long tick = player.level().getGameTime();
        // Start clip lasts exactly 18 ticks; periodic motor clips then start
        // at 18, 30, 42... ticks. All sounds are spatial, server-authoritative
        // and use vanilla PLAYERS-category distance attenuation.
        if (ChainsawAudioTiming.engineClip(tick, input.startedAt)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ZoneZeroChainsaw.SOUND_ENGINE.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.40f, 1.0f);
        }
        if (!SustainedCutLogic.shouldCut(tick, input.lastHeldTick,
                ZoneZeroChainsaw.isEngineOn(saw), player.getOffhandItem().isEmpty(),
                ZoneZeroChainsaw.fuel(saw))) {
            input.heldTicks = 0;
            input.lastCutAudioAt = ChainsawAudioTiming.NONE;
            input.lastContactAudioAt = ChainsawAudioTiming.NONE;
            ZoneZeroChainsaw.setCutting(saw, false);
            return;
        }
        // Cut audio has its OWN clock. It begins once on a held attack and
        // repeats every eight ticks; it must not depend on 4-tick hit cadence.
        if (ChainsawAudioTiming.clipDue(tick, input.lastCutAudioAt,
                ChainsawAudioTiming.CUT_INTERVAL)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ZoneZeroChainsaw.SOUND_CUT.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.47f, 1.0f);
            input.lastCutAudioAt = tick;
        }
        // Hold the continuous pose indefinitely while input is refreshed.
        // Only fuel/damage pulse every four ticks; never trigger a new swing.
        ++input.heldTicks;
        if (!SustainedCutLogic.consumeUnit(input.heldTicks)) return;
        input.heldTicks = 0;
        ZoneZeroChainsaw.setFuel(saw, ZoneZeroChainsaw.fuel(saw) - 1);
        net.minecraft.world.item.component.CustomData.update(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                saw, tag -> tag.putLong("ChainsawCuttingAt", tick));
        LivingEntity target = findTarget(player);
        if (target != null) {
            float damage = ZoneZeroChainsaw.damagePerHit(saw);
            // Vanilla hurt immunity is longer than our 4-tick cutting interval.
            // Clear the short immunity window only for an authorized blade contact.
            // Otherwise repeated holding would silently hit once every ~10 ticks.
            if (target.invulnerableTime > 0 && !target.isInvulnerable()) {
                target.invulnerableTime = 0;
            }
            if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                target.knockback(0.12, player.getX() - target.getX(), player.getZ() - target.getZ());
                if (ChainsawAudioTiming.clipDue(tick, input.lastContactAudioAt,
                        ChainsawAudioTiming.CONTACT_COOLDOWN)) {
                    // Sound at the actual contact target, NOT the attacker's
                    // ears. This remains one short hit clip per six ticks.
                    player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                            ZoneZeroChainsaw.SOUND_CONTACT.get(),
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.49f, 1.0f);
                    input.lastContactAudioAt = tick;
                }
            }
        }
        if (ZoneZeroChainsaw.fuel(saw) == 0) {
            ZoneZeroChainsaw.setEngineOn(saw, false);
            ZoneZeroChainsaw.setCoastUntil(saw, tick + 40);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ZoneZeroChainsaw.SOUND_STALL.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.66f, 1.0f);
            input.lastCutAudioAt = ChainsawAudioTiming.NONE;
            input.lastContactAudioAt = ChainsawAudioTiming.NONE;
            player.displayClientMessage(Component.translatable("message.zonezero_chainsaw.empty"), true);
        }
    }

    /** Only runtime/animation state is cleared; all saved saw properties remain. */
    private static void resetCutInput(InputState input) {
        input.heldTicks = 0;
        input.lastHeldTick = -1000;
        input.lastCutAudioAt = ChainsawAudioTiming.NONE;
        input.lastContactAudioAt = ChainsawAudioTiming.NONE;
    }

    private static void clearInventoryRuntime(Player player) {
        for (ItemStack stack : player.getInventory().items)
            if (stack.is(ZoneZeroChainsaw.CHAINSAW.get()))
                ZoneZeroChainsaw.clearRuntimeState(stack);
        for (ItemStack stack : player.getInventory().offhand)
            if (stack.is(ZoneZeroChainsaw.CHAINSAW.get()))
                ZoneZeroChainsaw.clearRuntimeState(stack);
        for (ItemStack stack : player.getInventory().armor)
            if (stack.is(ZoneZeroChainsaw.CHAINSAW.get()))
                ZoneZeroChainsaw.clearRuntimeState(stack);
    }

    @SubscribeEvent
    public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        INPUTS.remove(player.getUUID());
        clearInventoryRuntime(player);
    }

    @SubscribeEvent
    public static void changedDimension(
            net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // A teleported player should never carry active remote sound/animation
        // state between dimensions or into Personal Shelter instances.
        InputState state = INPUTS.remove(player.getUUID());
        if (state != null && state.lastSaw.is(ZoneZeroChainsaw.CHAINSAW.get()))
            ZoneZeroChainsaw.clearRuntimeState(state.lastSaw);
        clearInventoryRuntime(player);
    }

    @SubscribeEvent
    public static void respawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        INPUTS.remove(player.getUUID());
        clearInventoryRuntime(player);
    }

    @SubscribeEvent
    public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        InputState state = INPUTS.remove(player.getUUID());
        if (state != null && state.lastSaw.is(ZoneZeroChainsaw.CHAINSAW.get()))
            ZoneZeroChainsaw.clearRuntimeState(state.lastSaw);
        clearInventoryRuntime(player);
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
