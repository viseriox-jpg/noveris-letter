package dev.noveris.letter.network;

import dev.noveris.letter.NoverisLetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectCourierPayload(ResourceLocation appearanceId) implements CustomPacketPayload {
    public static final Type<SelectCourierPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "select_courier"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectCourierPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, SelectCourierPayload::appearanceId,
                    SelectCourierPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
