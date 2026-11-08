package tw.zonezero.chainsaw;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ZoneZeroChainsaw.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ChainsawClientKeys {
    public static final KeyMapping TOGGLE_ENGINE = new KeyMapping(
            "key.zonezero_chainsaw.toggle_engine", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.zonezero_chainsaw");

    private ChainsawClientKeys() {}

    @SubscribeEvent
    public static void onKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_ENGINE);
    }
}
