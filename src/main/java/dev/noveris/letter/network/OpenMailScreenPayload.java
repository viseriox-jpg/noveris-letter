package dev.noveris.letter.network;

import dev.noveris.letter.NoverisLetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenMailScreenPayload(int tab) implements CustomPacketPayload {
    public static final Type<OpenMailScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "open_mail_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMailScreenPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(OpenMailScreenPayload::new, OpenMailScreenPayload::tab);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
