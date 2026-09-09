package dev.noveris.letter.client;

import dev.noveris.letter.NoverisLetter;
import dev.noveris.letter.network.MailActionResultPayload;
import dev.noveris.letter.network.OpenMailScreenPayload;
import dev.noveris.letter.network.MailSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = NoverisLetter.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientNetwork {
    private ClientNetwork() { }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenMailScreenPayload.TYPE, OpenMailScreenPayload.STREAM_CODEC, ClientNetwork::open);
        registrar.playToClient(MailActionResultPayload.TYPE, MailActionResultPayload.STREAM_CODEC, ClientNetwork::result);
        registrar.playToClient(MailSnapshotPayload.TYPE, MailSnapshotPayload.STREAM_CODEC, ClientNetwork::snapshot);
    }

    private static void open(OpenMailScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new MailScreen(payload.tab())));
    }

    private static void result(MailActionResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.displayClientMessage(Component.literal(payload.message()), true);
        });
    }

    private static void snapshot(MailSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof MailScreen screen) screen.setSnapshot(payload);
        });
    }
}
