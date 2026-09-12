package dev.noveris.letter.client;

import dev.noveris.letter.NoverisLetter;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.courier.CourierBirdEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CourierBirdModel extends GeoModel<CourierBirdEntity> {
    private static final ResourceLocation RAVEN_MODEL = id("geo/entity/raven/raven.geo.json");
    private static final ResourceLocation SPARROW_MODEL = id("geo/entity/sparrow/sparrow.geo.json");
    private static final ResourceLocation BARN_OWL_MODEL = id("geo/entity/barnowl/barnowl.geo.json");
    private static final ResourceLocation RAVEN_TEXTURE = id("textures/entity/courier/raven.png");
    private static final ResourceLocation SPARROW_TEXTURE = id("textures/entity/courier/sparrow.png");
    private static final ResourceLocation BARN_OWL_TEXTURE = id("textures/entity/courier/barnowl.png");
    private static final ResourceLocation RAVEN_ANIMATION = id("animations/animation.raven.json");
    private static final ResourceLocation SPARROW_ANIMATION = id("animations/animation.sparrow.json");
    private static final ResourceLocation BARN_OWL_ANIMATION = id("animations/animation.barnowl.json");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, path); }

    @Override
    public ResourceLocation getModelResource(CourierBirdEntity entity) {
        ResourceLocation id = entity.getAppearanceId();
        if (id.equals(CourierAppearanceRegistry.SPARROW_ID)) return SPARROW_MODEL;
        if (id.equals(CourierAppearanceRegistry.BARN_OWL_ID)) return BARN_OWL_MODEL;
        return RAVEN_MODEL;
    }
    @Override
    public ResourceLocation getTextureResource(CourierBirdEntity entity) {
        ResourceLocation id = entity.getAppearanceId();
        if (id.equals(CourierAppearanceRegistry.SPARROW_ID)) return SPARROW_TEXTURE;
        if (id.equals(CourierAppearanceRegistry.BARN_OWL_ID)) return BARN_OWL_TEXTURE;
        return RAVEN_TEXTURE;
    }
    @Override
    public ResourceLocation getAnimationResource(CourierBirdEntity entity) {
        ResourceLocation id = entity.getAppearanceId();
        if (id.equals(CourierAppearanceRegistry.SPARROW_ID)) return SPARROW_ANIMATION;
        if (id.equals(CourierAppearanceRegistry.BARN_OWL_ID)) return BARN_OWL_ANIMATION;
        return RAVEN_ANIMATION;
    }
}
