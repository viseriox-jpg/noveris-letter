package dev.noveris.letter.courier;

import dev.noveris.letter.delivery.DeliveryManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/** Single runtime entity shared by all five Crop Critters courier appearances. */
public final class CourierCropEntity extends TamableAnimal implements CourierEntity, GeoEntity {
    private static final EntityDataAccessor<String> COURIER_APPEARANCE =
            SynchedEntityData.defineId(CourierCropEntity.class, EntityDataSerializers.STRING);

    private final CourierState courierState = new CourierState(this);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public CourierCropEntity(EntityType<? extends CourierCropEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COURIER_APPEARANCE, CourierAppearanceRegistry.MELON_ID.toString());
    }

    @Override public UUID getLetterId() { return courierState.letterId(); }
    @Override public UUID getTargetPlayerId() { return courierState.targetPlayerId(); }
    @Override public CourierMode getMode() { return courierState.mode(); }
    @Override public boolean isWaitingForPickup() { return courierState.isWaitingForPickup(); }
    @Override public ResourceLocation getAppearanceId() { return ResourceLocation.parse(entityData.get(COURIER_APPEARANCE)); }

    /** Changes only the visual appearance; used by the client-side courier preview. */
    public void setAppearance(ResourceLocation appearance) {
        entityData.set(COURIER_APPEARANCE, appearance.toString());
    }

    @Override
    public void configure(UUID letterId, UUID targetPlayerId, ResourceLocation appearance, CourierMode mode) {
        courierState.configure(letterId, targetPlayerId, appearance, mode);
        setAppearance(appearance);
    }

    @Override public void beginDeparture(ServerPlayer recipient) { courierState.beginDeparture(recipient); }
    @Override public Entity entity() { return this; }
    @Override public void tick() { super.tick(); courierState.tick(); }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (DeliveryManager.finishCourierPickup(this, serverPlayer)) return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        courierState.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        courierState.load(tag);
        setAppearance(courierState.appearanceId());
    }

    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob parent) { return null; }
    @Override public boolean isFood(ItemStack stack) { return false; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController(this));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
}
