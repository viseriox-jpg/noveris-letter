package dev.noveris.letter.delivery;

import dev.noveris.letter.book.LetterBookFactory;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.courier.CourierEntities;
import dev.noveris.letter.courier.CourierEntity;
import dev.noveris.letter.courier.CourierSpeech;
import dev.noveris.letter.mail.MailLetter;
import dev.noveris.letter.mail.MailSavedData;
import dev.noveris.letter.mail.MailService;
import dev.noveris.letter.network.CourierSpeechPayload;
import dev.noveris.letter.network.MailNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DeliveryManager {
    private static final int TICK_INTERVAL = 20;
    private static final int MAX_DELIVERIES_PER_PLAYER = 4;
    private static final long RETRY_DELAY_MS = 1000L;
    private static final int COURIER_SPEECH_TICKS = 100;
    private static final Map<UUID, UUID> ACTIVE_COURIERS = new HashMap<>();
    private static final Set<UUID> PICKUP_SPEECH_SHOWN = new HashSet<>();

    private DeliveryManager() { }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        reconcileActivePresentations(server);
        if (server.getTickCount() % TICK_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) processFor(server, player);
    }

    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) processFor(player.server, player);
    }

    public static void processFor(MinecraftServer server, ServerPlayer player) {
        for (int i = 0; i < MAX_DELIVERIES_PER_PLAYER; i++) {
            if (processPickupOne(server, player)) continue;
            if (!processDeliveryOne(server, player)) return;
        }
    }

    private static boolean processPickupOne(MinecraftServer server, ServerPlayer sender) {
        MailSavedData data = MailSavedData.get(server.overworld());
        long now = System.currentTimeMillis();
        var next = data.deliveryQueue().nextPickupFor(sender.getUUID(), now);
        if (next.isEmpty()) return false;

        DeliveryEntry entry = next.get();
        MailService service = new MailService(server, new CourierAppearanceRegistry(), 1500, 120);
        MailLetter letter = service.findVisible(sender, entry.letterId()).orElse(null);
        if (letter == null) {
            data.deliveryQueue().remove(entry.id());
            data.markChanged();
            return true;
        }

        var appearance = new CourierAppearanceRegistry().resolveOrDefault(entry.courierAppearanceId());
        ServerLevel level = sender.serverLevel();
        CourierEntity courier = createCourier(level, appearance.id());
        if (courier == null) return false;

        Vec3 spawn = findSpawnPosition(level, sender, appearance.id(), true);
        courier.entity().setPos(spawn.x, spawn.y, spawn.z);
        courier.configure(letter.id(), letter.recipientId(), appearance.id());
        courier.beginPickup(sender);
        if (!level.addFreshEntity(courier.entity())) return false;

        ACTIVE_COURIERS.put(letter.id(), courier.entity().getUUID());
        data.deliveryQueue().replace(entry.withState(DeliveryState.PICKUP_PRESENTING));
        data.markChanged();
        sender.displayClientMessage(Component.literal("Um " + appearance.displayName().getString().toLowerCase() + " está vindo buscar sua carta."), true);
        return true;
    }

    private static boolean processDeliveryOne(MinecraftServer server, ServerPlayer recipient) {
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
        CourierEntity courier = createCourier(level, appearance.id());
        if (courier == null) return false;

        Vec3 spawn = findSpawnPosition(level, recipient, appearance.id(), false);
        courier.entity().setPos(spawn.x, spawn.y, spawn.z);
        courier.configure(letter.id(), recipient.getUUID(), appearance.id());
        if (!level.addFreshEntity(courier.entity())) return false;

        ACTIVE_COURIERS.put(letter.id(), courier.entity().getUUID());
        data.deliveryQueue().replace(entry.withState(DeliveryState.PRESENTING));
        data.markChanged();
        recipient.displayClientMessage(Component.literal("Um " + appearance.displayName().getString().toLowerCase() + " está a caminho com sua correspondência."), true);
        return true;
    }

    public static boolean tryCollectCourier(CourierEntity courier, ServerPlayer sender) {
        if (!courier.isWaitingForPickup() || courier.getPickupSenderId() == null || !courier.getPickupSenderId().equals(sender.getUUID())) return false;

        UUID letterId = courier.getLetterId();
        if (letterId == null) return false;

        MinecraftServer server = sender.server;
        MailSavedData data = MailSavedData.get(server.overworld());
        DeliveryEntry entry = data.deliveryQueue().snapshot().stream()
                .filter(e -> e.letterId().equals(letterId))
                .findFirst().orElse(null);
        if (entry == null || (entry.state() != DeliveryState.PICKUP_PRESENTING && entry.state() != DeliveryState.PICKUP_WAITING)) return false;

        MailService service = new MailService(server, new CourierAppearanceRegistry(), 1500, 120);
        if (service.findVisible(sender, letterId).isEmpty()) return false;

        long now = System.currentTimeMillis();
        data.deliveryQueue().replace(entry.readyAt(now + RETRY_DELAY_MS));
        data.markChanged();

        String speech = CourierSpeech.randomPickupDoneFor(courier.getAppearanceId());
        if (!speech.isBlank()) {
            MailNetwork.sendCourierSpeechToTracking(courier.entity(),
                    new CourierSpeechPayload(courier.entity().getId(), speech, COURIER_SPEECH_TICKS));
        }

        sender.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.65F, 1.05F);
        sender.displayClientMessage(Component.literal("Você entregou a carta ao carteiro."), true);
        PICKUP_SPEECH_SHOWN.remove(letterId);
        clearActive(letterId);
        courier.beginDeparture(sender);
        return true;
    }

    public static void showPickupSpeech(CourierEntity courier) {
        UUID letterId = courier.getLetterId();
        if (letterId == null || !PICKUP_SPEECH_SHOWN.add(letterId)) return;
        String speech = CourierSpeech.randomPickupFor(courier.getAppearanceId());
        if (speech.isBlank()) return;
        MailNetwork.sendCourierSpeechToTracking(courier.entity(),
                new CourierSpeechPayload(courier.entity().getId(), speech, COURIER_SPEECH_TICKS));
    }

    private static CourierEntity createCourier(ServerLevel level, ResourceLocation appearanceId) {
        if (appearanceId.equals(CourierAppearanceRegistry.SPARROW_ID)) {
            return CourierEntities.COURIER_LEGACY_BIRD.get().create(level);
        }
        if (appearanceId.equals(CourierAppearanceRegistry.MELON_ID)
                || appearanceId.equals(CourierAppearanceRegistry.CARROT_ID)
                || appearanceId.equals(CourierAppearanceRegistry.WHEAT_ID)
                || appearanceId.equals(CourierAppearanceRegistry.PUMPKIN_ID)
                || appearanceId.equals(CourierAppearanceRegistry.POTATO_ID)) {
            return CourierEntities.COURIER_CROP.get().create(level);
        }
        return CourierEntities.COURIER_CROP.get().create(level);
    }

    private static Vec3 findSpawnPosition(ServerLevel level, ServerPlayer player, ResourceLocation appearanceId, boolean pickup) {
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = pickup ? 18.0D + player.getRandom().nextDouble() * 5.0D : 28.0D + player.getRandom().nextDouble() * 6.0D;
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
        boolean flying = appearanceId.equals(CourierAppearanceRegistry.SPARROW_ID);

        if (flying) {
            return new Vec3(x + 0.5D, player.getY() + (pickup ? 2.5D : 4.0D) + player.getRandom().nextDouble() * 2.0D, z + 0.5D);
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos ground = new BlockPos(x, y, z);
        if (level.getBlockState(ground).isAir() && level.getBlockState(ground.below()).isSolid()) {
            return new Vec3(x + 0.5D, y, z + 0.5D);
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
            distance = pickup ? 18.0D + player.getRandom().nextDouble() * 5.0D : 28.0D + player.getRandom().nextDouble() * 6.0D;
            x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            ground = new BlockPos(x, y, z);
            if (level.getBlockState(ground).isAir() && level.getBlockState(ground.below()).isSolid()) {
                return new Vec3(x + 0.5D, y, z + 0.5D);
            }
        }

        return new Vec3(player.getX() + (pickup ? 20.0D : 30.0D),
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) player.getX() + (pickup ? 20 : 30), (int) player.getZ()),
                player.getZ());
    }

    public static void finishCourierDelivery(CourierEntity courier, ServerPlayer recipient) {
        UUID letterId = courier.getLetterId();
        if (letterId == null) {
            courier.entity().remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            return;
        }

        MinecraftServer server = recipient.server;
        MailService service = new MailService(server, new CourierAppearanceRegistry(), 1500, 120);
        MailSavedData data = MailSavedData.get(server.overworld());
        MailLetter letter = service.findVisible(recipient, letterId).orElse(null);
        if (letter == null) {
            clearActive(letterId);
            courier.entity().remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            return;
        }

        ItemStack book = LetterBookFactory.create(letter);
        boolean stored = recipient.getInventory().add(book);
        if (!stored) recipient.drop(book, false);

        if (service.markDelivered(letter.id(), System.currentTimeMillis())) {
            data.deliveryQueue().snapshot().stream().filter(e -> e.letterId().equals(letter.id())).findFirst().ifPresent(e -> data.deliveryQueue().remove(e.id()));
            data.markChanged();
            recipient.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 0.9F);
            recipient.displayClientMessage(Component.literal(stored
                    ? "O mensageiro entregou uma correspondência selada em suas mãos."
                    : "O mensageiro deixou a correspondência aos seus pés porque sua mochila estava cheia."), true);

            String speech = CourierSpeech.randomFor(courier.getAppearanceId());
            if (!speech.isBlank()) {
                MailNetwork.sendCourierSpeechToTracking(courier.entity(),
                        new CourierSpeechPayload(courier.entity().getId(), speech, COURIER_SPEECH_TICKS));
            }
        }

        clearActive(letter.id());
        courier.beginDeparture(recipient);
    }

    private static void reconcileActivePresentations(MinecraftServer server) {
        MailSavedData data = MailSavedData.get(server.overworld());
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (DeliveryEntry entry : data.deliveryQueue().snapshot()) {
            boolean pickup = entry.state() == DeliveryState.PICKUP_PRESENTING || entry.state() == DeliveryState.PICKUP_WAITING;
            boolean delivery = entry.state() == DeliveryState.PRESENTING;
            if (!pickup && !delivery) continue;
            UUID courierId = ACTIVE_COURIERS.get(entry.letterId());
            if (courierId != null && findEntity(server, courierId) instanceof CourierEntity) continue;
            if (pickup) data.deliveryQueue().replace(entry.pickupRetryAt(now + RETRY_DELAY_MS));
            else data.deliveryQueue().replace(entry.retryAt(now + RETRY_DELAY_MS));
            PICKUP_SPEECH_SHOWN.remove(entry.letterId());
            clearActive(entry.letterId());
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

    private static void clearActive(UUID letterId) { ACTIVE_COURIERS.remove(letterId); }

    private static void reconcilePendingLetters(MailSavedData data, ServerPlayer recipient) {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (MailLetter letter : data.letters().values()) {
            if (letter.status() != dev.noveris.letter.mail.MailStatus.IN_TRANSIT || !letter.recipientId().equals(recipient.getUUID())) continue;
            if (data.deliveryQueue().containsLetter(letter.id())) continue;
            ResourceLocation courierId = data.profile(letter.senderId()).selectedAppearance();
            data.deliveryQueue().enqueue(DeliveryFactory.create(letter.id(), letter.senderId(), letter.recipientId(), DeliveryType.NORMAL, DeliveryPriority.NORMAL, courierId, now, now).withState(DeliveryState.PICKUP_QUEUED));
            changed = true;
        }
        if (changed) data.markChanged();
    }
}
