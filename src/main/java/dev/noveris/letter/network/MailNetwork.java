package dev.noveris.letter.network;

import com.mojang.authlib.GameProfile;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.delivery.DeliveryManager;
import dev.noveris.letter.mail.MailSavedData;
import dev.noveris.letter.mail.MailService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Modern NeoForge payload boundary; clients never choose an author UUID or price. */
public final class MailNetwork {
    private static final int PAGE_SIZE = 4;
    private MailNetwork() { }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SendLetterPayload.TYPE, SendLetterPayload.STREAM_CODEC, MailNetwork::handleSend);
        registrar.playToServer(RequestMailSnapshotPayload.TYPE, RequestMailSnapshotPayload.STREAM_CODEC, MailNetwork::handleSnapshotRequest);
        registrar.playToServer(SelectCourierPayload.TYPE, SelectCourierPayload.STREAM_CODEC, MailNetwork::handleSelectCourier);
    }

    public static void open(ServerPlayer player, int tab) {
        int safeTab = Math.max(0, Math.min(3, tab));
        PacketDistributor.sendToPlayer(player, new OpenMailScreenPayload(safeTab));
        sendSnapshot(player, safeTab, 0);
    }

    private static void handleSnapshotRequest(RequestMailSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) sendSnapshot(player, payload.tab(), payload.page());
        });
    }

    private static void sendSnapshot(ServerPlayer player, int tab, int requestedPage) {
        DeliveryManager.processFor(player.server, player);
        MailService service = new MailService(player.server, new CourierAppearanceRegistry(), 1500, 120);
        var letters = tab == 1 ? service.sent(player) : service.inbox(player);
        int totalPages = Math.max(1, (letters.size() + PAGE_SIZE - 1) / PAGE_SIZE), page = Math.max(0, Math.min(requestedPage, totalPages - 1)), from = page * PAGE_SIZE;
        var entries = letters.subList(from, Math.min(from + PAGE_SIZE, letters.size())).stream()
                .map(letter -> new MailSnapshotPayload.Entry(letter.id(), letter.senderName(), letter.recipientName(), letter.subject(), preview(letter.content()), letter.status(), letter.sentAt()))
                .toList();
        PacketDistributor.sendToPlayer(player, new MailSnapshotPayload(Math.max(0, Math.min(3, tab)), page, totalPages, entries));
    }

    private static String preview(String content) {
        String clean = content.replace('\n', ' ').replace('\r', ' ').trim();
        return clean.length() <= 150 ? clean : clean.substring(0, 147) + "...";
    }

    private static void handleSelectCourier(SelectCourierPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            CourierAppearanceRegistry registry = new CourierAppearanceRegistry();
            MailSavedData data = MailSavedData.get(player.server.overworld());
            var appearance = registry.find(payload.appearanceId()).orElse(null);
            if (appearance == null) {
                PacketDistributor.sendToPlayer(player, new MailActionResultPayload(false, "Carteiro desconhecido."));
                return;
            }
            var profile = data.profile(player.getUUID());
            if (!profile.isUnlocked(appearance.id())) {
                PacketDistributor.sendToPlayer(player, new MailActionResultPayload(false, "Esse carteiro ainda não está desbloqueado."));
                return;
            }
            profile.select(appearance.id());
            data.markChanged();
            PacketDistributor.sendToPlayer(player, new MailActionResultPayload(true, "Carteiro selecionado: " + appearance.displayName().getString() + "."));
        });
    }

    private static void handleSend(SendLetterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
            String recipientName = payload.recipient().trim();
            ServerPlayer online = sender.server.getPlayerList().getPlayers().stream()
                    .filter(p -> p.getGameProfile().getName().equalsIgnoreCase(recipientName)).findFirst().orElse(null);
            GameProfile profile = online != null ? online.getGameProfile() : sender.server.getProfileCache().get(recipientName).orElse(null);
            MailService.SendResult result = profile == null
                    ? MailService.SendResult.INVALID_RECIPIENT
                    : new MailService(sender.server, new CourierAppearanceRegistry(), 1500, 120)
                    .sendLetter(sender, profile.getId(), payload.subject(), payload.content(), payload.anonymous(), System.currentTimeMillis());
            String message = switch (result) {
                case SUCCESS -> payload.anonymous() ? "Sua correspondência anônima foi selada." : "Sua correspondência foi selada e confiada aos mensageiros de Noveris.";
                case INVALID_RECIPIENT -> "Destinatário não encontrado. Use o nick exato de um jogador conhecido pelo servidor.";
                case INVALID_CONTENT -> "A correspondência está vazia ou excede o limite de 1500 caracteres.";
                case INVALID_SUBJECT -> "O assunto excede o limite permitido.";
                default -> "Não foi possível selar esta correspondência.";
            };
            PacketDistributor.sendToPlayer(sender, new MailActionResultPayload(result == MailService.SendResult.SUCCESS, message));
            if (result == MailService.SendResult.SUCCESS && online != null) DeliveryManager.processFor(sender.server, online);
        });
    }
}
