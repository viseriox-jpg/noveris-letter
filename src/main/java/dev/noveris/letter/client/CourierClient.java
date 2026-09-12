package dev.noveris.letter.client;

import dev.noveris.letter.courier.CourierEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class CourierClient {
    private CourierClient() { }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CourierEntities.COURIER_BIRD.get(), CourierBirdRenderer::new);
    }
}
