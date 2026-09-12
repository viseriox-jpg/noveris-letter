package dev.noveris.letter.courier;

import dev.noveris.letter.delivery.DeliveryManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class CourierState {
    private static final double WALK_SPEED = 1.05D;
    private static final double ARRIVAL_DISTANCE = 2.5D;
    private static final double DEPARTURE_SPEED = 1.15D;
    private static final int DEPARTURE_TICKS = 100;

    private final Entity owner;
    private UUID letterId;
    private UUID recipientId;
    private ResourceLocation appearanceId = CourierAppearanceRegistry.DEFAULT_ID;
    private int lostTargetTicks;
    private int departureTicks;
    private boolean departing;
    private boolean deliveryFinished;
    private Vec3 departureTarget = Vec3.ZERO;

    public CourierState(Entity owner) { this.owner = owner; }
    public UUID letterId() { return letterId; }
    public UUID recipientId() { return recipientId; }
    public ResourceLocation appearanceId() { return appearanceId; }

    public void configure(UUID letterId, UUID recipientId, ResourceLocation appearance) {
        this.letterId = letterId;
        this.recipientId = recipientId;
        this.appearanceId = appearance;
        lostTargetTicks = 0;
        departureTicks = 0;
        departing = false;
        deliveryFinished = false;
        departureTarget = Vec3.ZERO;
        owner.setNoGravity(false);
    }

    public void beginDeparture(ServerPlayer recipient) {
        if (!(owner instanceof Mob mob)) {
            owner.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        Vec3 away = owner.position().subtract(recipient.getX(), owner.getY(), recipient.getZ());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 0.01D) horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        departureTarget = owner.position().add(horizontal.normalize().scale(26.0D));
        departing = true;
        deliveryFinished = true;
        departureTicks = 0;
        faceTowards(departureTarget);
        mob.getNavigation().moveTo(departureTarget.x, departureTarget.y, departureTarget.z, DEPARTURE_SPEED);
    }

    public void tick() {
        owner.setNoGravity(false);
        if (owner.level().isClientSide()) return;
        if (!(owner instanceof Mob mob)) {
            owner.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        if (departing) {
            faceTowards(departureTarget);
            mob.getNavigation().moveTo(departureTarget.x, departureTarget.y, departureTarget.z, DEPARTURE_SPEED);
            departureTicks++;
            if (departureTicks >= DEPARTURE_TICKS || mob.getNavigation().isDone() || owner.position().distanceToSqr(departureTarget) < 1.5D) {
                owner.remove(Entity.RemovalReason.DISCARDED);
            }
            return;
        }

        if (letterId == null || recipientId == null) {
            owner.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        ServerPlayer target = owner.level().getServer() == null
                ? null
                : owner.level().getServer().getPlayerList().getPlayer(recipientId);
        if (target == null || target.isRemoved() || target.isSpectator()) {
            lostTargetTicks++;
            mob.getNavigation().stop();
            if (lostTargetTicks > 200) owner.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        lostTargetTicks = 0;
        double distance = owner.distanceTo(target);
        if (distance <= ARRIVAL_DISTANCE) {
            mob.getNavigation().stop();
            faceTowards(target.position());
            if (!deliveryFinished) DeliveryManager.finishCourierDelivery((CourierEntity) owner, target);
            return;
        }

        mob.getNavigation().moveTo(target, WALK_SPEED);
        faceTowards(target.position());
    }

    private void faceTowards(Vec3 target) {
        if (!(owner instanceof Mob mob)) return;
        Vec3 delta = target.subtract(owner.position());
        if (delta.x * delta.x + delta.z * delta.z < 0.001D) return;
        float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
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
