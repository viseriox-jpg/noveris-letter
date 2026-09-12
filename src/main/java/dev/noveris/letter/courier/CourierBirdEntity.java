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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public final class CourierBirdEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<String> APPEARANCE = SynchedEntityData.defineId(CourierBirdEntity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private UUID letterId;
    private UUID recipientId;
    private int lostTargetTicks;

    public CourierBirdEntity(EntityType<? extends CourierBirdEntity> type, Level level) { super(type, level); setPersistenceRequired(); }
    public void configure(UUID letterId, UUID recipientId, ResourceLocation appearance) { this.letterId = letterId; this.recipientId = recipientId; setAppearance(appearance); this.lostTargetTicks = 0; }
    public UUID getLetterId() { return letterId; }
    public UUID getRecipientId() { return recipientId; }
    public ResourceLocation getAppearanceId() { return ResourceLocation.parse(entityData.get(APPEARANCE)); }
    public void setAppearance(ResourceLocation appearance) { entityData.set(APPEARANCE, appearance.toString()); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(APPEARANCE, CourierAppearanceRegistry.DEFAULT_ID.toString());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (letterId == null || recipientId == null) { remove(RemovalReason.DISCARDED); return; }
        var target = level().getServer() == null ? null : level().getServer().getPlayerList().getPlayer(recipientId);
        if (target == null || target.isRemoved() || target.isSpectator()) {
            lostTargetTicks++;
            getNavigation().stop();
            if (lostTargetTicks > 200) remove(RemovalReason.DISCARDED);
            return;
        }
        lostTargetTicks = 0;
        if (distanceTo(target) <= 2.25D) { DeliveryManager.finishCourierDelivery(this, target); return; }
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (tickCount % 10 == 0 || getNavigation().isDone()) getNavigation().moveTo(target, 1.15D);
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
        setAppearance(ResourceLocation.parse(tag.getString("appearance").orElse(CourierAppearanceRegistry.DEFAULT_ID.toString())));
        letterId = tag.getUUID("letter").orElse(null);
        recipientId = tag.getUUID("recipient").orElse(null);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> state.setAndContinue(
                RawAnimation.begin().thenLoop(state.isMoving() ? "walk" : "idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
}
