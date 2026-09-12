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
    public static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "raven");
    public static final ResourceLocation SPARROW_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "sparrow");
    public static final ResourceLocation BARN_OWL_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "barn_owl");
    private final Map<ResourceLocation, CourierAppearance> appearances = new LinkedHashMap<>();

    public CourierAppearanceRegistry() {
        register(new CourierAppearance(DEFAULT_ID, Component.literal("Corvo"), DEFAULT_ID,
                ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "textures/entity/courier/raven.png"),
                ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "textures/entity/courier/raven.png"),
                CourierRarity.COMMON, CourierMovementStyle.GROUND, true, false, 0, "default"));
        register(new CourierAppearance(SPARROW_ID, Component.literal("Pardal"), SPARROW_ID,
                ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "textures/entity/courier/sparrow.png"),
                ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "textures/entity/courier/sparrow.png"),
                CourierRarity.COMMON, CourierMovementStyle.GROUND, true, false, 0, "default"));
        register(new CourierAppearance(BARN_OWL_ID, Component.literal("Coruja-das-torres"), BARN_OWL_ID,
                ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "textures/entity/courier/barnowl.png"),
                ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "textures/entity/courier/barnowl.png"),
                CourierRarity.RARE, CourierMovementStyle.GROUND, true, false, 0, "default"));
    }

    public synchronized void register(CourierAppearance appearance) {
        if (appearances.putIfAbsent(appearance.id(), appearance) != null) throw new IllegalArgumentException("Duplicate courier appearance: " + appearance.id());
    }
    public synchronized Optional<CourierAppearance> find(ResourceLocation id) { return Optional.ofNullable(appearances.get(id)); }
    public synchronized CourierAppearance resolveOrDefault(ResourceLocation id) { return find(id).orElseGet(() -> appearances.get(DEFAULT_ID)); }
    public synchronized Collection<CourierAppearance> values() { return List.copyOf(appearances.values()); }
}
