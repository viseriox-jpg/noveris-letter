package dev.noveris.letter.client;

import dev.noveris.letter.NoverisLetter;
import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.courier.CourierCropEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Loads the exact Crop Critters geometry, texture and animation definitions. */
public final class CourierCropModel extends GeoModel<CourierCropEntity> {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, path);
    }

    private String cropName(CourierCropEntity entity) {
        ResourceLocation appearance = entity.getAppearanceId();
        if (appearance.equals(CourierAppearanceRegistry.CARROT_ID)) return "carrot_critter";
        if (appearance.equals(CourierAppearanceRegistry.WHEAT_ID)) return "wheat_critter";
        if (appearance.equals(CourierAppearanceRegistry.PUMPKIN_ID)) return "pumpkin_critter";
        if (appearance.equals(CourierAppearanceRegistry.POTATO_ID)) return "potato_critter";
        return "melon_critter";
    }

    @Override
    public ResourceLocation getModelResource(CourierCropEntity entity) {
        return id("geo/entity/" + cropName(entity) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CourierCropEntity entity) {
        return id("textures/entity/critters/" + cropName(entity) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(CourierCropEntity entity) {
        String animation = cropName(entity).equals("pumpkin_critter")
                ? "pumpkin_critter.animation.json"
                : "basic_critter.animation.json";
        return id("animations/entity/" + animation);
    }
}
