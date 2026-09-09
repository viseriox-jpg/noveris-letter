package dev.noveris.letter.courier;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record CourierAppearance(
        ResourceLocation id,
        Component displayName,
        ResourceLocation modelId,
        ResourceLocation texture,
        ResourceLocation icon,
        CourierRarity rarity,
        CourierMovementStyle movementStyle,
        boolean defaultUnlocked,
        boolean purchasable,
        long price,
        String currencyType
) {
    public CourierAppearance {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(movementStyle, "movementStyle");
        Objects.requireNonNull(currencyType, "currencyType");

        if (price < 0) {
            throw new IllegalArgumentException("price cannot be negative");
        }
    }
}
