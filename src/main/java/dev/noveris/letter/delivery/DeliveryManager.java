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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Processes ready deliveries for online recipients. */
public final class DeliveryManager {
    private static final int TICK_INTERVAL = 20;
    private static final int MAX_DELIVERIES_PER_PLAYER = 4;

    private DeliveryManager() { }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % TICK_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            processFor(server, player);
        }
    }

    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            processFor(player.server, player);
        }
    }

    public static void processFor(MinecraftServer server, ServerPlayer recipient) {
        for (int i = 0; i < MAX_DELIVERIES_PER_PLAYER; i++) {
            if (!processOne(server, recipient)) return;
        }
    }

    private static boolean processOne(MinecraftServer server, ServerPlayer recipient) {
        MailService service = new MailService(server, new CourierAppearanceRegistry(), 1500, 120);
        var data = dev.noveris.letter.mail.MailSavedData.get(server.overworld());
        reconcilePendingLetters(data, recipient);

        var next = data.deliveryQueue().nextReadyFor(recipient.getUUID(), System.currentTimeMillis());
        if (next.isEmpty()) return false;

        DeliveryEntry entry = next.get();
        MailLetter letter = service.findVisible(recipient, entry.letterId()).orElse(null);
        if (letter == null) {
            data.deliveryQueue().remove(entry.id());
            data.markChanged();
            return true;
        }

        ItemStack book = LetterBookFactory.create(letter);
        boolean stored = recipient.getInventory().add(book);
        if (!stored) {
            // A full inventory must not leave the letter permanently in transit.
            // Drop the sealed book at the recipient's feet and finish delivery.
            recipient.drop(book, false);
        }

        if (service.markDelivered(letter.id(), System.currentTimeMillis())) {
            data.deliveryQueue().remove(entry.id());
            data.markChanged();
            recipient.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.9F);
            recipient.displayClientMessage(
                    Component.literal(stored
                            ? "Uma correspondência selada foi confiada às suas mãos."
                            : "Uma correspondência chegou — sua mochila estava cheia e o livro foi deixado aos seus pés."), true);
            return true;
        }
        return false;
    }

    /** Repairs queue entries after an interrupted save or an older mod version. */
    private static void reconcilePendingLetters(dev.noveris.letter.mail.MailSavedData data, ServerPlayer recipient) {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (MailLetter letter : data.letters().values()) {
            if (letter.status() != dev.noveris.letter.mail.MailStatus.IN_TRANSIT) continue;
            if (!letter.recipientId().equals(recipient.getUUID())) continue;
            if (data.deliveryQueue().containsLetter(letter.id())) continue;
            data.deliveryQueue().enqueue(DeliveryFactory.create(
                    letter.id(), letter.senderId(), letter.recipientId(),
                    DeliveryType.NORMAL, DeliveryPriority.NORMAL,
                    CourierAppearanceRegistry.DEFAULT_ID, now, now));
            changed = true;
        }
        if (changed) data.markChanged();
    }
}
