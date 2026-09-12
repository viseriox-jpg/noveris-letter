package dev.noveris.letter.delivery;

import dev.noveris.letter.book.LetterBookFactory;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.courier.CourierEntities;
import dev.noveris.letter.courier.CourierEntity;
import dev.noveris.letter.mail.MailLetter;
import dev.noveris.letter.mail.MailSavedData;
import dev.noveris.letter.mail.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) processFor(server, player);
    }

    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) processFor(player.server, player);
    }

    public static void processFor(MinecraftServer server, ServerPlayer recipient) {
        for (int i = 0; i < MAX_DELIVERIES_PER_PLAYER; i++) if (!processOne(server, recipient)) return;
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
        CourierEntity courier = createCourier(level, appearance.id());
        if (courier == null) return false;

        Vec3 spawn = findSpawnPosition(level, recipient, appearance.id());
        courier.entity().setPos(spawn.x, spawn.y, spawn.z);
        courier.configure(letter.id(), recipient.getUUID(), appearance.id());
        if (!level.addFreshEntity(courier.entity())) return false;

        ACTIVE_COURIERS.put(letter.id(), courier.entity().getUUID());
        data.deliveryQueue().replace(entry.withState(DeliveryState.PRESENTING));
        data.markChanged();
        recipient.displayClientMessage(Component.literal("Um " + appearance.displayName().getString().toLowerCase() + " está a caminho com sua correspondência."), true);
        return true;
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

    private static Vec3 findSpawnPosition(ServerLevel level, ServerPlayer recipient, ResourceLocation appearanceId) {
        double angle = recipient.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = 28.0D + recipient.getRandom().nextDouble() * 6.0D;
        int x = (int) Math.floor(recipient.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(recipient.getZ() + Math.sin(angle) * distance);
        boolean flying = appearanceId.equals(CourierAppearanceRegistry.SPARROW_ID);

        if (flying) {
            return new Vec3(x + 0.5D, recipient.getY() + 4.0D + recipient.getRandom().nextDouble() * 2.0D, z + 0.5D);
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos ground = new BlockPos(x, y, z);
        if (level.getBlockState(ground).isAir() && level.getBlockState(ground.below()).isSolid()) {
            return new Vec3(x + 0.5D, y, z + 0.5D);
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            angle = recipient.getRandom().nextDouble() * Math.PI * 2.0D;
            distance = 28.0D + recipient.getRandom().nextDouble() * 6.0D;
            x = (int) Math.floor(recipient.getX() + Math.cos(angle) * distance);
            z = (int) Math.floor(recipient.getZ() + Math.sin(angle) * distance);
            y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            ground = new BlockPos(x, y, z);
            if (level.getBlockState(ground).isAir() && level.getBlockState(ground.below()).isSolid()) {
                return new Vec3(x + 0.5D, y, z + 0.5D);
            }
        }

        return new Vec3(recipient.getX() + 30.0D,
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) recipient.getX() + 30, (int) recipient.getZ()),
                recipient.getZ());
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
            if (courierId != null && findEntity(server, courierId) instanceof CourierEntity) continue;
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

    private static void clearActive(UUID letterId) { ACTIVE_COURIERS.remove(letterId); }

    private static void reconcilePendingLetters(MailSavedData data, ServerPlayer recipient) {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (MailLetter letter : data.letters().values()) {
            if (letter.status() != dev.noveris.letter.mail.MailStatus.IN_TRANSIT || !letter.recipientId().equals(recipient.getUUID())) continue;
            if (data.deliveryQueue().containsLetter(letter.id())) continue;
            ResourceLocation courierId = data.profile(letter.senderId()).selectedAppearance();
            data.deliveryQueue().enqueue(DeliveryFactory.create(letter.id(), letter.senderId(), letter.recipientId(), DeliveryType.NORMAL, DeliveryPriority.NORMAL, courierId, now, now));
            changed = true;
        }
        if (changed) data.markChanged();
    }
}
