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
    public static final ResourceLocation DEFAULT_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "mossbloom");
    public static final ResourceLocation MOSSBLOOM_ID = DEFAULT_ID;
    public static final ResourceLocation COATI_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "coati");
    public static final ResourceLocation RED_PANDA_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "red_panda");
    public static final ResourceLocation BOOPLET_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "booplet");
    public static final ResourceLocation CAPYBARA_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "capybara");
    public static final ResourceLocation SPARROW_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "sparrow");
    public static final ResourceLocation BARN_OWL_ID = ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "barn_owl");
    private final Map<ResourceLocation, CourierAppearance> appearances = new LinkedHashMap<>();

    public CourierAppearanceRegistry() {
        register(new CourierAppearance(MOSSBLOOM_ID, Component.literal("Mossbloom"), MOSSBLOOM_ID, tex("mossbloom.png"), tex("mossbloom.png"), CourierRarity.COMMON, CourierMovementStyle.GROUND, true, false, 0, "default"));
        register(new CourierAppearance(COATI_ID, Component.literal("Coati"), COATI_ID, tex("coati.png"), tex("coati.png"), CourierRarity.COMMON, CourierMovementStyle.GROUND, true, false, 0, "default"));
        register(new CourierAppearance(RED_PANDA_ID, Component.literal("Red Panda"), RED_PANDA_ID, tex("red_panda.png"), tex("red_panda.png"), CourierRarity.RARE, CourierMovementStyle.GROUND, true, false, 0, "default"));
        register(new CourierAppearance(BOOPLET_ID, Component.literal("Booplet"), BOOPLET_ID, tex("booplet.png"), tex("booplet.png"), CourierRarity.RARE, CourierMovementStyle.GROUND, true, false, 0, "default"));
        register(new CourierAppearance(CAPYBARA_ID, Component.literal("Capybara"), CAPYBARA_ID, tex("capybara.png"), tex("capybara.png"), CourierRarity.EPIC, CourierMovementStyle.GROUND, true, false, 0, "default"));
        register(new CourierAppearance(SPARROW_ID, Component.literal("Pardal"), SPARROW_ID, tex("sparrowfly.png"), tex("sparrowfly.png"), CourierRarity.COMMON, CourierMovementStyle.FLYING, true, false, 0, "legacy"));
        register(new CourierAppearance(BARN_OWL_ID, Component.literal("Coruja-das-torres"), BARN_OWL_ID, tex("barnowlfly.png"), tex("barnowlfly.png"), CourierRarity.RARE, CourierMovementStyle.FLYING, true, false, 0, "legacy"));
    }
    private static ResourceLocation tex(String file) { return ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "textures/entity/courier/" + file); }
    public synchronized void register(CourierAppearance appearance) { if (appearances.putIfAbsent(appearance.id(), appearance) != null) throw new IllegalArgumentException("Duplicate courier appearance: " + appearance.id()); }
    public synchronized Optional<CourierAppearance> find(ResourceLocation id) { return Optional.ofNullable(appearances.get(id)); }
    public synchronized CourierAppearance resolveOrDefault(ResourceLocation id) { return find(id).orElseGet(() -> appearances.get(DEFAULT_ID)); }
    public synchronized Collection<CourierAppearance> values() { return List.copyOf(appearances.values()); }
}
