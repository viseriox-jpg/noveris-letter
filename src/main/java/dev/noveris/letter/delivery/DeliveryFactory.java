package dev.noveris.letter.delivery;

import dev.noveris.letter.courier.CourierAppearanceRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class DeliveryFactory {
    private DeliveryFactory() {
    }

    public static DeliveryEntry create(
            UUID letterId,
            UUID senderId,
            UUID recipientId,
            DeliveryType type,
            DeliveryPriority priority,
            ResourceLocation selectedAppearance,
            long now,
            long earliestDeliveryTime
    ) {
        ResourceLocation appearance = selectedAppearance != null
                ? selectedAppearance
                : CourierAppearanceRegistry.DEFAULT_ID;

        return new DeliveryEntry(
                UUID.randomUUID(),
                letterId,
                senderId,
                recipientId,
                type,
                priority,
                appearance,
                now,
                earliestDeliveryTime,
                0,
                DeliveryState.QUEUED
        );
    }
}
