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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Processes only ready queue entries; presentation can be added without changing this contract. */
public final class DeliveryManager {
    private static final int TICK_INTERVAL = 20;
    private static final int MAX_DELIVERIES_PER_TICK = 4;

    private DeliveryManager() { }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % TICK_INTERVAL != 0) return;
        for (int i = 0; i < MAX_DELIVERIES_PER_TICK; i++) processOne(server);
    }

    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % TICK_INTERVAL != 0) return;
        processOne(player.server, player);
    }

    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) processFor(player.server, player);
    }

    public static void processFor(MinecraftServer server, ServerPlayer recipient) {
        for (int i = 0; i < MAX_DELIVERIES_PER_TICK; i++) {
            if (!processOne(server, recipient)) return;
        }
    }

    private static void processOne(MinecraftServer server) {
        processOne(server, null);
    }

    private static boolean processOne(MinecraftServer server, ServerPlayer preferredRecipient) {
        MailService service = new MailService(server, new CourierAppearanceRegistry(), 1500, 120);
        var data = dev.noveris.letter.mail.MailSavedData.get(server.overworld());
        reconcilePendingLetters(data, preferredRecipient);
        var next = preferredRecipient == null
                ? data.deliveryQueue().nextReady(System.currentTimeMillis())
                : data.deliveryQueue().nextReadyFor(preferredRecipient.getUUID(), System.currentTimeMillis());
        if (next.isEmpty()) return false;
        DeliveryEntry entry = next.get();
        ServerPlayer recipient = preferredRecipient != null && preferredRecipient.getUUID().equals(entry.recipientId())
                ? preferredRecipient : server.getPlayerList().getPlayer(entry.recipientId());
        if (recipient == null) return false;
        MailLetter letter = service.findVisible(recipient, entry.letterId()).orElse(null);
        if (letter == null) {
            data.deliveryQueue().remove(entry.id());
            data.markChanged();
            return true;
        }
        if (!recipient.getInventory().add(LetterBookFactory.create(letter))) {
            recipient.displayClientMessage(Component.literal("O mensageiro hesita — não há espaço para a correspondência."), true);
            return false;
        }
        if (service.markDelivered(letter.id(), System.currentTimeMillis())) {
            data.deliveryQueue().remove(entry.id());
            data.markChanged();
            recipient.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.9F);
            recipient.displayClientMessage(Component.literal("Uma correspondência selada foi confiada às suas mãos."), true);
        }
        return true;
    }

    /** Repairs queue entries after an interrupted save or an older mod version. */
    private static void reconcilePendingLetters(dev.noveris.letter.mail.MailSavedData data, ServerPlayer recipient) {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (MailLetter letter : data.letters().values()) {
            if (letter.status() != dev.noveris.letter.mail.MailStatus.IN_TRANSIT) continue;
            if (recipient != null && !letter.recipientId().equals(recipient.getUUID())) continue;
            if (data.deliveryQueue().containsLetter(letter.id())) continue;
            data.deliveryQueue().enqueue(DeliveryFactory.create(letter.id(), letter.senderId(), letter.recipientId(),
                    DeliveryType.NORMAL, DeliveryPriority.NORMAL, CourierAppearanceRegistry.DEFAULT_ID, now, now));
            changed = true;
        }
        if (changed) data.markChanged();
    }
}
