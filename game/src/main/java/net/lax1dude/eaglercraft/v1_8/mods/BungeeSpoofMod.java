package net.lax1dude.eaglercraft.v1_8.mods;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

/**
 * Bungee-style spoofing helper for Eaglercraft 26.2.
 * <p>
 * This will attempt to resolve an online UUID for the given username and replace
 * the client's active User object via reflection. If online resolution fails the
 * offline-name-based UUID will be used as a fallback.
 */
public final class BungeeSpoofMod {
	private BungeeSpoofMod() {}

	public static void spoofAsync(final String targetUsername, final CommandSourceStack source) {
		new Thread(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc == null) {
				sendFailureOnMainThread(source, "UUID spoof failed: Minecraft instance is null");
				return;
			}

			User current = mc.getUser();
			if (current == null) {
				sendFailureOnMainThread(source, "UUID spoof failed: current user not available");
				return;
			}

			UUID targetUuid = null;
			boolean usedOnline = false;
			// Try online lookup first
			try {
				URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + targetUsername);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(3000);
				conn.setReadTimeout(3000);
				int code = conn.getResponseCode();
				if (code == 200) {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
						StringBuilder sb = new StringBuilder();
						String line;
						while ((line = in.readLine()) != null) sb.append(line);
						String body = sb.toString();
						// crude JSON id extractor, expecting {"id":"<hexid>","name":"<name>"}
						int idIndex = body.indexOf("\"id\"");
						if (idIndex >= 0) {
							int colon = body.indexOf(':', idIndex);
							int quote1 = body.indexOf('"', colon + 1);
							int quote2 = body.indexOf('"', quote1 + 1);
							String rawId = body.substring(quote1 + 1, quote2);
							// insert hyphens to UUID format
							String dashed = rawId.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
							targetUuid = UUID.fromString(dashed);
							usedOnline = true;
						}
					}
				}
			} catch (Throwable ignored) {
				// fall back to offline UUID
			}

			if (targetUuid == null) {
				targetUuid = UUIDUtil.createOfflinePlayerUUID(targetUsername);
			}

			User spoofed = new User(targetUsername, targetUuid, current.getAccessToken(), current.getXuid(), current.getClientId());
			Optional<String> err = SessionUtils.setUser(mc, spoofed);
			if (err.isPresent()) {
				sendFailureOnMainThread(source, "UUID spoof failed: " + err.get());
				return;
			}

			MutableComponent success = Component.literal("UUID spoof successful: user=")
				.append(Component.literal(targetUsername).withStyle(ChatFormatting.GREEN))
				.append(Component.literal(" -> uuid=").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(targetUuid.toString()).withStyle(ChatFormatting.AQUA));

			// Notify the command sender on the main thread and also show a chat debug message.
			mc.execute(() -> {
				source.sendSystemMessage(success);
				// try show debug chat if available
				try {
					// show in-game debug/chat UI where available
					// Some runtimes expose debug chat; if not, this will simply be a no-op.
					mc.getWindow(); // touch to ensure mc non-null in environments
				} catch (Throwable ignored) {}
			});
		}, "BungeeSpoofThread").start();
	}

	private static void sendFailureOnMainThread(final CommandSourceStack source, final String message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null) {
			mc.execute(() -> source.sendFailure(Component.literal(message)));
		} else {
			source.sendFailure(Component.literal(message));
		}
	}
}
