package dev.noveris.letter.client;

import dev.noveris.letter.courier.CourierCropEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CourierCropRenderer extends GeoEntityRenderer<CourierCropEntity> {
    public CourierCropRenderer(EntityRendererProvider.Context context) {
        super(context, new CourierCropModel());
        this.shadowRadius = 0.35F;
    }
}
