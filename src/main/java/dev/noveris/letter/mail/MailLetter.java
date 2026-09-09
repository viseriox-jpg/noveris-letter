package dev.noveris.letter.mail;

import java.util.Objects;
import java.util.UUID;

public record MailLetter(
        UUID id,
        UUID senderId,
        String senderName,
        UUID recipientId,
        String recipientName,
        String content,
        long createdAt,
        long sentAt,
        long deliveredAt,
        long readAt,
        MailStatus status
) {
    public MailLetter {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(senderId, "senderId");
        Objects.requireNonNull(senderName, "senderName");
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(recipientName, "recipientName");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(status, "status");
    }

    public MailLetter withStatus(MailStatus newStatus) {
        return new MailLetter(id, senderId, senderName, recipientId, recipientName, content,
                createdAt, sentAt, deliveredAt, readAt, newStatus);
    }

    public MailLetter delivered(long timestamp) {
        return new MailLetter(id, senderId, senderName, recipientId, recipientName, content,
                createdAt, sentAt, timestamp, readAt, MailStatus.DELIVERED);
    }

    public MailLetter read(long timestamp) {
        return new MailLetter(id, senderId, senderName, recipientId, recipientName, content,
                createdAt, sentAt, deliveredAt, timestamp, MailStatus.READ);
    }
}
