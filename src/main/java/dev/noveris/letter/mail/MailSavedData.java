package dev.noveris.letter.mail;

import dev.noveris.letter.courier.CourierProfile;
import dev.noveris.letter.delivery.DeliveryEntry;
import dev.noveris.letter.delivery.DeliveryPriority;
import dev.noveris.letter.delivery.DeliveryQueue;
import dev.noveris.letter.delivery.DeliveryState;
import dev.noveris.letter.delivery.DeliveryType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.core.HolderLookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Single server-side source of truth for letters, indexes, delivery and courier state. */
public final class MailSavedData extends SavedData {
    public static final String DATA_NAME = "noveris_letter_mail";

    private final Map<UUID, MailLetter> letters = new HashMap<>();
    private final Map<UUID, List<UUID>> inboxByPlayer = new HashMap<>();
    private final Map<UUID, List<UUID>> sentByPlayer = new HashMap<>();
    private final Map<UUID, List<UUID>> archivedByPlayer = new HashMap<>();
    private final Map<UUID, List<UUID>> blockedByPlayer = new HashMap<>();
    private final Map<UUID, CourierProfile> courierProfiles = new HashMap<>();
    private final DeliveryQueue deliveryQueue = new DeliveryQueue();

    public static MailSavedData get(ServerLevel level) {
        ServerLevel storageLevel = level.getServer().overworld();
        return storageLevel.getDataStorage().computeIfAbsent(
                new SavedData.Factory<MailSavedData>(MailSavedData::load, MailSavedData::new, DataFixTypes.LEVEL), DATA_NAME);
    }

    public Map<UUID, MailLetter> letters() { return letters; }
    public Map<UUID, List<UUID>> inboxByPlayer() { return inboxByPlayer; }
    public Map<UUID, List<UUID>> sentByPlayer() { return sentByPlayer; }
    public Map<UUID, List<UUID>> archivedByPlayer() { return archivedByPlayer; }
    public Map<UUID, List<UUID>> blockedByPlayer() { return blockedByPlayer; }
    public Map<UUID, CourierProfile> courierProfiles() { return courierProfiles; }
    public DeliveryQueue deliveryQueue() { return deliveryQueue; }

    public CourierProfile profile(UUID playerId) {
        return courierProfiles.computeIfAbsent(playerId, ignored -> new CourierProfile());
    }

    public void markChanged() { setDirty(); }

    private static MailSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MailSavedData data = new MailSavedData();
        ListTag letters = tag.getList("letters", Tag.TAG_COMPOUND);
        for (int i = 0; i < letters.size(); i++) {
            MailLetter letter = readLetter(letters.getCompound(i));
            data.letters.put(letter.id(), letter);
        }
        readIndex(tag.getList("inbox", Tag.TAG_COMPOUND), data.inboxByPlayer);
        readIndex(tag.getList("sent", Tag.TAG_COMPOUND), data.sentByPlayer);
        readIndex(tag.getList("archived", Tag.TAG_COMPOUND), data.archivedByPlayer);
        readIndex(tag.getList("blocked", Tag.TAG_COMPOUND), data.blockedByPlayer);

        ListTag deliveries = tag.getList("delivery_queue", Tag.TAG_COMPOUND);
        List<DeliveryEntry> entries = new ArrayList<>();
        for (int i = 0; i < deliveries.size(); i++) entries.add(readDelivery(deliveries.getCompound(i)));
        data.deliveryQueue.replaceAll(entries);

