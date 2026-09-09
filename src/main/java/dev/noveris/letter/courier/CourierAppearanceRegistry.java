package dev.noveris.letter.courier;

import dev.noveris.letter.NoverisLetter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CourierAppearanceRegistry {
    public static final ResourceLocation DEFAULT_ID =
            ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "classic_mailman");

    private final Map<ResourceLocation, CourierAppearance> appearances = new LinkedHashMap<>();

    public CourierAppearanceRegistry() {
        register(new CourierAppearance(
                DEFAULT_ID,
                Component.translatable("courier.noveris_letter.classic_mailman"),
                DEFAULT_ID,
                ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "textures/entity/courier/classic_mailman.png"),
                ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "textures/gui/courier/classic_mailman.png"),
                CourierRarity.COMMON,
                CourierMovementStyle.GROUND,
                true,
                false,
                0,
                "default"
        ));
    }

    public synchronized void register(CourierAppearance appearance) {
        if (appearances.putIfAbsent(appearance.id(), appearance) != null) {
            throw new IllegalArgumentException("Duplicate courier appearance: " + appearance.id());
        }
    }

    public synchronized Optional<CourierAppearance> find(ResourceLocation id) {
        return Optional.ofNullable(appearances.get(id));
    }

    public synchronized CourierAppearance resolveOrDefault(ResourceLocation id) {
        return find(id).orElseGet(() -> appearances.get(DEFAULT_ID));
    }

    public synchronized Collection<CourierAppearance> values() {
        return List.copyOf(appearances.values());
    }
}
