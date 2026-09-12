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
    public static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "melon_critter");
    public static final ResourceLocation MELON_ID = DEFAULT_ID;
    public static final ResourceLocation CARROT_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "carrot_critter");
    public static final ResourceLocation WHEAT_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "wheat_critter");
    public static final ResourceLocation PUMPKIN_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "pumpkin_critter");
    public static final ResourceLocation POTATO_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "potato_critter");
    public static final ResourceLocation SPARROW_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "sparrow");
    public static final ResourceLocation BARN_OWL_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "barn_owl");

    private final Map<ResourceLocation, CourierAppearance> appearances = new LinkedHashMap<>();

    public CourierAppearanceRegistry() {
        register(crop(MELON_ID, "Melito", "melon_critter.png", CourierRarity.COMMON));
        register(crop(CARROT_ID, "Cenourito", "carrot_critter.png", CourierRarity.COMMON));
        register(crop(WHEAT_ID, "Tiquinho", "wheat_critter.png", CourierRarity.COMMON));
        register(crop(PUMPKIN_ID, "Abobito", "pumpkin_critter.png", CourierRarity.RARE));
        register(crop(POTATO_ID, "Batatin", "potato_critter.png", CourierRarity.RARE));
        register(new CourierAppearance(SPARROW_ID, Component.literal("Pardal"), SPARROW_ID,
                tex("sparrowfly.png"), tex("sparrowfly.png"), CourierRarity.COMMON,
                CourierMovementStyle.FLYING, true, false, 0, "legacy"));
        register(new CourierAppearance(BARN_OWL_ID, Component.literal("Coruja-das-torres"), BARN_OWL_ID,
                tex("barnowlfly.png"), tex("barnowlfly.png"), CourierRarity.RARE,
                CourierMovementStyle.FLYING, true, false, 0, "legacy"));
    }

    private static CourierAppearance crop(ResourceLocation id, String name, String texture, CourierRarity rarity) {
        return new CourierAppearance(id, Component.literal(name), id, tex(texture), tex(texture), rarity,
                CourierMovementStyle.GROUND, true, false, 0, "default");
    }

    private static ResourceLocation tex(String file) {
        return ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "textures/entity/courier/" + file);
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
