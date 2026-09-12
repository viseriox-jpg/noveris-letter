package dev.noveris.letter.courier;

import dev.noveris.letter.delivery.DeliveryManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
    private static final double FLIGHT_SPEED = 0.085D;
    private static final double ARRIVAL_DISTANCE = 2.25D;
    private static final double FLIGHT_HEIGHT = 4.5D;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private UUID letterId;
    private UUID recipientId;
    private int lostTargetTicks;

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
        setNoGravity(true);
    }

    public UUID getLetterId() { return letterId; }
    public UUID getRecipientId() { return recipientId; }
    public ResourceLocation getAppearanceId() { return ResourceLocation.parse(entityData.get(APPEARANCE)); }
    public void setAppearance(ResourceLocation appearance) { entityData.set(APPEARANCE, appearance.toString()); }
    public boolean isFlying() { return true; }

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
            DeliveryManager.finishCourierDelivery(this, target);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        String appearance = tag.getString("appearance");
        if (appearance == null || appearance.isBlank()) appearance = CourierAppearanceRegistry.DEFAULT_ID.toString();
        setAppearance(ResourceLocation.parse(appearance));
        letterId = tag.getUUID("letter");
        recipientId = tag.getUUID("recipient");
        setNoGravity(true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 0, state -> {
            if (isFlying()) return state.setAndContinue(RawAnimation.begin().thenLoop("fly"));
            return state.setAndContinue(RawAnimation.begin().thenLoop(state.isMoving() ? "walk" : "idle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
}
