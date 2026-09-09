package dev.noveris.letter.delivery;

public enum DeliveryState {
    QUEUED,
    READY,
    PRESENTING,
    DELIVERED,
    RETRY_WAIT,
    CANCELLED
}
