package dev.noveris.letter.network;

import dev.noveris.letter.NoverisLetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CourierSpeechPayload(int entityId, String message, int durationTicks) implements CustomPacketPayload {
    public static final Type<CourierSpeechPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "courier_speech"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CourierSpeechPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CourierSpeechPayload::entityId,
            ByteBufCodecs.stringUtf8(96), CourierSpeechPayload::message,
            ByteBufCodecs.VAR_INT, CourierSpeechPayload::durationTicks,
            CourierSpeechPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
