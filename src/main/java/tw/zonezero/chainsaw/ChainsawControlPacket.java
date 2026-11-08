package tw.zonezero.chainsaw;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Tiny C2S control command. No client-supplied damage, fuel amount, or target.
 * 1 = toggle engine; 2 = left mouse held; 3 = released.
 */
public record ChainsawControlPacket(byte action) implements CustomPacketPayload {
    public static final Type<ChainsawControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZoneZeroChainsaw.MODID, "control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainsawControlPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> buf.writeByte(packet.action()),
                    buf -> new ChainsawControlPacket(buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (packet, context) -> {
            // NeoForge 1.21.1 defaults to MAIN, but using enqueueWork makes
            // inventory, fuel, targets and sounds safe even when another
            // registrar explicitly uses a networking thread.
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.isAlive() && !player.isSpectator()) {
                    ChainsawGameplay.onControl(player, packet.action());
                }
            });
        });
    }
}
