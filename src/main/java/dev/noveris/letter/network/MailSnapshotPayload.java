package dev.noveris.letter.network;

import dev.noveris.letter.NoverisLetter;
import dev.noveris.letter.mail.MailStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MailSnapshotPayload(int tab, int page, int totalPages, List<Entry> entries) implements CustomPacketPayload {
    public record Entry(UUID id, String sender, String recipient, String subject, String preview, MailStatus status, long timestamp) { }
    public static final Type<MailSnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "mail_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MailSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.tab());
                buf.writeVarInt(payload.page());
                buf.writeVarInt(payload.totalPages());
                buf.writeVarInt(Math.min(payload.entries().size(), 4));
                payload.entries().stream().limit(4).forEach(entry -> {
                    buf.writeUUID(entry.id()); buf.writeUtf(entry.sender(), 80); buf.writeUtf(entry.recipient(), 80);
                    buf.writeUtf(entry.subject(), 120); buf.writeUtf(entry.preview(), 160);
                    buf.writeUtf(entry.status().name(), 24); buf.writeLong(entry.timestamp());
                });
            }, buf -> {
                int tab = buf.readVarInt(); int page = buf.readVarInt(); int totalPages = buf.readVarInt();
                int size = Math.min(buf.readVarInt(), 4); List<Entry> entries = new ArrayList<>();
                for (int i = 0; i < size; i++) entries.add(new Entry(buf.readUUID(), buf.readUtf(80), buf.readUtf(80), buf.readUtf(120), buf.readUtf(160), MailStatus.valueOf(buf.readUtf(24)), buf.readLong()));
                return new MailSnapshotPayload(tab, page, totalPages, List.copyOf(entries));
            });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
