package dev.noveris.letter.delivery;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public record DeliveryEntry(
        UUID id,
        UUID letterId,
        UUID senderId,
        UUID recipientId,
        DeliveryType deliveryType,
        DeliveryPriority priority,
        ResourceLocation courierAppearanceId,
        long queuedAt,
        long earliestDeliveryTime,
        int attempts,
        DeliveryState state
) {
    public DeliveryEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(letterId, "letterId");
        Objects.requireNonNull(senderId, "senderId");
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(deliveryType, "deliveryType");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(courierAppearanceId, "courierAppearanceId");
        Objects.requireNonNull(state, "state");
    }

    public DeliveryEntry withState(DeliveryState newState) {
        return new DeliveryEntry(id, letterId, senderId, recipientId, deliveryType, priority,
                courierAppearanceId, queuedAt, earliestDeliveryTime, attempts, newState);
    }

    public DeliveryEntry retryAt(long timestamp) {
        return new DeliveryEntry(id, letterId, senderId, recipientId, deliveryType, priority,
                courierAppearanceId, queuedAt, timestamp, attempts + 1, DeliveryState.RETRY_WAIT);
    }
}
