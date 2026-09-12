package dev.noveris.letter;

import com.mojang.logging.LogUtils;
import dev.noveris.letter.courier.CourierEntities;
import dev.noveris.letter.delivery.DeliveryManager;
import dev.noveris.letter.network.MailNetwork;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(NoverisLetter.MOD_ID)
public final class NoverisLetter {
    public static final String MOD_ID = "noveris_letter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoverisLetter(IEventBus modBus) {
        LOGGER.info("Initializing Noveris Letter");
        CourierEntities.ENTITY_TYPES.register(modBus);
        modBus.addListener(NoverisLetter::registerEntityAttributes);
        NeoForge.EVENT_BUS.addListener(NoverisLetter::registerCommands);
        NeoForge.EVENT_BUS.addListener(DeliveryManager::tick);
        NeoForge.EVENT_BUS.addListener(DeliveryManager::playerLoggedIn);
        modBus.addListener(NoverisLetter::registerPayloads);
    }

    private static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .build();
        event.put(CourierEntities.COURIER_BOOPLET.get(), attributes);
        event.put(CourierEntities.COURIER_CAPYBARA.get(), attributes);
        event.put(CourierEntities.COURIER_COATI.get(), attributes);
        event.put(CourierEntities.COURIER_MOSSBLOOM.get(), attributes);
        event.put(CourierEntities.COURIER_RED_PANDA.get(), attributes);
    }

    private static void registerCommands(RegisterCommandsEvent event) { MailCommands.register(event); }
    private static void registerPayloads(RegisterPayloadHandlersEvent event) { MailNetwork.register(event); }
}
