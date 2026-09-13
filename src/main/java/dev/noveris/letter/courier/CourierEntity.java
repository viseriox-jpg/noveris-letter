package dev.noveris.letter.courier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/** Common contract for every courier that can carry a letter. */
public interface CourierEntity {
    UUID getLetterId();
    UUID getRecipientId();
    UUID getPickupSenderId();
    ResourceLocation getAppearanceId();
    boolean isWaitingForPickup();
    void configure(UUID letterId, UUID recipientId, ResourceLocation appearance);
    void beginPickup(ServerPlayer sender);
    void beginDeparture(ServerPlayer player);
    Entity entity();
}
