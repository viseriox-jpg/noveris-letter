package dev.noveris.letter;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import dev.noveris.letter.network.MailNetwork;
import dev.noveris.letter.delivery.DeliveryManager;
import org.slf4j.Logger;

@Mod(NoverisLetter.MOD_ID)
public final class NoverisLetter {
    public static final String MOD_ID = "noveris_letter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoverisLetter(IEventBus modBus) {
        LOGGER.info("Initializing Noveris Letter");
        NeoForge.EVENT_BUS.addListener(NoverisLetter::registerCommands);
        NeoForge.EVENT_BUS.addListener(DeliveryManager::tick);
        NeoForge.EVENT_BUS.addListener(DeliveryManager::playerTick);
        NeoForge.EVENT_BUS.addListener(DeliveryManager::playerLoggedIn);
        modBus.addListener(NoverisLetter::registerPayloads);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        MailCommands.register(event);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        MailNetwork.register(event);
    }
}
