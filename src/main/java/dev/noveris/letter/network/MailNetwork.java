package dev.noveris.letter.network;

import com.mojang.authlib.GameProfile;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.mail.MailService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Modern NeoForge payload boundary; clients never choose an author UUID or price. */
public final class MailNetwork {
    private MailNetwork() { }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SendLetterPayload.TYPE, SendLetterPayload.STREAM_CODEC, MailNetwork::handleSend);
    }

    public static void open(ServerPlayer player, int tab) {
        PacketDistributor.sendToPlayer(player, new OpenMailScreenPayload(Math.max(0, Math.min(3, tab))));
    }

    private static void handleSend(SendLetterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
            String recipientName = payload.recipient().trim();
            ServerPlayer online = sender.server.getPlayerList().getPlayerByName(recipientName);
            GameProfile profile = online != null ? online.getGameProfile()
                    : sender.server.getProfileCache().get(recipientName).orElse(null);
            MailService.SendResult result = profile == null ? MailService.SendResult.INVALID_RECIPIENT
                    : new MailService(sender.server, new CourierAppearanceRegistry(), 1500, 120)
                    .sendLetter(sender, profile.getId(), payload.subject(), payload.content(), System.currentTimeMillis());
            String message = result == MailService.SendResult.SUCCESS
                    ? "Sua correspondência foi selada e confiada aos mensageiros de Noveris."
                    : "Não foi possível selar esta correspondência.";
            PacketDistributor.sendToPlayer(sender, new MailActionResultPayload(result == MailService.SendResult.SUCCESS, message));
        });
    }
}
