package dev.noveris.letter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.mail.MailService;

import java.util.Collection;
import com.mojang.authlib.GameProfile;

/** Command entry point. Player postal actions are handled by the GUI. */
public final class MailCommands {
    private MailCommands() { }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("correio")
                .executes(context -> open(context.getSource(), 0))
                .then(Commands.literal("carteiro")
                        .executes(context -> open(context.getSource(), 3)));

        var historico = Commands.literal("historico")
                .then(Commands.literal("apagar")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("jogador", GameProfileArgument.gameProfile())
                                .executes(MailCommands::clearHistory)));

        root.then(historico);
        dispatcher.register(root);
    }

    private static int open(CommandSourceStack source, int tab) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("O serviço postal requer um jogador."));
            return 0;
        }
        dev.noveris.letter.network.MailNetwork.open(player, tab);
        return 1;
    }

    private static int clearHistory(CommandContext<CommandSourceStack> context) {
        Collection<GameProfile> profiles;
        try {
            profiles = GameProfileArgument.getGameProfiles(context, "jogador");
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("Jogador não encontrado."));
            return 0;
        }
        if (profiles.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Jogador não encontrado."));
            return 0;
        }

        MailService service = new MailService(
                context.getSource().getServer(), new CourierAppearanceRegistry(), 1500, 120);
        int removed = 0;
        for (GameProfile profile : profiles) {
            removed += service.clearHistory(profile.getId());
        }

        final int totalRemoved = removed;
        context.getSource().sendSuccess(() -> Component.literal(
                totalRemoved == 0
                        ? "Nenhuma correspondência foi encontrada no histórico do jogador."
                        : totalRemoved + " correspondência(s) foram apagadas definitivamente para todos os envolvidos."),
                true);
        return removed;
    }
}
