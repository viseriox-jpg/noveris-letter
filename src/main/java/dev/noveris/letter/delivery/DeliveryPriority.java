package dev.noveris.letter.delivery;

public enum DeliveryPriority {
    LOW(0),
    NORMAL(10),
    HIGH(20),
    URGENT(30);

    private final int weight;

    DeliveryPriority(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
