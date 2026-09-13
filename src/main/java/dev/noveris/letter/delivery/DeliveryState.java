package dev.noveris.letter.delivery;

public enum DeliveryState {
    PICKUP_QUEUED,
    PICKUP_PRESENTING,
    PICKUP_WAITING,
    QUEUED,
    READY,
    PRESENTING,
    DELIVERED,
    RETRY_WAIT,
    PICKUP_RETRY_WAIT,
    CANCELLED
}
