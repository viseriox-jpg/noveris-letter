package dev.noveris.letter.client;

import dev.noveris.letter.courier.CourierBirdEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CourierBirdRenderer extends GeoEntityRenderer<CourierBirdEntity> {
    public CourierBirdRenderer(EntityRendererProvider.Context context) {
        super(context, new CourierBirdModel());
        this.shadowRadius = 0.18F;
    }
}
