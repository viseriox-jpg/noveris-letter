package dev.noveris.letter.network;

import dev.noveris.letter.NoverisLetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SendLetterPayload(String recipient, String subject, String content, boolean anonymous) implements CustomPacketPayload {
    public static final Type<SendLetterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "send_letter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SendLetterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(80), SendLetterPayload::recipient,
            ByteBufCodecs.stringUtf8(120), SendLetterPayload::subject,
            ByteBufCodecs.stringUtf8(1500), SendLetterPayload::content,
            ByteBufCodecs.BOOL, SendLetterPayload::anonymous,
            SendLetterPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
