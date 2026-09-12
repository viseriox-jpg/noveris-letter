package dev.noveris.letter.courier;

import net.minecraft.resources.ResourceLocation;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class CourierProfile {
    private ResourceLocation selectedAppearance;
    private final Set<ResourceLocation> unlockedAppearances = new LinkedHashSet<>();
    public CourierProfile() {
        this.selectedAppearance = CourierAppearanceRegistry.DEFAULT_ID;
        this.unlockedAppearances.addAll(java.util.List.of(
                CourierAppearanceRegistry.MOSSBLOOM_ID,
                CourierAppearanceRegistry.COATI_ID,
                CourierAppearanceRegistry.RED_PANDA_ID,
                CourierAppearanceRegistry.BOOPLET_ID,
                CourierAppearanceRegistry.CAPYBARA_ID,
                CourierAppearanceRegistry.SPARROW_ID,
                CourierAppearanceRegistry.BARN_OWL_ID));
    }
    public ResourceLocation selectedAppearance() { return selectedAppearance; }
    public Set<ResourceLocation> unlockedAppearances() { return Set.copyOf(unlockedAppearances); }
    public boolean isUnlocked(ResourceLocation id) { return unlockedAppearances.contains(id); }
    public boolean unlock(ResourceLocation id) { Objects.requireNonNull(id, "id"); return unlockedAppearances.add(id); }
    public void select(ResourceLocation id) { Objects.requireNonNull(id, "id"); if (!isUnlocked(id)) throw new IllegalStateException("Courier appearance is not unlocked: " + id); selectedAppearance = id; }
    public void restore(ResourceLocation selected, Collection<ResourceLocation> unlocked) {
        unlockedAppearances.clear();
        unlockedAppearances.addAll(java.util.List.of(
                CourierAppearanceRegistry.MOSSBLOOM_ID,
                CourierAppearanceRegistry.COATI_ID,
                CourierAppearanceRegistry.RED_PANDA_ID,
                CourierAppearanceRegistry.BOOPLET_ID,
                CourierAppearanceRegistry.CAPYBARA_ID,
                CourierAppearanceRegistry.SPARROW_ID,
                CourierAppearanceRegistry.BARN_OWL_ID));
        unlockedAppearances.addAll(unlocked);
        selectedAppearance = unlockedAppearances.contains(selected) ? selected : CourierAppearanceRegistry.DEFAULT_ID;
    }
}
