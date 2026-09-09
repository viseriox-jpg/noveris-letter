package dev.noveris.letter.delivery;

import dev.noveris.letter.book.LetterBookFactory;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.mail.MailLetter;
import dev.noveris.letter.mail.MailService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Processes only ready queue entries; presentation can be added without changing this contract. */
public final class DeliveryManager {
    private static final int TICK_INTERVAL = 20;
    private static final int MAX_DELIVERIES_PER_TICK = 1;

    private DeliveryManager() { }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % TICK_INTERVAL != 0) return;
        for (int i = 0; i < MAX_DELIVERIES_PER_TICK; i++) processOne(server);
    }

    private static void processOne(MinecraftServer server) {
        MailService service = new MailService(server, new CourierAppearanceRegistry(), 1500, 120);
        var data = dev.noveris.letter.mail.MailSavedData.get(server.overworld());
        var next = data.deliveryQueue().nextReady(System.currentTimeMillis());
        if (next.isEmpty()) return;
        DeliveryEntry entry = next.get();
        ServerPlayer recipient = server.getPlayerList().getPlayer(entry.recipientId());
        if (recipient == null) return;
        MailLetter letter = service.findVisible(recipient, entry.letterId()).orElse(null);
        if (letter == null) {
            data.deliveryQueue().remove(entry.id());
            data.markChanged();
            return;
        }
        if (!recipient.getInventory().add(LetterBookFactory.create(letter))) {
            recipient.displayClientMessage(Component.literal("O mensageiro hesita — não há espaço para a correspondência."), true);
            return;
        }
        if (service.markDelivered(letter.id(), System.currentTimeMillis())) {
            data.deliveryQueue().remove(entry.id());
            data.markChanged();
            recipient.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.9F);
            recipient.displayClientMessage(Component.literal("Uma correspondência selada foi confiada às suas mãos."), true);
        }
    }
}
