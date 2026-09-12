package dev.noveris.letter.courier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.UUID;

public final class CourierCoatiEntity extends TamableAnimal implements CourierBirdEntity {
    private static final EntityDataAccessor<String> COURIER_APPEARANCE = SynchedEntityData.defineId(CourierCoatiEntity.class, EntityDataSerializers.STRING);
    public final AnimationState diggingAnimationState = new AnimationState();
    public final AnimationState sittingAnimationState = new AnimationState();
    public final AnimationState standingUpAnimationState = new AnimationState();
    public final AnimationState sniffingAnimationState = new AnimationState();
    public final AnimationState unBurrowingAnimationState = new AnimationState();
    public final AnimationState pickUpItemAnimationState = new AnimationState();
    public final AnimationState idlingAnimationState = new AnimationState();
    public final AnimationState leftEarTwitchAnimationState = new AnimationState();
    public final AnimationState rightEarTwitchAnimationState = new AnimationState();
    private final CourierState courierState = new CourierState(this);
    public CourierCoatiEntity(EntityType<? extends TamableAnimal> type, Level level) { super(type, level); setNoGravity(true); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(COURIER_APPEARANCE, CourierAppearanceRegistry.COATI_ID.toString()); }
    public boolean hasChest() { return false; }
    public int getDaysFedHoney() { return 0; }
    public int getDaysFedHoneyNeeded() { return 7; }
    @Override public UUID getLetterId() { return courierState.letterId(); }
    @Override public UUID getRecipientId() { return courierState.recipientId(); }
    @Override public ResourceLocation getAppearanceId() { return ResourceLocation.parse(entityData.get(COURIER_APPEARANCE)); }
    @Override public void configure(UUID l, UUID r, ResourceLocation a) { courierState.configure(l,r,a); entityData.set(COURIER_APPEARANCE,a.toString()); }
    @Override public void beginDeparture(ServerPlayer p) { courierState.beginDeparture(p); }
    @Override public Entity entity() { return this; }
    @Override public void tick() { super.tick(); courierState.tick(); }
    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); courierState.save(t); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); courierState.load(t); entityData.set(COURIER_APPEARANCE,courierState.appearanceId().toString()); }
    @Override public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob o) { return null; }
    @Override public boolean isFood(ItemStack s) { return false; }
}