        ListTag profiles = tag.getList("courier_profiles", Tag.TAG_COMPOUND);
        for (int i = 0; i < profiles.size(); i++) {
            CompoundTag profileTag = profiles.getCompound(i);
            UUID player = profileTag.getUUID("player");
            List<ResourceLocation> unlocked = profileTag.getList("unlocked", Tag.TAG_STRING).stream()
                    .map(element -> ResourceLocation.parse(element.getAsString())).toList();
            data.profile(player).restore(ResourceLocation.parse(profileTag.getString("selected")), unlocked);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag lettersTag = new ListTag();
        letters.values().forEach(letter -> lettersTag.add(writeLetter(letter)));
        tag.put("letters", lettersTag);
        writeIndex(tag, "inbox", inboxByPlayer);
        writeIndex(tag, "sent", sentByPlayer);
        writeIndex(tag, "archived", archivedByPlayer);
        writeIndex(tag, "blocked", blockedByPlayer);

        ListTag deliveries = new ListTag();
        deliveryQueue.snapshot().forEach(entry -> deliveries.add(writeDelivery(entry)));
        tag.put("delivery_queue", deliveries);

        ListTag profiles = new ListTag();
        courierProfiles.forEach((player, profile) -> {
            CompoundTag value = new CompoundTag();
            value.putUUID("player", player);
            value.putString("selected", profile.selectedAppearance().toString());
            ListTag unlocked = new ListTag();
            profile.unlockedAppearances().forEach(id -> unlocked.add(StringTag.valueOf(id.toString())));
            value.put("unlocked", unlocked);
            profiles.add(value);
        });
        tag.put("courier_profiles", profiles);
        return tag;
    }

    private static void writeIndex(CompoundTag root, String name, Map<UUID, List<UUID>> index) {
        ListTag values = new ListTag();
        index.forEach((owner, ids) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", owner);
            ListTag letters = new ListTag();
            ids.forEach(id -> letters.add(StringTag.valueOf(id.toString())));
            entry.put("ids", letters);
            values.add(entry);
        });
        root.put(name, values);
    }

    private static void readIndex(ListTag values, Map<UUID, List<UUID>> index) {
        for (int i = 0; i < values.size(); i++) {
            CompoundTag entry = values.getCompound(i);
            List<UUID> ids = new ArrayList<>();
            entry.getList("ids", Tag.TAG_STRING).forEach(id -> ids.add(UUID.fromString(id.getAsString())));
            index.put(entry.getUUID("player"), ids);
        }
    }

    private static CompoundTag writeLetter(MailLetter letter) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", letter.id()); tag.putUUID("sender", letter.senderId()); tag.putString("sender_name", letter.senderName());
        tag.putUUID("recipient", letter.recipientId()); tag.putString("recipient_name", letter.recipientName()); tag.putString("content", letter.content());
        tag.putLong("created", letter.createdAt()); tag.putLong("sent", letter.sentAt()); tag.putLong("delivered", letter.deliveredAt()); tag.putLong("read", letter.readAt());
        tag.putString("status", letter.status().name());
        return tag;
    }

    private static MailLetter readLetter(CompoundTag tag) {
        return new MailLetter(tag.getUUID("id"), tag.getUUID("sender"), tag.getString("sender_name"), tag.getUUID("recipient"),
                tag.getString("recipient_name"), tag.getString("content"), tag.getLong("created"), tag.getLong("sent"),
                tag.getLong("delivered"), tag.getLong("read"), MailStatus.valueOf(tag.getString("status")));
    }

    private static CompoundTag writeDelivery(DeliveryEntry entry) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", entry.id()); tag.putUUID("letter", entry.letterId()); tag.putUUID("sender", entry.senderId()); tag.putUUID("recipient", entry.recipientId());
        tag.putString("type", entry.deliveryType().name()); tag.putString("priority", entry.priority().name()); tag.putString("appearance", entry.courierAppearanceId().toString());
        tag.putLong("queued", entry.queuedAt()); tag.putLong("earliest", entry.earliestDeliveryTime()); tag.putInt("attempts", entry.attempts()); tag.putString("state", entry.state().name());
        return tag;
    }

    private static DeliveryEntry readDelivery(CompoundTag tag) {
        return new DeliveryEntry(tag.getUUID("id"), tag.getUUID("letter"), tag.getUUID("sender"), tag.getUUID("recipient"),
                DeliveryType.valueOf(tag.getString("type")), DeliveryPriority.valueOf(tag.getString("priority")), ResourceLocation.parse(tag.getString("appearance")),
                tag.getLong("queued"), tag.getLong("earliest"), tag.getInt("attempts"), DeliveryState.valueOf(tag.getString("state")));
    }
}
