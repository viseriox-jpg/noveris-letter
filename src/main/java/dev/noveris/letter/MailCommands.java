package dev.noveris.letter;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.mail.MailLetter;
import dev.noveris.letter.mail.MailService;

import java.util.Collection;
import java.util.List;

/** Player-facing commands. All mutations are delegated to MailService. */
public final class MailCommands {
    private static final int MAX_MESSAGE_LENGTH = 500;

    private MailCommands() { }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("correio")
                .executes(context -> open(context.getSource()))
                .then(Commands.literal("escrever")
                        .then(Commands.argument("destinatario", GameProfileArgument.gameProfile())
                                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                                        .executes(MailCommands::send))))
                .then(Commands.literal("inbox").executes(MailCommands::inbox))
                .then(Commands.literal("enviadas").executes(MailCommands::sent))
                .then(Commands.literal("ler")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(MailCommands::read)))
                .then(Commands.literal("arquivar")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(MailCommands::archive)))
                .then(Commands.literal("excluir")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(MailCommands::delete))));
    }

    private static int open(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Noveris Letter"), false);
        source.sendSuccess(() -> Component.literal("/correio escrever <jogador> <mensagem>"), false);
        source.sendSuccess(() -> Component.literal("/correio inbox | enviadas | ler <id>"), false);
        return 1;
    }

    private static int send(CommandContext<CommandSourceStack> context) throws Exception {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "destinatario");
        if (profiles.size() != 1) {
            context.getSource().sendFailure(Component.literal("Escolha exatamente um destinatário."));
            return 0;
        }
        GameProfile recipient = profiles.iterator().next();
        String content = StringArgumentType.getString(context, "mensagem");
        MailService.SendResult result = service(sender).sendLetter(sender, recipient.getId(), content, System.currentTimeMillis());
        if (result != MailService.SendResult.SUCCESS) {
            context.getSource().sendFailure(Component.literal(error(result)));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Correspondência enviada para " + recipient.getName() + "."), false);
        return 1;
    }

    private static int inbox(CommandContext<CommandSourceStack> context) throws Exception {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<MailLetter> letters = service(player).inbox(player);
        context.getSource().sendSuccess(() -> Component.literal("Recebidas (" + letters.size() + ")"), false);
        letters.forEach(letter -> context.getSource().sendSuccess(() -> summary(letter), false));
        return letters.size();
    }

    private static int sent(CommandContext<CommandSourceStack> context) throws Exception {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<MailLetter> letters = service(player).sent(player);
        context.getSource().sendSuccess(() -> Component.literal("Enviadas (" + letters.size() + ")"), false);
        letters.forEach(letter -> context.getSource().sendSuccess(() -> summary(letter), false));
        return letters.size();
    }

    private static int read(CommandContext<CommandSourceStack> context) throws Exception {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MailLetter letter = find(player, context);
        if (letter == null || !letter.recipientId().equals(player.getUUID())) return 0;
        service(player).markRead(player, letter.id(), System.currentTimeMillis());
        context.getSource().sendSuccess(() -> Component.literal("Carta de " + letter.senderName() + ": " + letter.content()), false);
        return 1;
    }

    private static int archive(CommandContext<CommandSourceStack> context) throws Exception {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MailLetter letter = find(player, context);
        boolean changed = letter != null && service(player).archive(player, letter.id());
        if (!changed) { context.getSource().sendFailure(Component.literal("Carta não encontrada.")); return 0; }
        context.getSource().sendSuccess(() -> Component.literal("Carta arquivada."), false);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> context) throws Exception {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MailLetter letter = find(player, context);
        boolean changed = letter != null && service(player).delete(player, letter.id());
        if (!changed) { context.getSource().sendFailure(Component.literal("Carta não encontrada.")); return 0; }
        context.getSource().sendSuccess(() -> Component.literal("Carta excluída."), false);
        return 1;
    }

    private static MailLetter find(ServerPlayer player, CommandContext<CommandSourceStack> context) {
        try {
            return service(player).findVisible(player, java.util.UUID.fromString(StringArgumentType.getString(context, "id"))).orElse(null);
        } catch (IllegalArgumentException ignored) {
            context.getSource().sendFailure(Component.literal("ID de carta inválido."));
            return null;
        }
    }

    private static Component summary(MailLetter letter) {
        return Component.literal(letter.id() + " | " + letter.senderName() + " -> " + letter.recipientName() + " | " + letter.status());
    }

    private static String error(MailService.SendResult result) {
        return switch (result) {
            case INVALID_RECIPIENT -> "Destinatário inválido.";
            case INVALID_CONTENT -> "A mensagem deve ter entre 1 e " + MAX_MESSAGE_LENGTH + " caracteres.";
            case UNAVAILABLE -> "Não foi possível enviar essa correspondência.";
            default -> "Não foi possível enviar essa correspondência.";
        };
    }

    private static MailService service(ServerPlayer player) {
        MinecraftServer server = player.server;
        return new MailService(server, new CourierAppearanceRegistry(), MAX_MESSAGE_LENGTH);
    }
}
