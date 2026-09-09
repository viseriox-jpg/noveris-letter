package dev.noveris.letter;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(NoverisLetter.MOD_ID)
public final class NoverisLetter {
    public static final String MOD_ID = "noveris_letter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoverisLetter(IEventBus modBus) {
        LOGGER.info("Initializing Noveris Letter");
    }
}
