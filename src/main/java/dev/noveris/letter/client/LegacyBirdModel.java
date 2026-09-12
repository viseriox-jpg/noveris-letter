package dev.noveris.letter.client;

import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.courier.CourierLegacyBirdEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class LegacyBirdModel extends GeoModel<CourierLegacyBirdEntity> {
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("noveris_letter", path); }
    @Override public ResourceLocation getModelResource(CourierLegacyBirdEntity entity) { return entity.getAppearanceId().equals(CourierAppearanceRegistry.BARN_OWL_ID) ? id("geo/entity/barnowl/barnowlfly.geo.json") : id("geo/entity/sparrow/sparrowfly.geo.json"); }
    @Override public ResourceLocation getTextureResource(CourierLegacyBirdEntity entity) { return entity.getAppearanceId().equals(CourierAppearanceRegistry.BARN_OWL_ID) ? id("textures/entity/courier/barnowlfly.png") : id("textures/entity/courier/sparrowfly.png"); }
    @Override public ResourceLocation getAnimationResource(CourierLegacyBirdEntity entity) { return entity.getAppearanceId().equals(CourierAppearanceRegistry.BARN_OWL_ID) ? id("animations/animation.barnowlfly.json") : id("animations/animation.sparrow.fly.json"); }
}
