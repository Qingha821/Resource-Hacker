package studio.sp;

import studio.sp.io.ConfigData;
import studio.sp.io.Networking;
import studio.sp.drm.CrypticUtils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBuf;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class Mrp implements ModInitializer {

	public static final Logger LOG = LoggerFactory.getLogger("MrpServer");
	private static byte[] clientConfigData = new byte[0];
	private static String crypticKey = "NULL_KEY";
	private static final String ENCRYPTED_PASSWORD = "IMKuVoUElAi3tAvuRiErCA==";
	private static final String AES_KEY = "987tgyuuq98479878qg987wefgoiugqk"; // 32字节

	@Override
	public void onInitialize() {
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
			LOG.info("Client detected, skip decryption method...");
			return;
		}

		File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "mrp.properties");
		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(configFile)) {
				writer.write("# Requires a 16-bytes key\n");
				writer.write("# Generator: https://acte.ltd/utils/randomkeygen\n");
				writer.write("cryptic_key=NULL_KEY\n");
				writer.write("cryptic_password=your_password\n");
			} catch (IOException e) {
				LOG.warn("Unable to generate config", e);
			}
		}

		Properties props = new Properties();
		try (FileReader reader = new FileReader(configFile)) {
			props.load(reader);
		} catch (Exception e) {
			LOG.warn("Lose access to config file, use default value", e);
		}
		crypticKey = props.getProperty("cryptic_key", "NULL_KEY");

		String password = props.getProperty("cryptic_password", "");
		String encryptedInput = "";
		try {
			javax.crypto.SecretKey sk = new javax.crypto.spec.SecretKeySpec(AES_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
			javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, sk);
			byte[] enc = cipher.doFinal(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			encryptedInput = java.util.Base64.getEncoder().encodeToString(enc);
		} catch (Exception e) {
			LOG.error("Failed to decrypt password", e);
			throw new NullPointerException("Failed to decrypt password");
		}
		if (!ENCRYPTED_PASSWORD.equals(encryptedInput)) {
			LOG.error("Wrong password detected, the server will automatically shut down!");
			LOG.warn("The mod is designed by Sparkpixel Server, please do not use it in other environment!");
			throw new NullPointerException("Wrong password detected!");
		}

		ByteBuf confBuf = Networking.write(new ConfigData(crypticKey));
		clientConfigData = new byte[confBuf.readableBytes()];
		confBuf.readBytes(clientConfigData);
		LOG.info("Configuration coded, bytes: {}", clientConfigData.length);

		ResourceLocation configChannel = new ResourceLocation(Networking.NAMESPACE, Networking.CHANNEL_CONFIG);
		ServerPlayNetworking.registerGlobalReceiver(configChannel, (server, player, handler, buf, responseSender) -> {
			LOG.info("Received configuration request and send to player : {}", player.getGameProfile().getName());
			ServerPlayNetworking.send(player, configChannel, new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(clientConfigData)));
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					Commands.literal("res-encrypt")
							.requires(source -> source.hasPermission(2))
							.then(Commands.argument("path", StringArgumentType.string())
									.executes(ctx -> {
										String path = StringArgumentType.getString(ctx, "path");
										CommandSourceStack source = ctx.getSource();
										Path file = Path.of(path);
										if (!Files.exists(file)) {
											source.sendFailure(net.minecraft.network.chat.Component.literal("File not exists"));
											return Command.SINGLE_SUCCESS;
										}
										new Thread(() -> {
											try {
												byte[] read = Files.readAllBytes(file);
												Path out = Path.of(path + ".out");
												Files.write(out, CrypticUtils.encrypt(
														Objects.requireNonNull(crypticKey, "cryptic_key"),
														read
												));
												source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Encrypted and saved to " + out), false);
											} catch (Throwable t) {
												source.sendFailure(net.minecraft.network.chat.Component.literal("Unable to encrypt: " + t));
												LOG.info("Unable to encrypt {}", path, t);
											}
										}).start();
										return Command.SINGLE_SUCCESS;
									})
							)
			);
		});
	}
}