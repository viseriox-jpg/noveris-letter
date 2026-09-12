package dev.noveris.letter.courier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public interface CourierBirdEntity {
    UUID getLetterId();
    UUID getRecipientId();
    ResourceLocation getAppearanceId();
    void configure(UUID letterId, UUID recipientId, ResourceLocation appearance);
    void beginDeparture(ServerPlayer recipient);
    Entity entity();
}
