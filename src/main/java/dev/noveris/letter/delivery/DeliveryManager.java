package dev.noveris.letter.delivery;

import dev.noveris.letter.book.LetterBookFactory;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.courier.CourierBirdEntity;
import dev.noveris.letter.courier.CourierEntities;
import dev.noveris.letter.mail.MailLetter;
import dev.noveris.letter.mail.MailSavedData;
import dev.noveris.letter.mail.MailService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Processes ready deliveries and dispatches the selected bird courier. */
public final class DeliveryManager {
    private static final int TICK_INTERVAL = 20;
    private static final int MAX_DELIVERIES_PER_PLAYER = 4;
    private static final long RETRY_DELAY_MS = 1000L;
    private static final Map<UUID, UUID> ACTIVE_COURIERS = new HashMap<>();

    private DeliveryManager() { }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        reconcileActivePresentations(server);
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
        MailSavedData data = MailSavedData.get(server.overworld());
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

        var appearance = new CourierAppearanceRegistry().resolveOrDefault(entry.courierAppearanceId());
        ServerLevel level = recipient.serverLevel();
        CourierBirdEntity courier = CourierEntities.COURIER_BIRD.get().create(level);
        if (courier == null) return false;

        Vec3 spawn = findSpawnPosition(recipient, courier);
        courier.setPos(spawn.x, spawn.y, spawn.z);
        courier.configure(letter.id(), recipient.getUUID(), appearance.id());
        if (!level.addFreshEntity(courier)) return false;

        ACTIVE_COURIERS.put(letter.id(), courier.getUUID());
        data.deliveryQueue().replace(entry.withState(DeliveryState.PRESENTING));
        data.markChanged();
        recipient.displayClientMessage(
                Component.literal("Um " + appearance.displayName().getString().toLowerCase() + " está a caminho com sua correspondência."),
                true);
        return true;
    }

    private static Vec3 findSpawnPosition(ServerPlayer recipient, CourierBirdEntity courier) {
        double angle = recipient.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = 52.0D + recipient.getRandom().nextDouble() * 16.0D;
        double height = 7.0D + recipient.getRandom().nextDouble() * 3.0D;
        double x = recipient.getX() + Math.cos(angle) * distance;
        double z = recipient.getZ() + Math.sin(angle) * distance;
        double y = recipient.getY() + height;
        courier.setPos(x, y, z);
        return new Vec3(x, y, z);
    }

    public static void finishCourierDelivery(CourierBirdEntity courier, ServerPlayer recipient) {
        UUID letterId = courier.getLetterId();
        if (letterId == null) {
            courier.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            return;
        }

        MinecraftServer server = recipient.server;
        MailService service = new MailService(server, new CourierAppearanceRegistry(), 1500, 120);
        MailSavedData data = MailSavedData.get(server.overworld());
        MailLetter letter = service.findVisible(recipient, letterId).orElse(null);
        if (letter == null) {
            clearActive(letterId);
            courier.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            return;
        }

        ItemStack book = LetterBookFactory.create(letter);
        boolean stored = recipient.getInventory().add(book);
        if (!stored) recipient.drop(book, false);

        if (service.markDelivered(letter.id(), System.currentTimeMillis())) {
            data.deliveryQueue().snapshot().stream()
                    .filter(entry -> entry.letterId().equals(letter.id()))
                    .findFirst()
                    .ifPresent(entry -> data.deliveryQueue().remove(entry.id()));
            data.markChanged();
            recipient.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.9F);
            recipient.displayClientMessage(
                    Component.literal(stored
                            ? "O mensageiro entregou uma correspondência selada em suas mãos."
                            : "O mensageiro deixou a correspondência aos seus pés porque sua mochila estava cheia."), true);
        }
        clearActive(letter.id());
        courier.beginDeparture(recipient);
    }

    private static void reconcileActivePresentations(MinecraftServer server) {
        MailSavedData data = MailSavedData.get(server.overworld());
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (DeliveryEntry entry : data.deliveryQueue().snapshot()) {
            if (entry.state() != DeliveryState.PRESENTING) continue;
            UUID courierId = ACTIVE_COURIERS.get(entry.letterId());
            if (courierId != null && findEntity(server, courierId) instanceof CourierBirdEntity) continue;
            data.deliveryQueue().replace(entry.retryAt(now + RETRY_DELAY_MS));
            changed = true;
        }
        if (changed) data.markChanged();
    }

    private static net.minecraft.world.entity.Entity findEntity(MinecraftServer server, UUID entityId) {
        for (ServerLevel level : server.getAllLevels()) {
            var entity = level.getEntity(entityId);
            if (entity != null) return entity;
        }
        return null;
    }

    private static void clearActive(UUID letterId) {
        ACTIVE_COURIERS.remove(letterId);
    }

    /** Repairs queue entries after an interrupted save or an older mod version. */
    private static void reconcilePendingLetters(MailSavedData data, ServerPlayer recipient) {
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
