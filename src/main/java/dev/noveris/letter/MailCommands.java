package dev.noveris.letter;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import dev.noveris.letter.network.MailNetwork;

/** Command entry point. Postal state is handled by services and screens. */
public final class MailCommands {
    private MailCommands() { }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("correio")
                .executes(context -> open(context.getSource(), 0))
                .then(Commands.literal("escrever").executes(context -> open(context.getSource(), 2)))
                .then(Commands.literal("inbox").executes(context -> open(context.getSource(), 0)))
                .then(Commands.literal("enviadas").executes(context -> open(context.getSource(), 1)))
                .then(Commands.literal("carteiro").executes(context -> open(context.getSource(), 3))));
    }

    private static int open(CommandSourceStack source, int tab) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("O serviço postal requer um jogador."));
            return 0;
        }
        MailNetwork.open(player, tab);
        return 1;
    }
}
