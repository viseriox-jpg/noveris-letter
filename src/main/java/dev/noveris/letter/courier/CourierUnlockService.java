package dev.noveris.letter.courier;

import dev.noveris.letter.courier.shop.CurrencyProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class CourierUnlockService {
    private final CourierAppearanceRegistry registry;
    private final CurrencyProvider currencyProvider;

    public CourierUnlockService(CourierAppearanceRegistry registry, CurrencyProvider currencyProvider) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.currencyProvider = Objects.requireNonNull(currencyProvider, "currencyProvider");
    }

    public boolean canSelect(CourierProfile profile, ResourceLocation appearanceId) {
        return registry.find(appearanceId).isPresent() && profile.isUnlocked(appearanceId);
    }

    public SelectionResult select(CourierProfile profile, ResourceLocation appearanceId) {
        if (!canSelect(profile, appearanceId)) {
            return SelectionResult.NOT_UNLOCKED;
        }

        profile.select(appearanceId);
        return SelectionResult.SUCCESS;
    }

    public PurchaseResult purchase(ServerPlayer player, CourierProfile profile, ResourceLocation appearanceId) {
        var appearance = registry.find(appearanceId).orElse(null);
        if (appearance == null) {
            return PurchaseResult.UNKNOWN_APPEARANCE;
        }

        if (profile.isUnlocked(appearanceId)) {
            return PurchaseResult.ALREADY_UNLOCKED;
        }

        if (!appearance.purchasable()) {
            return PurchaseResult.NOT_PURCHASABLE;
        }

        long price = appearance.price();
        if (!currencyProvider.hasBalance(player, price)) {
            return PurchaseResult.INSUFFICIENT_FUNDS;
        }

        if (!currencyProvider.withdraw(player, price)) {
            return PurchaseResult.PAYMENT_FAILED;
        }

        profile.unlock(appearanceId);
        return PurchaseResult.SUCCESS;
    }

    public enum PurchaseResult {
        SUCCESS,
        UNKNOWN_APPEARANCE,
        ALREADY_UNLOCKED,
        NOT_PURCHASABLE,
        INSUFFICIENT_FUNDS,
        PAYMENT_FAILED
    }

    public enum SelectionResult {
        SUCCESS,
        NOT_UNLOCKED
    }
}
