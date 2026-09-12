package dev.noveris.letter.client;

import dev.noveris.letter.NoverisLetter;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.courier.CourierBirdEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CourierBirdModel extends GeoModel<CourierBirdEntity> {
    private static final ResourceLocation RAVEN_MODEL = id("geo/entity/raven/raven.geo.json");
    private static final ResourceLocation RAVEN_FLY_MODEL = id("geo/entity/raven/ravenfly.geo.json");
    private static final ResourceLocation SPARROW_MODEL = id("geo/entity/sparrow/sparrow.geo.json");
    private static final ResourceLocation SPARROW_FLY_MODEL = id("geo/entity/sparrow/sparrowfly.geo.json");
    private static final ResourceLocation BARN_OWL_MODEL = id("geo/entity/barnowl/barnowl.geo.json");
    private static final ResourceLocation BARN_OWL_FLY_MODEL = id("geo/entity/barnowl/barnowlfly.geo.json");
    private static final ResourceLocation RAVEN_TEXTURE = id("textures/entity/courier/raven.png");
    private static final ResourceLocation RAVEN_FLY_TEXTURE = id("textures/entity/courier/ravenfly.png");
    private static final ResourceLocation SPARROW_TEXTURE = id("textures/entity/courier/sparrow.png");
    private static final ResourceLocation SPARROW_FLY_TEXTURE = id("textures/entity/courier/sparrowfly.png");
    private static final ResourceLocation BARN_OWL_TEXTURE = id("textures/entity/courier/barnowl.png");
    private static final ResourceLocation BARN_OWL_FLY_TEXTURE = id("textures/entity/courier/barnowlfly.png");
    private static final ResourceLocation RAVEN_ANIMATION = id("animations/animation.raven.json");
    private static final ResourceLocation SPARROW_ANIMATION = id("animations/animation.sparrow.json");
    private static final ResourceLocation SPARROW_FLY_ANIMATION = id("animations/animation.sparrow.fly.json");
    private static final ResourceLocation BARN_OWL_ANIMATION = id("animations/animation.barnowl.json");
    private static final ResourceLocation BARN_OWL_FLY_ANIMATION = id("animations/animation.barnowlfly.json");

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, path); }

    @Override
    public ResourceLocation getModelResource(CourierBirdEntity entity) {
        ResourceLocation id = entity.getAppearanceId();
        if (id.equals(CourierAppearanceRegistry.SPARROW_ID)) return entity.isFlying() ? SPARROW_FLY_MODEL : SPARROW_MODEL;
        if (id.equals(CourierAppearanceRegistry.BARN_OWL_ID)) return entity.isFlying() ? BARN_OWL_FLY_MODEL : BARN_OWL_MODEL;
        return entity.isFlying() ? RAVEN_FLY_MODEL : RAVEN_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CourierBirdEntity entity) {
        ResourceLocation id = entity.getAppearanceId();
        if (id.equals(CourierAppearanceRegistry.SPARROW_ID)) return entity.isFlying() ? SPARROW_FLY_TEXTURE : SPARROW_TEXTURE;
        if (id.equals(CourierAppearanceRegistry.BARN_OWL_ID)) return entity.isFlying() ? BARN_OWL_FLY_TEXTURE : BARN_OWL_TEXTURE;
        return entity.isFlying() ? RAVEN_FLY_TEXTURE : RAVEN_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CourierBirdEntity entity) {
        ResourceLocation id = entity.getAppearanceId();
        if (id.equals(CourierAppearanceRegistry.SPARROW_ID)) return SPARROW_FLY_ANIMATION;
        if (id.equals(CourierAppearanceRegistry.BARN_OWL_ID)) return BARN_OWL_FLY_ANIMATION;
        return RAVEN_ANIMATION;
    }
}
