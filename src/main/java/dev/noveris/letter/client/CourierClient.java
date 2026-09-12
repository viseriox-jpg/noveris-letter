package dev.noveris.letter.client;

import dev.noveris.letter.NoverisLetter;
import dev.noveris.letter.courier.CourierEntities;
import dev.noveris.letter.courier.precompiled.BoopletModel;
import dev.noveris.letter.courier.precompiled.CapybaraModel;
import dev.noveris.letter.courier.precompiled.CoatiModel;
import dev.noveris.letter.courier.precompiled.MossbloomModel;
import dev.noveris.letter.courier.precompiled.RedPandaModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = NoverisLetter.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CourierClient {
    private CourierClient() { }
    @SubscribeEvent public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CourierModelLayers.BOOPLET, BoopletModel::getTexturedModelData);
        event.registerLayerDefinition(CourierModelLayers.CAPYBARA, CapybaraModel::getTexturedModelData);
        event.registerLayerDefinition(CourierModelLayers.COATI, CoatiModel::getTexturedModelData);
        event.registerLayerDefinition(CourierModelLayers.MOSSBLOOM, MossbloomModel::getTexturedModelData);
        event.registerLayerDefinition(CourierModelLayers.RED_PANDA, RedPandaModel::getTexturedModelData);
    }
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CourierEntities.COURIER_BOOPLET.get(), CourierBirdRenderer::new);
        event.registerEntityRenderer(CourierEntities.COURIER_CAPYBARA.get(), CourierBirdRenderer::new);
        event.registerEntityRenderer(CourierEntities.COURIER_COATI.get(), CourierBirdRenderer::new);
        event.registerEntityRenderer(CourierEntities.COURIER_MOSSBLOOM.get(), CourierBirdRenderer::new);
        event.registerEntityRenderer(CourierEntities.COURIER_RED_PANDA.get(), CourierBirdRenderer::new);
        event.registerEntityRenderer(CourierEntities.COURIER_LEGACY_BIRD.get(), LegacyBirdRenderer::new);
    }
}
