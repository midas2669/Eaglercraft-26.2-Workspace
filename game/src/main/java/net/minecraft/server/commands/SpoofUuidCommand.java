package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

public class SpoofUuidCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
           Commands.literal("uuidspoof")
              .requires(Commands.hasPermission(Commands.LEVEL_ALL))
              .then(
                 Commands.argument("username", StringArgumentType.word())
                    .executes(context -> {
                       String username = StringArgumentType.getString(context, "username");
                       return spoof(context.getSource(), username);
                    })
              )
              .executes(context -> {
                 context.getSource().sendFailure(Component.literal("Usage: /uuidspoof <username>"));
                 return 0;
              })
        );
    }

    private static int spoof(final CommandSourceStack source, final String username) {
        String targetUsername = username == null ? "" : username.trim();
        if (targetUsername.isEmpty()) {
           source.sendFailure(Component.literal("UUID spoof failed: empty username"));
           return 0;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
           source.sendFailure(Component.literal("UUID spoof failed: Minecraft client is not initialized"));
           return 0;
        }

        User currentUser = minecraft.getUser();
        UUID targetUuid = UUIDUtil.createOfflinePlayerUUID(targetUsername);
        User spoofedUser = new User(targetUsername, targetUuid, currentUser.getAccessToken(), currentUser.getXuid(), currentUser.getClientId());
        minecraft.setUser(spoofedUser);

        MutableComponent success = Component.literal("UUID spoof successful: user=")
           .append(Component.literal(targetUsername).withStyle(ChatFormatting.GREEN))
           .append(Component.literal(" -> uuid=") .withStyle(ChatFormatting.YELLOW))
           .append(Component.literal(targetUuid.toString()).withStyle(ChatFormatting.AQUA));

        source.sendSystemMessage(success);
        minecraft.showDebugChat(success);
        return 1;
    }
}
