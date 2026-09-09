package dev.noveris.letter.network;

import dev.noveris.letter.NoverisLetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestMailSnapshotPayload(int tab) implements CustomPacketPayload {
    public static final Type<RequestMailSnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "request_mail_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestMailSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.tab()), buf -> new RequestMailSnapshotPayload(buf.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
