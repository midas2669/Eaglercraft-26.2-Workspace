package net.lax1dude.eaglercraft.v1_8.mods;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

/**
 * Utilities for manipulating the running Minecraft client session via reflection.
 */
public final class SessionUtils {
	private SessionUtils() {}

	/**
	 * Attempt to replace the private final 'user' field on the Minecraft instance.
	 *
	 * @param mc the client instance
	 * @param newUser the new User object to set
	 * @return empty Optional on success, or Optional containing an error message on failure
	 */
	public static Optional<String> setUser(final Minecraft mc, final User newUser) {
		try {
			Field userField = Minecraft.class.getDeclaredField("user");
			userField.setAccessible(true);

			// remove final modifier so we can write to it
			try {
				Field modifiersField = Field.class.getDeclaredField("modifiers");
				modifiersField.setAccessible(true);
				modifiersField.setInt(userField, userField.getModifiers() & ~Modifier.FINAL);
			} catch (NoSuchFieldException ignored) {
				// some runtimes do not have the 'modifiers' field on Field; ignore and attempt to set anyway
			}

			userField.set(mc, newUser);
			return Optional.empty();
		} catch (Throwable t) {
			return Optional.of("Failed to set user via reflection: " + t.getClass().getName() + ": " + t.getMessage());
		}
	}
}
