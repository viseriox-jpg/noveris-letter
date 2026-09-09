package dev.noveris.letter.network;

import dev.noveris.letter.NoverisLetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MailActionResultPayload(boolean success, String message) implements CustomPacketPayload {
    public static final Type<MailActionResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "mail_action_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MailActionResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MailActionResultPayload::success,
            ByteBufCodecs.stringUtf8(256), MailActionResultPayload::message,
            MailActionResultPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
