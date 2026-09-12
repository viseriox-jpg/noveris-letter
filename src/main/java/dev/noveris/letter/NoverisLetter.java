package dev.noveris.letter;

import com.mojang.logging.LogUtils;
import dev.noveris.letter.client.CourierClient;
import dev.noveris.letter.courier.CourierEntities;
import dev.noveris.letter.delivery.DeliveryManager;
import dev.noveris.letter.network.MailNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(NoverisLetter.MOD_ID)
public final class NoverisLetter {
    public static final String MOD_ID = "noveris_letter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoverisLetter(IEventBus modBus) {
        LOGGER.info("Initializing Noveris Letter");
        CourierEntities.ENTITY_TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(NoverisLetter::registerCommands);
        NeoForge.EVENT_BUS.addListener(DeliveryManager::tick);
        NeoForge.EVENT_BUS.addListener(DeliveryManager::playerLoggedIn);
        modBus.addListener(NoverisLetter::registerPayloads);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modBus.addListener(CourierClient::registerRenderers));
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        MailCommands.register(event);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        MailNetwork.register(event);
    }
}
