package dev.noveris.letter.client;

import dev.noveris.letter.courier.CourierLegacyBirdEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class LegacyBirdRenderer extends GeoEntityRenderer<CourierLegacyBirdEntity> {
    public LegacyBirdRenderer(EntityRendererProvider.Context context) { super(context, new LegacyBirdModel()); this.shadowRadius = 0.35F; }
}
