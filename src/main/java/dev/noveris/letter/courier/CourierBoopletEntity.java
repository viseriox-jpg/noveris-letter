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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.UUID;

public final class CourierBoopletEntity extends Animal implements CourierBirdEntity {
    private static final EntityDataAccessor<String> COURIER_APPEARANCE = SynchedEntityData.defineId(CourierBoopletEntity.class, EntityDataSerializers.STRING);
    public final AnimationState happyAnimationState = new AnimationState();
    public final AnimationState boopAnimationState = new AnimationState();
    public final AnimationState swimAnimationState = new AnimationState();
    private final CourierState courierState = new CourierState(this);
    public CourierBoopletEntity(EntityType<? extends Animal> type, Level level) { super(type, level); setNoGravity(true); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(COURIER_APPEARANCE, CourierAppearanceRegistry.BOOPLET_ID.toString()); }
    public boolean isFleeing() { return false; }
    public boolean isFluffy() { return false; }
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
