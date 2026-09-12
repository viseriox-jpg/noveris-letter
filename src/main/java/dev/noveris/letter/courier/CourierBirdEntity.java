package dev.noveris.letter.courier;

import dev.noveris.letter.delivery.DeliveryManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public final class CourierBirdEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> APPEARANCE = SynchedEntityData.defineId(CourierBirdEntity.class, EntityDataSerializers.STRING);
    private static final double FLIGHT_SPEED = 0.065D;
    private static final double ARRIVAL_DISTANCE = 2.0D;
    private static final double FLIGHT_HEIGHT = 4.5D;
    private static final double DEPARTURE_SPEED = 0.11D;
    private static final int DEPARTURE_TICKS = 70;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private UUID letterId;
    private UUID recipientId;
    private int lostTargetTicks;
    private int departureTicks;
    private Vec3 departureVelocity = Vec3.ZERO;
    private boolean departing;
    private boolean deliveryFinished;

    public CourierBirdEntity(EntityType<? extends CourierBirdEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoGravity(true);
    }

    public void configure(UUID letterId, UUID recipientId, ResourceLocation appearance) {
        this.letterId = letterId;
        this.recipientId = recipientId;
        setAppearance(appearance);
        this.lostTargetTicks = 0;
        this.departureTicks = 0;
        this.departureVelocity = Vec3.ZERO;
        this.departing = false;
        this.deliveryFinished = false;
        setNoGravity(true);
    }

    public UUID getLetterId() { return letterId; }
    public UUID getRecipientId() { return recipientId; }
    public ResourceLocation getAppearanceId() { return ResourceLocation.parse(entityData.get(APPEARANCE)); }
    public void setAppearance(ResourceLocation appearance) { entityData.set(APPEARANCE, appearance.toString()); }
    public boolean isFlying() { return true; }
    public boolean isDeparting() { return departing; }

    public void beginDeparture(ServerPlayer recipient) {
        Vec3 away = position().subtract(recipient.getX(), recipient.getY() + 1.5D, recipient.getZ());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 0.01D) horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 direction = horizontal.normalize();
        departureVelocity = direction.scale(DEPARTURE_SPEED).add(0.0D, 0.018D, 0.0D);
        departing = true;
        deliveryFinished = true;
        departureTicks = 0;
        setDeltaMovement(departureVelocity);
        hasImpulse = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(APPEARANCE, CourierAppearanceRegistry.DEFAULT_ID.toString());
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        if (level().isClientSide()) return;

        if (departing) {
            departureTicks++;
            setDeltaMovement(departureVelocity);
            hasImpulse = true;
            if (departureTicks >= DEPARTURE_TICKS) remove(RemovalReason.DISCARDED);
            return;
        }

        if (letterId == null || recipientId == null) {
            remove(RemovalReason.DISCARDED);
            return;
        }

        var target = level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(recipientId);
        if (target == null || target.isRemoved() || target.isSpectator()) {
            lostTargetTicks++;
            setDeltaMovement(Vec3.ZERO);
            if (lostTargetTicks > 200) remove(RemovalReason.DISCARDED);
            return;
        }
        lostTargetTicks = 0;

        double targetY = target.getY() + FLIGHT_HEIGHT;
        Vec3 toTarget = new Vec3(target.getX() - getX(), targetY - getY(), target.getZ() - getZ());
        double distance = toTarget.length();
        if (distance <= ARRIVAL_DISTANCE) {
            setDeltaMovement(Vec3.ZERO);
            if (!deliveryFinished) DeliveryManager.finishCourierDelivery(this, target);
            return;
        }

        Vec3 velocity = toTarget.scale(FLIGHT_SPEED / Math.max(distance, 0.001D));
        setDeltaMovement(velocity);
        hasImpulse = true;
        setYRot((float) (Math.toDegrees(Math.atan2(-velocity.x, velocity.z))));
        yRotO = getYRot();
        setXRot((float) (Math.toDegrees(-Math.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)))));
        xRotO = getXRot();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("appearance", getAppearanceId().toString());
        if (letterId != null) tag.putUUID("letter", letterId);
        if (recipientId != null) tag.putUUID("recipient", recipientId);
        tag.putBoolean("departing", departing);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        String appearance = tag.getString("appearance");
        if (appearance == null || appearance.isBlank()) appearance = CourierAppearanceRegistry.DEFAULT_ID.toString();
        setAppearance(ResourceLocation.parse(appearance));
        if (tag.hasUUID("letter")) letterId = tag.getUUID("letter");
        if (tag.hasUUID("recipient")) recipientId = tag.getUUID("recipient");
        departing = tag.getBoolean("departing");
        deliveryFinished = departing;
        setNoGravity(true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "flight", 0, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("fly"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
}
