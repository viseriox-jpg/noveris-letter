package dev.noveris.letter.courier;

import dev.noveris.letter.delivery.DeliveryManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/** The preserved flying Sparrow courier. */
public final class CourierLegacyBirdEntity extends PathfinderMob implements CourierEntity, GeoEntity {
    private static final EntityDataAccessor<String> APPEARANCE = SynchedEntityData.defineId(CourierLegacyBirdEntity.class, EntityDataSerializers.STRING);
    private static final double FLIGHT_SPEED = 0.24D;
    private static final double ARRIVAL_DISTANCE = 2.5D;
    private static final double DEPARTURE_SPEED = 0.30D;
    private static final int DELIVERY_WAIT_TICKS = 100;
    private static final int PICKUP_WAIT_TICKS = 40;
    private static final int DEPARTURE_TICKS = 100;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private UUID letterId;
    private UUID targetPlayerId;
    private UUID departureLookId;
    private int lostTargetTicks;
    private int departureTicks;
    private int departureWaitTicks;
    private Vec3 departureTarget = Vec3.ZERO;
    private CourierMode mode = CourierMode.DELIVERY;
    private boolean pickupWaiting;
    private boolean departureWaiting;
    private boolean departing;
    private boolean deliveryFinished;

    public CourierLegacyBirdEntity(EntityType<? extends CourierLegacyBirdEntity> type, Level level) { super(type, level); setPersistenceRequired(); setNoGravity(true); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(APPEARANCE, CourierAppearanceRegistry.SPARROW_ID.toString()); }
    @Override public UUID getLetterId() { return letterId; }
    @Override public UUID getTargetPlayerId() { return targetPlayerId; }
    @Override public CourierMode getMode() { return mode; }
    @Override public boolean isWaitingForPickup() { return mode == CourierMode.PICKUP && pickupWaiting; }
    @Override public ResourceLocation getAppearanceId() { return ResourceLocation.parse(entityData.get(APPEARANCE)); }
    public void setAppearance(ResourceLocation id) { entityData.set(APPEARANCE, id.toString()); }
    public boolean isFlying() { return true; }
    public boolean isDeparting() { return departing; }
    @Override public void configure(UUID letterId, UUID targetPlayerId, ResourceLocation appearance, CourierMode mode) { this.letterId = letterId; this.targetPlayerId = targetPlayerId; this.mode = mode; this.departureLookId = null; setAppearance(appearance); lostTargetTicks = 0; departureTicks = 0; departureWaitTicks = 0; departureTarget = Vec3.ZERO; pickupWaiting = false; departureWaiting = false; departing = false; deliveryFinished = false; setNoGravity(true); }
    @Override public void beginDeparture(ServerPlayer recipient) {
        CourierMode completedMode = mode; pickupWaiting = false; departureLookId = recipient.getUUID();
        Vec3 away = position().subtract(recipient.getX(), recipient.getY() + 1.5D, recipient.getZ());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 0.01D) horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        departureTarget = position().add(horizontal.normalize().scale(28.0D)).add(0.0D, 4.0D, 0.0D);
        departureWaiting = true; departing = false; deliveryFinished = true; departureTicks = 0;
        departureWaitTicks = completedMode == CourierMode.PICKUP ? PICKUP_WAIT_TICKS : DELIVERY_WAIT_TICKS;
        setDeltaMovement(Vec3.ZERO); faceTowards(recipient.position());
    }
    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (DeliveryManager.finishCourierPickup(this, serverPlayer)) return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
    @Override public void tick() {
        super.tick(); setNoGravity(true); if (level().isClientSide()) return;
        if (departureWaiting) { ServerPlayer look = level().getServer() == null || departureLookId == null ? null : level().getServer().getPlayerList().getPlayer(departureLookId); setDeltaMovement(Vec3.ZERO); if (look != null) faceTowards(look.position()); if (--departureWaitTicks <= 0) { departureWaiting = false; departing = true; faceTowards(departureTarget); } return; }
        if (departing) { faceTowards(departureTarget); setDeltaMovement(departureTarget.subtract(position()).normalize().scale(DEPARTURE_SPEED)); departureTicks++; if (departureTicks >= DEPARTURE_TICKS || position().distanceToSqr(departureTarget) < 2.0D) remove(Entity.RemovalReason.DISCARDED); return; }
        if (letterId == null || targetPlayerId == null) { remove(Entity.RemovalReason.DISCARDED); return; }
        if (mode == CourierMode.PICKUP) {
            ServerPlayer sender = level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(targetPlayerId);
            if (sender == null || sender.isRemoved() || sender.isSpectator()) { lostTargetTicks++; setDeltaMovement(Vec3.ZERO); if (lostTargetTicks > 200) remove(Entity.RemovalReason.DISCARDED); return; }
            lostTargetTicks = 0;
            Vec3 targetPos = sender.position().add(0.0D, 1.2D, 0.0D);
            double distance = position().distanceTo(targetPos);
            if (distance <= ARRIVAL_DISTANCE) { setDeltaMovement(Vec3.ZERO); faceTowards(targetPos); pickupWaiting = true; return; }
            pickupWaiting = false; faceTowards(targetPos); setDeltaMovement(targetPos.subtract(position()).normalize().scale(FLIGHT_SPEED)); hasImpulse = true; return;
        }
        ServerPlayer target = level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(targetPlayerId);
        if (target == null || target.isRemoved() || target.isSpectator()) { lostTargetTicks++; setDeltaMovement(Vec3.ZERO); if (lostTargetTicks > 200) remove(Entity.RemovalReason.DISCARDED); return; }
        lostTargetTicks = 0;
        Vec3 targetPos = target.position().add(0.0D, 1.2D, 0.0D);
        double distance = position().distanceTo(targetPos);
        if (distance <= ARRIVAL_DISTANCE) { setDeltaMovement(Vec3.ZERO); if (!deliveryFinished) DeliveryManager.finishCourierDelivery(this, target); return; }
        faceTowards(targetPos); setDeltaMovement(targetPos.subtract(position()).normalize().scale(FLIGHT_SPEED)); hasImpulse = true;
    }
    private void faceTowards(Vec3 target) { Vec3 delta = target.subtract(position()); float yaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0D); setYRot(yaw); setYHeadRot(yaw); yBodyRot = yaw; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putString("courier_appearance", getAppearanceId().toString()); if (letterId != null) tag.putUUID("courier_letter", letterId); if (targetPlayerId != null) tag.putUUID("courier_target", targetPlayerId); if (departureLookId != null) tag.putUUID("courier_departure_look", departureLookId); tag.putString("courier_mode", mode.name()); tag.putBoolean("courier_pickup_waiting", pickupWaiting); tag.putBoolean("courier_departure_waiting", departureWaiting); tag.putInt("courier_departure_wait_ticks", departureWaitTicks); tag.putBoolean("courier_departing", departing); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); String appearance = tag.getString("courier_appearance"); if (!appearance.isBlank()) setAppearance(ResourceLocation.parse(appearance)); if (tag.hasUUID("courier_letter")) letterId = tag.getUUID("courier_letter"); if (tag.hasUUID("courier_target")) targetPlayerId = tag.getUUID("courier_target"); else if (tag.hasUUID("courier_pickup_sender")) targetPlayerId = tag.getUUID("courier_pickup_sender"); else if (tag.hasUUID("courier_recipient")) targetPlayerId = tag.getUUID("courier_recipient"); if (tag.hasUUID("courier_departure_look")) departureLookId = tag.getUUID("courier_departure_look"); mode = tag.contains("courier_mode") ? CourierMode.valueOf(tag.getString("courier_mode")) : tag.getBoolean("courier_pickup_mode") ? CourierMode.PICKUP : CourierMode.DELIVERY; pickupWaiting = tag.getBoolean("courier_pickup_waiting"); departureWaiting = tag.getBoolean("courier_departure_waiting"); departureWaitTicks = tag.getInt("courier_departure_wait_ticks"); departing = tag.getBoolean("courier_departing"); deliveryFinished = departureWaiting || departing; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { controllers.add(new AnimationController<>(this, "flight", 0, this::handleFlightAnimation)); }
    private PlayState handleFlightAnimation(AnimationState<CourierLegacyBirdEntity> state) { return state.setAndContinue(RawAnimation.begin().thenLoop("fly")); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
    @Override public Entity entity() { return this; }
}
