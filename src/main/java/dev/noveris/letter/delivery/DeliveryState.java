package dev.noveris.letter.delivery;

public enum DeliveryState {
    WAITING_PICKUP,
    PICKUP_PRESENTING,
    IN_TRANSIT,
    DELIVERY_PRESENTING,
    DELIVERED,
    RETRY_WAIT,
    CANCELLED
}
