package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class SpoofUuidCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
           Commands.literal("uuidspoof")
              .requires(Commands.hasPermission(Commands.LEVEL_ALL))
              .then(
                 Commands.argument("username", StringArgumentType.word())
                    .executes(context -> {
                       String username = StringArgumentType.getString(context, "username");
                       // schedule spoof work asynchronously
                       net.lax1dude.eaglercraft.v1_8.mods.BungeeSpoofMod.spoofAsync(username, context.getSource());
                       return 1;
                    })
              )
              .executes(context -> {
                 context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Usage: /uuidspoof <username>"));
                 return 0;
              })
        );
    }
}
