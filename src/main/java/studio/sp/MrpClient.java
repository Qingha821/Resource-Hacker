package studio.sp;

import studio.sp.io.ConfigData;
import studio.sp.io.Networking;

import lombok.Getter;
import lombok.Setter;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import io.netty.buffer.Unpooled;

public final class MrpClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER)
            throw new UnsupportedOperationException("Client only mod");

        MrpClient.init();

        ClientEntityEvents.ENTITY_LOAD.register((Entity entity, ClientLevel world) -> {
            if (entity instanceof LocalPlayer)
                MrpClient.sendRequest();
        });
    }

    public static final String MOD_ID = "MrpClient";
    public static final Logger LOG = LoggerFactory.getLogger("MrpClient");

    @Getter
    private static final ResourceLocation channelConfig = new ResourceLocation("reshack", Networking.CHANNEL_CONFIG);
    @Getter
    private static final ResourceLocation channelReset = new ResourceLocation("reshack", Networking.CHANNEL_RESET);

    @Getter
    @Setter
    private static ConfigData config = null;

    @Getter
    private static List<String> loadedUrls = new ArrayList<>();

    public static void init() {
        LOG.info("MrpClient powered by Sparkpixel");
    }

    public static void sendRequest() {
        LOG.info("Sending configuration request");
        Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(new ServerboundCustomPayloadPacket(
                channelConfig,
                new FriendlyByteBuf(Unpooled.buffer())
        ));
    }

    public static boolean configure(ClientboundCustomPayloadPacket packet) {
        if (packet.getIdentifier().equals(MrpClient.getChannelConfig())) {
            setConfig(Networking.read(packet.getData()));
            return true;
        } else if (packet.getIdentifier().equals(MrpClient.getChannelReset())) {
            MrpClient.getLoadedUrls().clear();
            return true;
        }
        return false;
    }

    public static String getKey() {
        if (config == null) {
            return null;
        }
        return config.getKey();
    }
}
