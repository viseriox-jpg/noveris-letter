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
    private static final int DEPARTURE_WAIT_TICKS = 100;
    private static final int DEPARTURE_TICKS = 100;

    private final Entity owner;
    private UUID letterId;
    private UUID targetPlayerId;
    private UUID departureLookId;
    private ResourceLocation appearanceId = CourierAppearanceRegistry.DEFAULT_ID;
    private int lostTargetTicks;
    private int departureWaitTicks;
    private int departureTicks;
    private CourierMode mode = CourierMode.DELIVERY;
    private boolean pickupWaiting;
    private boolean departureWaiting;
    private boolean departing;
    private boolean deliveryFinished;
    private Vec3 departureTarget = Vec3.ZERO;

    public CourierState(Entity owner) { this.owner = owner; }
    public UUID letterId() { return letterId; }
    public UUID targetPlayerId() { return targetPlayerId; }
    public CourierMode mode() { return mode; }
    public ResourceLocation appearanceId() { return appearanceId; }
    public boolean isWaitingForPickup() { return mode == CourierMode.PICKUP && pickupWaiting; }

    public void configure(UUID letterId, UUID targetPlayerId, ResourceLocation appearance, CourierMode mode) {
        this.letterId = letterId;
        this.targetPlayerId = targetPlayerId;
        this.mode = mode;
        this.departureLookId = null;
        this.appearanceId = appearance;
        lostTargetTicks = 0;
        departureWaitTicks = 0;
        departureTicks = 0;
        pickupWaiting = false;
        departureWaiting = false;
        departing = false;
        deliveryFinished = false;
        departureTarget = Vec3.ZERO;
        owner.setNoGravity(false);
    }

    public void beginDeparture(ServerPlayer player) {
        if (!(owner instanceof Mob mob)) {
            owner.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        CourierMode completedMode = mode;
        pickupWaiting = false;
        departureLookId = player.getUUID();

        Vec3 away = owner.position().subtract(player.getX(), owner.getY(), player.getZ());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 0.01D) horizontal = new Vec3(1.0D, 0.0D, 0.0D);

        // Pickup acknowledgment is brief; recipients get time to read the delivery speech.
        departureTarget = owner.position().add(horizontal.normalize().scale(26.0D));
        departureWaiting = true;
        departureWaitTicks = completedMode == CourierMode.PICKUP ? DEPARTURE_WAIT_TICKS - 40 : 0;
        departing = false;
        deliveryFinished = true;
        departureTicks = 0;
        mob.getNavigation().stop();
        faceTowards(player.position());
    }

    public void tick() {
        owner.setNoGravity(false);
        if (owner.level().isClientSide()) return;
        if (!(owner instanceof Mob mob)) {
            owner.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        if (departureWaiting) {
            ServerPlayer target = owner.level().getServer() == null || departureLookId == null
                    ? null
                    : owner.level().getServer().getPlayerList().getPlayer(departureLookId);
            mob.getNavigation().stop();
            if (target != null && !target.isRemoved() && !target.isSpectator()) {
                faceTowards(target.position());
            }

            departureWaitTicks++;
            if (departureWaitTicks >= DEPARTURE_WAIT_TICKS) {
                departureWaiting = false;
                departing = true;
                departureTicks = 0;
                faceTowards(departureTarget);
                mob.getNavigation().moveTo(departureTarget.x, departureTarget.y, departureTarget.z, DEPARTURE_SPEED);
            }
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

        if (letterId == null || targetPlayerId == null) {
            owner.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        if (mode == CourierMode.PICKUP) {
            ServerPlayer sender = owner.level().getServer() == null
                    ? null
                    : owner.level().getServer().getPlayerList().getPlayer(targetPlayerId);
            if (sender == null || sender.isRemoved() || sender.isSpectator()) {
                lostTargetTicks++;
                mob.getNavigation().stop();
                if (lostTargetTicks > 200) owner.remove(Entity.RemovalReason.DISCARDED);
                return;
            }

            lostTargetTicks = 0;
            double distance = owner.distanceTo(sender);
            if (distance <= ARRIVAL_DISTANCE) {
                mob.getNavigation().stop();
                faceTowards(sender.position());
                if (!pickupWaiting) {
                    pickupWaiting = true;
                    DeliveryManager.showPickupSpeech((CourierEntity) owner);
                }
                return;
            }

            pickupWaiting = false;
            mob.getNavigation().moveTo(sender, WALK_SPEED);
            faceTowards(sender.position());
            return;
        }

        ServerPlayer target = owner.level().getServer() == null
                ? null
                    : owner.level().getServer().getPlayerList().getPlayer(targetPlayerId);
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
        if (targetPlayerId != null) tag.putUUID("courier_target", targetPlayerId);
        tag.putString("courier_mode", mode.name());
        if (departureLookId != null) tag.putUUID("courier_departure_look", departureLookId);
        tag.putBoolean("courier_pickup_waiting", pickupWaiting);
        tag.putBoolean("courier_departure_waiting", departureWaiting);
        tag.putBoolean("courier_departing", departing);
    }

    public void load(CompoundTag tag) {
        String appearance = tag.getString("courier_appearance");
        if (!appearance.isBlank()) appearanceId = ResourceLocation.parse(appearance);
        if (tag.hasUUID("courier_letter")) letterId = tag.getUUID("courier_letter");
        if (tag.hasUUID("courier_target")) targetPlayerId = tag.getUUID("courier_target");
        else if (tag.hasUUID("courier_pickup_sender")) targetPlayerId = tag.getUUID("courier_pickup_sender");
        else if (tag.hasUUID("courier_recipient")) targetPlayerId = tag.getUUID("courier_recipient");
        if (tag.hasUUID("courier_departure_look")) departureLookId = tag.getUUID("courier_departure_look");
        mode = tag.contains("courier_mode") ? CourierMode.valueOf(tag.getString("courier_mode"))
                : tag.getBoolean("courier_pickup_mode") ? CourierMode.PICKUP : CourierMode.DELIVERY;
        pickupWaiting = tag.getBoolean("courier_pickup_waiting");
        departureWaiting = tag.getBoolean("courier_departure_waiting");
        departing = tag.getBoolean("courier_departing");
        deliveryFinished = departureWaiting || departing;
    }
}
