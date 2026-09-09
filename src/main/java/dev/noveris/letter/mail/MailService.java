package dev.noveris.letter.mail;

import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.courier.CourierProfile;
import dev.noveris.letter.delivery.DeliveryEntry;
import dev.noveris.letter.delivery.DeliveryFactory;
import dev.noveris.letter.delivery.DeliveryPriority;
import dev.noveris.letter.delivery.DeliveryType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-only application service. Screens, packets and commands must delegate here. */
public final class MailService {
    private final MinecraftServer server;
    private final CourierAppearanceRegistry appearances;
    private final int maxMessageLength;
    private final int maxSubjectLength;

    public MailService(MinecraftServer server, CourierAppearanceRegistry appearances, int maxMessageLength) {
        this(server, appearances, maxMessageLength, 120);
    }

    public MailService(MinecraftServer server, CourierAppearanceRegistry appearances, int maxMessageLength, int maxSubjectLength) {
        this.server = Objects.requireNonNull(server, "server");
        this.appearances = Objects.requireNonNull(appearances, "appearances");
        if (maxMessageLength < 1) throw new IllegalArgumentException("maxMessageLength must be positive");
        this.maxMessageLength = maxMessageLength;
        this.maxSubjectLength = maxSubjectLength;
    }

    private MailSavedData data() { return MailSavedData.get(server.overworld()); }

    public SendResult sendLetter(ServerPlayer sender, UUID recipientId, String content, long now) {
        return sendLetter(sender, recipientId, "", content, now);
    }

    public SendResult sendLetter(ServerPlayer sender, UUID recipientId, String subject, String content, long now) {
        Objects.requireNonNull(sender, "sender");
        if (recipientId == null) return SendResult.INVALID_RECIPIENT;
        if (content == null || content.isBlank() || content.length() > maxMessageLength) return SendResult.INVALID_CONTENT;
        if (subject == null || subject.length() > maxSubjectLength) return SendResult.INVALID_SUBJECT;
        MailSavedData data = data();
        if (data.blockedByPlayer().getOrDefault(recipientId, List.of()).contains(sender.getUUID())) return SendResult.UNAVAILABLE;

        UUID id = UUID.randomUUID();
        String recipientName = server.getProfileCache().get(recipientId).map(profile -> profile.getName()).orElse("Unknown player");
        MailLetter letter = new MailLetter(id, sender.getUUID(), sender.getGameProfile().getName(), recipientId,
                recipientName, subject.trim(),
                content, now, now, -1L, -1L, MailStatus.IN_TRANSIT);
        CourierProfile profile = data.profile(sender.getUUID());
        DeliveryEntry delivery = DeliveryFactory.create(id, sender.getUUID(), recipientId, DeliveryType.NORMAL,
                DeliveryPriority.NORMAL, appearances.resolveOrDefault(profile.selectedAppearance()).id(), now, now);
        data.letters().put(id, letter);
        data.sentByPlayer().computeIfAbsent(sender.getUUID(), ignored -> new java.util.ArrayList<>()).add(id);
        data.deliveryQueue().enqueue(delivery);
        data.markChanged();
        return SendResult.SUCCESS;
    }

    public Optional<MailLetter> findVisible(ServerPlayer viewer, UUID letterId) {
        MailLetter letter = data().letters().get(letterId);
        if (letter == null || (!letter.senderId().equals(viewer.getUUID()) && !letter.recipientId().equals(viewer.getUUID()))) return Optional.empty();
        return Optional.of(letter);
    }

    public List<MailLetter> inbox(ServerPlayer viewer) {
        return lettersFor(viewer, data().inboxByPlayer().getOrDefault(viewer.getUUID(), List.of()));
    }

    public List<MailLetter> sent(ServerPlayer viewer) {
        return lettersFor(viewer, data().sentByPlayer().getOrDefault(viewer.getUUID(), List.of()));
    }

    private List<MailLetter> lettersFor(ServerPlayer viewer, List<UUID> ids) {
        return ids.stream().map(data().letters()::get).filter(Objects::nonNull)
                .filter(letter -> letter.senderId().equals(viewer.getUUID()) || letter.recipientId().equals(viewer.getUUID()))
                .toList();
    }

    public boolean markDelivered(UUID letterId, long now) {
        MailSavedData data = data(); MailLetter letter = data.letters().get(letterId);
        if (letter == null || letter.status() == MailStatus.DELETED) return false;
        List<UUID> inbox = data.inboxByPlayer().computeIfAbsent(letter.recipientId(), ignored -> new java.util.ArrayList<>());
        if (letter.status() != MailStatus.DELIVERED && letter.status() != MailStatus.READ) {
            data.letters().put(letterId, letter.delivered(now));
            data.markChanged();
        }
        if (!inbox.contains(letterId)) {
            inbox.add(letterId);
            data.markChanged();
        }
        return true;
    }

    public boolean markRead(ServerPlayer reader, UUID letterId, long now) {
        Optional<MailLetter> visible = findVisible(reader, letterId);
        if (visible.isEmpty() || !visible.get().recipientId().equals(reader.getUUID())) return false;
        data().letters().put(letterId, visible.get().read(now)); data().markChanged(); return true;
    }

    public boolean archive(ServerPlayer owner, UUID letterId) {
        Optional<MailLetter> visible = findVisible(owner, letterId);
        if (visible.isEmpty()) return false;
        MailSavedData data = data(); data.archivedByPlayer().computeIfAbsent(owner.getUUID(), ignored -> new java.util.ArrayList<>()).add(letterId); data.markChanged(); return true;
    }

    public boolean delete(ServerPlayer owner, UUID letterId) {
        Optional<MailLetter> visible = findVisible(owner, letterId);
        if (visible.isEmpty()) return false;
        data().letters().put(letterId, visible.get().withStatus(MailStatus.DELETED)); data().markChanged(); return true;
    }

    public boolean blockPlayer(ServerPlayer owner, UUID blocked) {
        if (blocked == null || owner.getUUID().equals(blocked)) return false;
        MailSavedData data = data(); boolean changed = data.blockedByPlayer().computeIfAbsent(owner.getUUID(), ignored -> new java.util.ArrayList<>()).add(blocked); data.markChanged(); return changed;
    }

    public boolean unblockPlayer(ServerPlayer owner, UUID blocked) {
        MailSavedData data = data(); List<UUID> blockedIds = data.blockedByPlayer().get(owner.getUUID());
        if (blockedIds == null) return false; boolean changed = blockedIds.remove(blocked); if (changed) data.markChanged(); return changed;
    }

    public enum SendResult { SUCCESS, INVALID_RECIPIENT, INVALID_CONTENT, INVALID_SUBJECT, UNAVAILABLE }
}
