package dev.noveris.letter.courier;

import dev.noveris.letter.delivery.DeliveryManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class CourierState {
    private static final double FLIGHT_SPEED = 0.055D;
    private static final double ARRIVAL_DISTANCE = 2.5D;
    private static final double FLIGHT_HEIGHT = 4.5D;
    private static final double DEPARTURE_SPEED = 0.105D;
    private static final int DEPARTURE_TICKS = 80;

    private final Entity owner;
    private UUID letterId;
    private UUID recipientId;
    private ResourceLocation appearanceId = CourierAppearanceRegistry.DEFAULT_ID;
    private int lostTargetTicks;
    private int departureTicks;
    private Vec3 departureVelocity = Vec3.ZERO;
    private boolean departing;
    private boolean deliveryFinished;

    public CourierState(Entity owner) { this.owner = owner; }
    public UUID letterId() { return letterId; }
    public UUID recipientId() { return recipientId; }
    public ResourceLocation appearanceId() { return appearanceId; }

    public void configure(UUID letterId, UUID recipientId, ResourceLocation appearance) {
        this.letterId = letterId;
        this.recipientId = recipientId;
        this.appearanceId = appearance;
        this.lostTargetTicks = 0;
        this.departureTicks = 0;
        this.departureVelocity = Vec3.ZERO;
        this.departing = false;
        this.deliveryFinished = false;
        owner.setNoGravity(true);
    }

    public void beginDeparture(ServerPlayer recipient) {
        Vec3 away = owner.position().subtract(recipient.getX(), recipient.getY() + 1.5D, recipient.getZ());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 0.01D) horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        departureVelocity = horizontal.normalize().scale(DEPARTURE_SPEED).add(0.0D, 0.018D, 0.0D);
        departing = true;
        deliveryFinished = true;
        departureTicks = 0;
        owner.setDeltaMovement(departureVelocity);
        owner.hasImpulse = true;
    }

    public void tick() {
        owner.setNoGravity(true);
        if (owner.level().isClientSide()) return;
        if (departing) {
            departureTicks++;
            owner.setDeltaMovement(departureVelocity);
            owner.hasImpulse = true;
            if (departureTicks >= DEPARTURE_TICKS) owner.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        if (letterId == null || recipientId == null) {
            owner.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        ServerPlayer target = owner.level().getServer() == null ? null : owner.level().getServer().getPlayerList().getPlayer(recipientId);
        if (target == null || target.isRemoved() || target.isSpectator()) {
            lostTargetTicks++;
            owner.setDeltaMovement(Vec3.ZERO);
            if (lostTargetTicks > 200) owner.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        lostTargetTicks = 0;
        double targetY = target.getY() + FLIGHT_HEIGHT;
        Vec3 toTarget = new Vec3(target.getX() - owner.getX(), targetY - owner.getY(), target.getZ() - owner.getZ());
        double distance = toTarget.length();
        if (distance <= ARRIVAL_DISTANCE) {
            owner.setDeltaMovement(Vec3.ZERO);
            if (!deliveryFinished) DeliveryManager.finishCourierDelivery((CourierBirdEntity) owner, target);
            return;
        }
        Vec3 velocity = toTarget.scale(FLIGHT_SPEED / Math.max(distance, 0.001D));
        owner.setDeltaMovement(velocity);
        owner.hasImpulse = true;
        owner.setYRot((float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)));
        owner.yRotO = owner.getYRot();
        owner.setXRot((float) Math.toDegrees(-Math.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z))));
        owner.xRotO = owner.getXRot();
    }

    public void save(CompoundTag tag) {
        tag.putString("courier_appearance", appearanceId.toString());
        if (letterId != null) tag.putUUID("courier_letter", letterId);
        if (recipientId != null) tag.putUUID("courier_recipient", recipientId);
        tag.putBoolean("courier_departing", departing);
    }

    public void load(CompoundTag tag) {
        String appearance = tag.getString("courier_appearance");
        if (!appearance.isBlank()) appearanceId = ResourceLocation.parse(appearance);
        if (tag.hasUUID("courier_letter")) letterId = tag.getUUID("courier_letter");
        if (tag.hasUUID("courier_recipient")) recipientId = tag.getUUID("courier_recipient");
        departing = tag.getBoolean("courier_departing");
        deliveryFinished = departing;
    }
}
