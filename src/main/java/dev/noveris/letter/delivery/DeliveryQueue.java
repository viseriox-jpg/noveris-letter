package dev.noveris.letter.delivery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DeliveryQueue {
    private static final Comparator<DeliveryEntry> ORDER =
            Comparator.comparingInt((DeliveryEntry entry) -> entry.priority().weight()).reversed()
                    .thenComparingLong(DeliveryEntry::earliestDeliveryTime)
                    .thenComparingLong(DeliveryEntry::queuedAt);

    private final List<DeliveryEntry> entries = new ArrayList<>();

    public synchronized void enqueue(DeliveryEntry entry) {
        if (contains(entry.id())) throw new IllegalArgumentException("Delivery already queued: " + entry.id());
        entries.add(entry);
    }
    public synchronized boolean contains(UUID deliveryId) { return entries.stream().anyMatch(entry -> entry.id().equals(deliveryId)); }
    public synchronized boolean containsLetter(UUID letterId) { return entries.stream().anyMatch(entry -> entry.letterId().equals(letterId)); }
    public synchronized Optional<DeliveryEntry> find(UUID deliveryId) { return entries.stream().filter(entry -> entry.id().equals(deliveryId)).findFirst(); }
    public synchronized Optional<DeliveryEntry> nextReady(long now) {
        return entries.stream().filter(entry -> entry.earliestDeliveryTime() <= now)
                .filter(entry -> entry.phase() == DeliveryPhase.DELIVERY)
                .filter(entry -> entry.state() == DeliveryState.IN_TRANSIT || entry.state() == DeliveryState.RETRY_WAIT)
                .sorted(ORDER).findFirst();
    }
    public synchronized Optional<DeliveryEntry> nextReadyFor(UUID recipientId, long now) {
        return entries.stream().filter(entry -> entry.recipientId().equals(recipientId)).filter(entry -> entry.earliestDeliveryTime() <= now)
                .filter(entry -> entry.phase() == DeliveryPhase.DELIVERY)
                .filter(entry -> entry.state() == DeliveryState.IN_TRANSIT || entry.state() == DeliveryState.RETRY_WAIT)
                .sorted(ORDER).findFirst();
    }
    public synchronized Optional<DeliveryEntry> nextPickupFor(UUID senderId, long now) {
        return entries.stream().filter(entry -> entry.senderId().equals(senderId)).filter(entry -> entry.earliestDeliveryTime() <= now)
                .filter(entry -> entry.phase() == DeliveryPhase.PICKUP)
                .filter(entry -> entry.state() == DeliveryState.WAITING_PICKUP || entry.state() == DeliveryState.RETRY_WAIT)
                .sorted(ORDER).findFirst();
    }
    public synchronized void replace(DeliveryEntry updated) {
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).id().equals(updated.id())) { entries.set(i, updated); return; }
        throw new IllegalArgumentException("Unknown delivery: " + updated.id());
    }
    public synchronized Optional<DeliveryEntry> remove(UUID deliveryId) {
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).id().equals(deliveryId)) return Optional.of(entries.remove(i));
        return Optional.empty();
    }
    public synchronized List<DeliveryEntry> snapshot() { return List.copyOf(entries); }
    public synchronized void replaceAll(Collection<DeliveryEntry> persistedEntries) { entries.clear(); entries.addAll(persistedEntries); }
}
