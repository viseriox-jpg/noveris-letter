package dev.noveris.letter.courier.shop;

import net.minecraft.server.level.ServerPlayer;

public interface CurrencyProvider {
    long balance(ServerPlayer player);

    boolean withdraw(ServerPlayer player, long amount);

    default boolean hasBalance(ServerPlayer player, long amount) {
        return balance(player) >= amount;
    }
}
