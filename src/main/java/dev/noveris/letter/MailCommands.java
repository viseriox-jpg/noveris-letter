package dev.noveris.letter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.mail.MailService;
import dev.noveris.letter.network.MailNetwork;

import java.util.Collection;

/** Command entry point. Postal state is handled by services and screens. */
public final class MailCommands {
    private MailCommands() { }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("correio")
                .executes(context -> open(context.getSource(), 0))
                .then(Commands.literal("escrever")
                        .executes(context -> open(context.getSource(), 2)))
                .then(Commands.literal("inbox")
                        .executes(context -> open(context.getSource(), 0)))
                .then(Commands.literal("enviadas")
                        .executes(context -> open(context.getSource(), 1)))
                .then(Commands.literal("carteiro")
                        .executes(context -> open(context.getSource(), 3)))
                .then(Commands.literal("enviar")
                        .then(Commands.argument("jogador", GameProfileArgument.gameProfile())
                                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                                        .executes(context -> send(context, false)))
                                .then(Commands.literal("anonimo")
                                        .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                                                .executes(context -> send(context, true))))));

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
        MailNetwork.open(player, tab);
        return 1;
    }

    private static int send(CommandContext<CommandSourceStack> context, boolean anonymous) {
        ServerPlayer sender = context.getSource().getPlayer();
        if (sender == null) {
            context.getSource().sendFailure(Component.literal("O serviço postal requer um jogador."));
            return 0;
        }

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

        String content = StringArgumentType.getString(context, "mensagem");
        MailService service = new MailService(sender.server, new CourierAppearanceRegistry(), 1500, 120);
        int sent = 0;
        for (GameProfile profile : profiles) {
            MailService.SendResult result = service.sendLetter(
                    sender, profile.getId(), "", content, anonymous, System.currentTimeMillis());
            if (result == MailService.SendResult.SUCCESS) {
                sent++;
                ServerPlayer online = sender.server.getPlayerList().getPlayer(profile.getId());
                if (online != null) {
                    dev.noveris.letter.delivery.DeliveryManager.processFor(sender.server, online);
                }
            }
        }

        if (sent == 0) {
            context.getSource().sendFailure(Component.literal("Não foi possível enviar a correspondência."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                anonymous
                        ? "Correspondência anônima enviada para " + sent + " jogador(es)."
                        : "Correspondência enviada para " + sent + " jogador(es)."), false);
        return sent;
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
